package com.example.musicplayer;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Fetches YouTube search results and audio stream URLs via the Invidious API.
 */
public class YouTubeApiHandler {
    private static final String TAG = "YouTubeApiHandler";

    private static final String INSTANCES_API_URL = "https://api.invidious.io/instances.json?sort_by=health";

    private static final String[] FALLBACK_INSTANCES = {
            "https://invidious.drgns.space",
            "https://inv.tux.rs",
            "https://yewtu.be",
            "https://inv.nadeko.net",
            "https://iv.datura.network",
            "https://invidious.nerdvpn.de",
            "https://invidious.perennialte.ch",
            "https://yt.cdaut.de",
            "https://invidious.privacyredirect.com",
            "https://invidious.darkness.services",
            "https://inv.nsh.fyi",
            "https://invidious.flokinet.to"
    };

    private static final long[] BACKOFF_MS = {0, 200, 500, 1000, 2000};

    private volatile List<String> instances = new ArrayList<>(java.util.Arrays.asList(FALLBACK_INSTANCES));
    private final Set<String> deadInstances = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    private final List<Runnable> pendingQueue = new ArrayList<>();
    private volatile boolean instancesReady = false;

    private final AtomicReference<Call> activeSearchCall = new AtomicReference<>(null);
    private final AtomicInteger searchGeneration = new AtomicInteger(0);

    private static final long SEARCH_DEBOUNCE_MS = 300;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch = null;

    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface YouTubeCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public YouTubeApiHandler() {
        this.client = buildHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
        fetchLiveInstances();
    }

    private void fetchLiveInstances() {
        Request req = new Request.Builder()
                .url(INSTANCES_API_URL)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.w(TAG, "Instance list fetch failed: " + e.getMessage());
                markInstancesReady();
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) { markInstancesReady(); return; }

                    JSONArray arr = new JSONArray(response.body().string());
                    List<String> fresh = new ArrayList<>();

                    for (int j = 0; j < arr.length(); j++) {
                        JSONArray pair = arr.getJSONArray(j);
                        JSONObject info = pair.getJSONObject(1);

                        if (!info.optBoolean("api", false)) continue;
                        String uri = info.optString("uri", "").trim();
                        if (!uri.startsWith("https://")) continue;

                        fresh.add(uri);
                    }

