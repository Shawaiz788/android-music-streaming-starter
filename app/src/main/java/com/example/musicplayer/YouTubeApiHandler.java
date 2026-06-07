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
 *
 * Invidious API used:
 *   Search  : GET {instance}/api/v1/search?q={query}&type=video&fields=videoId,title,author,videoThumbnails,lengthSeconds
 *   Stream  : GET {instance}/api/v1/videos/{videoId}?fields=adaptiveFormats
 *
 * Instance list refreshed at startup from https://api.invidious.io/instances.json
 * Falls back to a hardcoded set if that fetch fails.
 */
public class YouTubeApiHandler {
    private static final String TAG = "YouTubeApiHandler";

    // Official Invidious instances API — returns array of [domain, {api: true, ...}] pairs
    private static final String INSTANCES_API_URL = "https://api.invidious.io/instances.json?sort_by=health";

    // Hardcoded fallback — well-known, CDN-backed or high-uptime instances
    private static final String[] FALLBACK_INSTANCES = {
            "https://yewtu.be",
            "https://inv.nadeko.net",
            "https://invidious.privacyredirect.com",
            "https://iv.datura.network",
            "https://invidious.nerdvpn.de",
            "https://invidious.perennialte.ch",
            "https://yt.cdaut.de",
            "https://invidious.darkness.services",
            "https://invidious.tiekoetter.com",
            "https://inv.thepixora.com",
    };

    // Backoff per attempt index: 0ms, 600ms, 1.2s, 2.5s — then fail
    private static final long[] BACKOFF_MS = {0, 600, 1200, 2500};

    private volatile List<String> instances;
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

    // ── Constructor ───────────────────────────────────────────────────────────

    public YouTubeApiHandler() {
        this.client = buildHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());

        List<String> seed = new ArrayList<>();
        Collections.addAll(seed, FALLBACK_INSTANCES);
        instances = seed;

        fetchLiveInstances();
    }

    // ── Live instance list ────────────────────────────────────────────────────

    /**
     * Fetches the official Invidious instances list sorted by health.
     * Only keeps instances that have the API enabled and use HTTPS.
     * Format: [ ["domain", { "api": true/false, "uri": "https://...", ... }], ... ]
     */
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

                    for (int i = 0; i < arr.length(); i++) {
                        JSONArray pair = arr.getJSONArray(i); // ["domain", {...}]
                        JSONObject info = pair.getJSONObject(1);

                        // Only include instances with API enabled over HTTPS
                        if (!info.optBoolean("api", false)) continue;
                        String uri = info.optString("uri", "").trim();
                        if (!uri.startsWith("https://")) continue;

                        fresh.add(uri);
                    }

                    if (!fresh.isEmpty()) {
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

    // ── Instance rotation ─────────────────────────────────────────────────────

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
            Log.w(TAG, "Dead: " + justFailed + " (" + deadInstances.size() + "/" + instances.size() + ")");
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
        return null; // all exhausted
    }

    private synchronized void markInstanceWorked(String instance) {
        if (instance != null) {
            lastWorkingInstance = instance;
            deadInstances.remove(instance);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractVideoId(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        try {
            Uri uri = Uri.parse(rawUrl);
            String v = uri.getQueryParameter("v");
            if (v != null && !v.isEmpty()) return v;
            String last = uri.getLastPathSegment();
            if (last != null && !last.isEmpty()) return last;
        } catch (Exception ignored) {}
        return rawUrl.substring(rawUrl.lastIndexOf('/') + 1);
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
            
            if (url.startsWith("/")) {
                url = instance + url;
            }
            return url;
        } catch (Exception e) { return ""; }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Debounced search — wire to TextWatcher. */
    public void search(String query, YouTubeCallback<List<Song>> callback) {
        if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
        pendingSearch = () -> { pendingSearch = null; startSearch(query, callback); };
        debounceHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
    }

    /** Immediate search — wire to IME action / search button. */
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
            if (first == null) {
                mainHandler.post(() -> callback.onError(new Exception("No YouTube instances available.")));
                return;
            }
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
            if (first == null) {
                mainHandler.post(() -> callback.onError(new Exception("No YouTube instances available.")));
                return;
            }
            getStreamUrlInternal(videoId, callback, first, 0);
        });
    }

    public void cancel() {
        if (pendingSearch != null) { debounceHandler.removeCallbacks(pendingSearch); pendingSearch = null; }
        Call active = activeSearchCall.getAndSet(null);
        if (active != null) active.cancel();
    }

    // ── Search  ───────────────────────────────────────────────────────────────
    // Invidious: GET /api/v1/search?q=QUERY&type=video
    // Response : JSON array of video objects with videoId, title, author, videoThumbnails, lengthSeconds

    private void searchInternal(String query, YouTubeCallback<List<Song>> callback,
                                String instance, int generation, int attempt) {
        if (searchGeneration.get() != generation) return;

        Runnable doRequest = () -> {
            if (searchGeneration.get() != generation) return;

            String url = instance + "/api/v1/search?q=" + Uri.encode(query)
                    + "&type=video&fields=videoId,title,author,videoThumbnails,lengthSeconds";

            Call call = client.newCall(new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build());
            activeSearchCall.set(call);

            call.enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    if (call.isCanceled()) return;
                    Log.e(TAG, "Search FAIL [" + instance + "]: " + e.getMessage());
                    tryNextSearch(query, callback, instance, generation, attempt + 1);
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (call.isCanceled()) { response.close(); return; }
                    if (!response.isSuccessful()) {
                        Log.w(TAG, "Search HTTP " + response.code() + " [" + instance + "]");
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
                        Log.e(TAG, "Search parse error [" + instance + "]", e);
                        tryNextSearch(query, callback, instance, generation, attempt + 1);
                    } finally {
                        response.close();
                    }
                }
            });
        };

        long delay = backoffMs(attempt);
        if (delay > 0) mainHandler.postDelayed(doRequest, delay);
        else doRequest.run();
    }

    private void tryNextSearch(String query, YouTubeCallback<List<Song>> callback,
                               String failed, int generation, int attempt) {
        if (searchGeneration.get() != generation) return;
        if (attempt >= BACKOFF_MS.length) {
            mainHandler.post(() -> callback.onError(
                    new Exception("Search unavailable. Check your connection and try again.")));
            return;
        }
        String next = getNextAliveInstance(failed);
        if (next == null) {
            mainHandler.post(() -> callback.onError(
                    new Exception("Search unavailable. All sources are currently unreachable.")));
        } else {
            searchInternal(query, callback, next, generation, attempt);
        }
    }

    // ── Stream URL ────────────────────────────────────────────────────────────
    // Invidious: GET /api/v1/videos/{videoId}?fields=adaptiveFormats
    // Response : { adaptiveFormats: [{type, bitrate, url, ...}] }
    // We pick the highest-bitrate audio/mp4 (AAC) stream, falling back to audio/webm (Opus)

    private void getStreamUrlInternal(String videoId, YouTubeCallback<String> callback,
                                      String instance, int attempt) {
        Runnable doRequest = () -> {
            String url = instance + "/api/v1/videos/" + videoId + "?fields=adaptiveFormats";

            client.newCall(new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()).enqueue(new Callback() {

                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Stream FAIL [" + instance + "]: " + e.getMessage());
                    tryNextStream(videoId, callback, instance, attempt + 1);
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        response.close();
                        tryNextStream(videoId, callback, instance, attempt + 1);
                        return;
                    }
                    try {
                        JSONArray formats = new JSONObject(response.body().string())
                                .optJSONArray("adaptiveFormats");
                        if (formats == null || formats.length() == 0) {
                            tryNextStream(videoId, callback, instance, attempt + 1);
                            return;
                        }

                        String bestMp4 = null, bestOpus = null;
                        long mp4Bitrate = 0, opusBitrate = 0;

                        for (int i = 0; i < formats.length(); i++) {
                            JSONObject f = formats.getJSONObject(i);
                            String type = f.optString("type", "");       // e.g. "audio/mp4; codecs=\"mp4a.40.2\""
                            String streamUrl = f.optString("url", "");
                            long bitrate = f.optLong("bitrate", 0);
                            if (streamUrl.isEmpty()) continue;

                            if (type.contains("audio/mp4") && bitrate > mp4Bitrate) {
                                bestMp4 = streamUrl; mp4Bitrate = bitrate;
                            } else if (type.contains("audio/webm") && bitrate > opusBitrate) {
                                bestOpus = streamUrl; opusBitrate = bitrate;
                            }
                        }

                        String chosen = bestMp4 != null ? bestMp4
                                : bestOpus != null ? bestOpus
                                : formats.getJSONObject(0).optString("url", "");

                        if (chosen.isEmpty()) {
                            tryNextStream(videoId, callback, instance, attempt + 1);
                            return;
                        }
                        
                        markInstanceWorked(instance);
                        streamCache.put(videoId, new CachedUrl(chosen));

                        final String finalUrl = chosen;
                        mainHandler.post(() -> callback.onSuccess(finalUrl));
                    } catch (Exception e) {
                        Log.e(TAG, "Stream parse error [" + instance + "]", e);
                        tryNextStream(videoId, callback, instance, attempt + 1);
                    } finally {
                        response.close();
                    }
                }
            });
        };

        long delay = backoffMs(attempt);
        if (delay > 0) mainHandler.postDelayed(doRequest, delay);
        else doRequest.run();
    }

    private void tryNextStream(String videoId, YouTubeCallback<String> callback,
                               String failed, int attempt) {
        if (attempt >= BACKOFF_MS.length) {
            mainHandler.post(() -> callback.onError(
                    new Exception("Stream unavailable. Check your connection and try again.")));
            return;
        }
        String next = getNextAliveInstance(failed);
        if (next == null) {
            mainHandler.post(() -> callback.onError(
                    new Exception("Stream unavailable. All sources are currently unreachable.")));
        } else {
            getStreamUrlInternal(videoId, callback, next, attempt);
        }
    }

    // ── OkHttp client ─────────────────────────────────────────────────────────

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
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build();
        } catch (Exception e) {
            return new OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(12, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(false)
                    .build();
        }
    }
}