                    if (!fresh.isEmpty()) {
                        String preferred = "https://invidious.drgns.space";
                        if (fresh.contains(preferred)) {
                            fresh.remove(preferred);
                            fresh.add(0, preferred);
                        }

                        instances = fresh;
                        deadInstances.clear();
                        currentIndex.set(0);
                        Log.i(TAG, "Loaded " + fresh.size() + " Invidious instances");
                    }
                    markInstancesReady();
                } catch (Exception e) {
                    Log.e(TAG, "Instance list parse error", e);
                    markInstancesReady();
                } finally {
                    response.close();
                }
            }
        });
    }

    private synchronized void markInstancesReady() {
        instancesReady = true;
        for (Runnable r : pendingQueue) mainHandler.post(r);
        pendingQueue.clear();
    }

    private synchronized void whenReady(Runnable task) {
        if (instancesReady) task.run();
        else pendingQueue.add(task);
    }

    private static String lastWorkingInstance = null;
    private final java.util.Map<String, CachedUrl> streamCache = new ConcurrentHashMap<>();

    private static class CachedUrl {
        String url;
        long expiry;
        CachedUrl(String url) {
            this.url = url;
            this.expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(45);
        }
        boolean isValid() { return System.currentTimeMillis() < expiry; }
    }

    private synchronized String getNextAliveInstance(String justFailed) {
        if (justFailed != null) {
            deadInstances.add(justFailed);
            if (justFailed.equals(lastWorkingInstance)) lastWorkingInstance = null;
            Log.w(TAG, "Marking instance dead: " + justFailed);
        }
        
        if (deadInstances.size() >= instances.size()) {
            deadInstances.clear();
        }

        if (lastWorkingInstance != null && !deadInstances.contains(lastWorkingInstance)) {
            return lastWorkingInstance;
        }

        for (int i = 0; i < instances.size(); i++) {
            int idx = (currentIndex.get() + i) % instances.size();
            String candidate = instances.get(idx);
            if (!deadInstances.contains(candidate)) {
                currentIndex.set(idx);
                return candidate;
            }
        }
        
        return !instances.isEmpty() ? instances.get(0) : FALLBACK_INSTANCES[0];
    }

    private synchronized void markInstanceWorked(String instance) {
        if (instance != null) {
            lastWorkingInstance = instance;
            deadInstances.remove(instance);
        }
    }

    private long backoffMs(int attempt) {
        int idx = Math.min(attempt, BACKOFF_MS.length - 1);
        return BACKOFF_MS[idx];
    }

    private String bestThumbnail(JSONArray thumbs, String instance) {
        if (thumbs == null || thumbs.length() == 0) return "";
        try {
            String url = "";
            for (int i = 0; i < thumbs.length(); i++) {
                JSONObject t = thumbs.getJSONObject(i);
                String q = t.optString("quality", "");
                if ("high".equals(q) || "medium".equals(q)) {
                    url = t.optString("url", "");
                    break;
                }
            }
            if (url.isEmpty()) url = thumbs.getJSONObject(0).optString("url", "");
            if (url.startsWith("/")) url = instance + url;
            return url;
        } catch (Exception e) { return ""; }
    }

    public void search(String query, YouTubeCallback<List<Song>> callback) {
        if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
        pendingSearch = () -> { pendingSearch = null; startSearch(query, callback); };
        debounceHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    public void searchImmediate(String query, YouTubeCallback<List<Song>> callback) {
        if (pendingSearch != null) { debounceHandler.removeCallbacks(pendingSearch); pendingSearch = null; }
        startSearch(query, callback);
    }

    private void startSearch(String query, YouTubeCallback<List<Song>> callback) {
        Call prev = activeSearchCall.getAndSet(null);
        if (prev != null) prev.cancel();
        int generation = searchGeneration.incrementAndGet();
        whenReady(() -> {
            String first = getNextAliveInstance(null);
            searchInternal(query, callback, first, generation, 0);
        });
    }

    public void getStreamUrl(String videoId, YouTubeCallback<String> callback) {
        CachedUrl cached = streamCache.get(videoId);
        if (cached != null && cached.isValid()) {
            mainHandler.post(() -> callback.onSuccess(cached.url));
            return;
        }

        whenReady(() -> {
            String first = getNextAliveInstance(null);
            getStreamUrlInternal(videoId, callback, first, 0);
        });
    }

    public void searchAlbums(String query, YouTubeCallback<List<Album>> callback) {
        whenReady(() -> {
            String first = getNextAliveInstance(null);
            // Search for "Artist Name Album" or "Top Albums"
            searchAlbumsInternal(query, callback, first, 0);
        });
    }

    private void searchAlbumsInternal(String query, YouTubeCallback<List<Album>> callback, String instance, int attempt) {
        // Appending "full album" to improve chances of finding official content
        String enhancedQuery = query;
        if (!query.toLowerCase().contains("album")) {
            enhancedQuery += " full album";
        }
        
        String url = instance + "/api/v1/search?q=" + Uri.encode(enhancedQuery)
                + "&type=playlist&fields=playlistId,title,author,playlistThumbnail";

        client.newCall(new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                tryNextAlbumSearch(query, callback, instance, attempt + 1);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    tryNextAlbumSearch(query, callback, instance, attempt + 1);
                    return;
                }
                try {
                    JSONArray results = new JSONArray(response.body().string());
                    List<Album> albums = new ArrayList<>();

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        String playlistId = item.optString("playlistId", "");
                        if (playlistId.isEmpty()) continue;

                        String title = item.optString("title", "");
                        String author = item.optString("author", "");
                        
                        // Filter out generic user playlists
                        String lowTitle = title.toLowerCase();
                        if (lowTitle.contains("mix") || lowTitle.contains("favorites") || 
                            lowTitle.contains("my songs") || lowTitle.contains("playlist")) continue;

                        Album album = new Album();
                        album.setId("yt_playlist_" + playlistId);
                        album.setTitle(title);
                        // Clean up " - Topic" from artist name for better UI
                        album.setArtist(author.replace(" - Topic", ""));
                        album.setImageUrl(item.optString("playlistThumbnail", ""));
                        album.setYear("2025");
                        
                        // Prioritize official Topic channels
                        if (author.endsWith(" - Topic")) {
                            albums.add(0, album);
                        } else {
                            albums.add(album);
                        }
                    }
                    
                    if (albums.isEmpty() && attempt < 3) {
                        tryNextAlbumSearch(query, callback, instance, attempt + 1);
                        return;
                    }

                    markInstanceWorked(instance);
                    mainHandler.post(() -> callback.onSuccess(albums));
                } catch (Exception e) {
                    tryNextAlbumSearch(query, callback, instance, attempt + 1);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void tryNextAlbumSearch(String query, YouTubeCallback<List<Album>> callback, String failed, int attempt) {
        if (attempt >= 10) {
            mainHandler.post(() -> callback.onError(new Exception("Album search unavailable")));
            return;
        }
        String next = getNextAliveInstance(failed);
        searchAlbumsInternal(query, callback, next, attempt);
    }

    public void getPlaylistSongs(String playlistId, YouTubeCallback<List<Song>> callback) {
        whenReady(() -> {
            String first = getNextAliveInstance(null);
            getPlaylistSongsInternal(playlistId, callback, first, 0);
        });
    }

    private void getPlaylistSongsInternal(String playlistId, YouTubeCallback<List<Song>> callback, String instance, int attempt) {
        // Mixes (RD...) and high attempt counts go to Piped
        if (playlistId.startsWith("RD") || attempt > 3) {
            getPipedPlaylistSongs(playlistId, callback, attempt);
            return;
        }

        String url = instance + "/api/v1/playlists/" + playlistId;
        Log.d(TAG, "Requesting playlist songs (Attempt " + attempt + ") from " + instance + ": " + url);

        client.newCall(new Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Playlist request failure [" + instance + "]: " + e.getMessage());
                tryNextPlaylistSongs(playlistId, callback, instance, attempt + 1);
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = "";
                try {
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "Playlist response unsuccessful [" + instance + "]: " + response.code());
                        response.close();
                        tryNextPlaylistSongs(playlistId, callback, instance, attempt + 1);
                        return;
                    }
                    
                    body = response.body().string();
                    JSONObject result = new JSONObject(body);
                    
                    JSONArray videos = result.optJSONArray("videos");
                    if (videos == null) videos = result.optJSONArray("playlistVideos");
                    
                    List<Song> songs = new ArrayList<>();
                    if (videos != null && videos.length() > 0) {
                        for (int i = 0; i < videos.length(); i++) {
                            JSONObject item = videos.getJSONObject(i);
                            String videoId = item.optString("videoId", item.optString("id", ""));
                            if (videoId.isEmpty()) continue;

                            Song song = new Song();
                            song.setId("youtube_" + videoId);
                            song.setTitle(item.optString("title", "Unknown Title"));
                            song.setArtist(item.optString("author", item.optString("uploaderName", "Unknown Artist")));
                            
                            JSONArray thumbs = item.optJSONArray("videoThumbnails");
                            if (thumbs == null) thumbs = item.optJSONArray("thumbnails");
                            song.setImageUrl(bestThumbnail(thumbs, instance));
                            
                            int duration = item.optInt("lengthSeconds", item.optInt("duration", 0));
                            song.setDuration(duration * 1000);
                            song.setSongUrl("https://www.youtube.com/watch?v=" + videoId);
                            song.setAlbum(result.optString("title", "YouTube Album"));
                            song.setAlbumId("yt_playlist_" + playlistId);
                            songs.add(song);
                        }
                    }
                    
                    if (songs.isEmpty()) {
                        Log.w(TAG, "Empty playlist from Invidious [" + instance + "]. Retrying with Piped.");
                        getPipedPlaylistSongs(playlistId, callback, attempt + 1);
                    } else {
                        Log.i(TAG, "Loaded " + songs.size() + " songs from " + instance);
                        markInstanceWorked(instance);
                        mainHandler.post(() -> callback.onSuccess(songs));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing playlist from " + instance, e);
                    tryNextPlaylistSongs(playlistId, callback, instance, attempt + 1);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void getPipedPlaylistSongs(String playlistId, YouTubeCallback<List<Song>> callback, int attempt) {
        // Rotate Piped instances if needed, but start with the main one
        String pipedInstance = "https://pipedapi.kavin.rocks";
        if (attempt > 7) pipedInstance = "https://pipedapi.tokhmi.xyz";
        
        String url = pipedInstance + "/playlists/" + playlistId;
        Log.d(TAG, "Requesting playlist from Piped: " + url);

        client.newCall(new Request.Builder()
                .url(url)
                .build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Piped playlist failure: " + e.getMessage());
                if (attempt < 10) {
                    tryNextPlaylistSongs(playlistId, callback, "piped", attempt + 1);
                } else {
                    mainHandler.post(() -> callback.onError(new Exception("Failed after multiple retries")));
                }
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Piped error " + response.code());
                    }
                    String body = response.body().string();
                    JSONObject result = new JSONObject(body);
                    JSONArray streams = result.optJSONArray("relatedStreams");
                    
                    List<Song> songs = new ArrayList<>();
                    if (streams != null) {
                        for (int i = 0; i < streams.length(); i++) {
                            JSONObject item = streams.getJSONObject(i);
                            String videoUrl = item.optString("url", "");
                            String videoId = "";
                            if (videoUrl.contains("v=")) {
                                videoId = videoUrl.split("v=")[1].split("&")[0];
                            } else if (videoUrl.contains("/watch?v=")) {
                                videoId = videoUrl.replace("/watch?v=", "").split("&")[0];
                            }
                            
                            if (videoId.isEmpty()) continue;

                            Song song = new Song();
                            song.setId("youtube_" + videoId);
                            song.setTitle(item.optString("title", "Unknown Title"));
                            song.setArtist(item.optString("uploaderName", "Unknown Artist"));
                            song.setImageUrl(item.optString("thumbnail", ""));
                            song.setDuration(item.optInt("duration", 0) * 1000);
                            song.setSongUrl("https://www.youtube.com/watch?v=" + videoId);
                            song.setAlbum(result.optString("name", "YouTube Album"));
                            song.setAlbumId("yt_playlist_" + playlistId);
                            songs.add(song);
                        }
                    }

                    if (songs.isEmpty()) {
                        Log.w(TAG, "Piped returned empty streams. Retrying next Invidious.");
                        tryNextPlaylistSongs(playlistId, callback, "piped", attempt + 1);
                    } else {
                        Log.i(TAG, "Loaded " + songs.size() + " songs from Piped");
                        mainHandler.post(() -> callback.onSuccess(songs));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Piped playlist", e);
                    tryNextPlaylistSongs(playlistId, callback, "piped", attempt + 1);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void tryNextPlaylistSongs(String playlistId, YouTubeCallback<List<Song>> callback, String failed, int attempt) {
        if (attempt >= 10) {
            mainHandler.post(() -> callback.onError(new Exception("Failed to load songs")));
            return;
        }
        String next = getNextAliveInstance(failed);
        getPlaylistSongsInternal(playlistId, callback, next, attempt);
    }

    public void cancel() {
        if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
        Call active = activeSearchCall.getAndSet(null);
        if (active != null) active.cancel();
    }

    private void searchInternal(String query, YouTubeCallback<List<Song>> callback, String instance, int generation, int attempt) {
        if (searchGeneration.get() != generation) return;
        String url = instance + "/api/v1/search?q=" + Uri.encode(query) + "&type=video&fields=videoId,title,author,videoThumbnails,lengthSeconds";
        Call call = client.newCall(new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build());
        activeSearchCall.set(call);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                tryNextSearch(query, callback, instance, generation, attempt + 1);
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (call.isCanceled()) { response.close(); return; }
                if (!response.isSuccessful()) {
                    response.close();
                    tryNextSearch(query, callback, instance, generation, attempt + 1);
                    return;
                }
                try {
                    JSONArray results = new JSONArray(response.body().string());
                    List<Song> songs = new ArrayList<>();
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject item = results.getJSONObject(i);
                        String videoId = item.optString("videoId", "");
                        if (videoId.isEmpty()) continue;
                        Song song = new Song();
                        song.setId("youtube_" + videoId);
                        song.setTitle(item.optString("title", "Unknown Title"));
                        song.setArtist(item.optString("author", "Unknown Artist"));
                        song.setImageUrl(bestThumbnail(item.optJSONArray("videoThumbnails"), instance));
                        song.setDuration(item.optInt("lengthSeconds", 0) * 1000);
                        song.setSongUrl("https://www.youtube.com/watch?v=" + videoId);
                        songs.add(song);
                    }
                    markInstanceWorked(instance);
                    mainHandler.post(() -> callback.onSuccess(songs));
                } catch (Exception e) {
                    tryNextSearch(query, callback, instance, generation, attempt + 1);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void tryNextSearch(String query, YouTubeCallback<List<Song>> callback, String failed, int generation, int attempt) {
        if (searchGeneration.get() != generation) return;
        if (attempt >= 10) {
            mainHandler.post(() -> callback.onError(new Exception("Search unavailable")));
            return;
        }
        String next = getNextAliveInstance(failed);
        searchInternal(query, callback, next, generation, attempt);
    }

    private void getStreamUrlInternal(String videoId, YouTubeCallback<String> callback, String instance, int attempt) {
        if (attempt >= 10) {
            callback.onError(new Exception("Stream unavailable"));
            return;
        }
        String url = instance + "/api/v1/videos/" + videoId + "?fields=adaptiveFormats,formatStreams&local=true";
        client.newCall(new Request.Builder().url(url).addHeader("User-Agent", "Mozilla/5.0").build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                tryNextStream(videoId, callback, instance, attempt + 1);
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    tryNextStream(videoId, callback, instance, attempt + 1);
                    return;
                }
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String chosen = null;
                    JSONArray formats = json.optJSONArray("adaptiveFormats");
                    if (formats == null) formats = json.optJSONArray("formatStreams");
                    if (formats != null) {
                        for (int i = 0; i < formats.length(); i++) {
                            JSONObject f = formats.getJSONObject(i);
                            String type = f.optString("type", "");
                            if (type.contains("audio/mp4") || type.contains("audio/webm")) {
                                chosen = f.optString("url", "");
                                break;
                            }
                        }
                    }
                    if (chosen == null || chosen.isEmpty()) {
                        tryNextStream(videoId, callback, instance, attempt + 1);
                        return;
                    }
                    if (chosen.startsWith("/")) chosen = instance + chosen;
                    final String finalUrl = chosen;
                    markInstanceWorked(instance);
                    streamCache.put(videoId, new CachedUrl(finalUrl));
                    mainHandler.post(() -> callback.onSuccess(finalUrl));
                } catch (Exception e) {
                    tryNextStream(videoId, callback, instance, attempt + 1);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void tryNextStream(String videoId, YouTubeCallback<String> callback, String failed, int attempt) {
        if (attempt >= 10) {
            mainHandler.post(() -> callback.onError(new Exception("Stream unavailable")));
            return;
        }
        String next = getNextAliveInstance(failed);
        getStreamUrlInternal(videoId, callback, next, attempt);
    }

    private OkHttpClient buildHttpClient() {
        try {
            TrustManager[] trustAll = new TrustManager[]{new X509TrustManager() {
                @Override public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) throws CertificateException {}
                @Override public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) throws CertificateException {}
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
            }};
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(ctx.getSocketFactory(), (X509TrustManager) trustAll[0])
                    .hostnameVerifier((h, s) -> true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();
        } catch (Exception e) {
            return new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).retryOnConnectionFailure(true).build();
        }
    }
}
