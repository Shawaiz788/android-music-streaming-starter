package com.example.musicplayer;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MyApplication extends Application {
    private static MyApplication instance;
    public static final ArrayList<Song> songs = new ArrayList<>();
    public static final ArrayList<Song> newReleases = new ArrayList<>();
    public static final ArrayList<Album> allAlbums = new ArrayList<>();
    public static final ArrayList<Song> recentSearches = new ArrayList<>();
    public static final ArrayList<Song> favouriteSongs = new ArrayList<>();
    public static final ArrayList<Album> favouriteAlbums = new ArrayList<>();
    public static final ArrayList<Artist> favouriteArtists = new ArrayList<>();
    public static final ArrayList<Playlist> favouritePlaylists = new ArrayList<>();
    public static final ArrayList<Song> downloadedSongs = new ArrayList<>();
    public static final ArrayList<Song> exploreSongs = new ArrayList<>();

    public static User currentUserInfo;
    public static long sessionSeed;

    public static FirebaseSongsHandler songsHandler;
    public static FirebaseRecentSearchHandler recentSearchHandler;
    public static FirebaseRecentSearchHandler recentSearchHandler_older;
    public static FirebaseFavouriteSongsHandler favouriteSongsHandler;
    public static FirebaseFavouriteAlbumsHandler favouriteAlbumsHandler;
    public static FirebaseAlbumsHandler albumHandler;
    public static FirebaseFavouriteArtistHandler favouriteArtistHandler;
    public static FirebaseUserHandler userHandler;
    public static FirebasePlaylistHandler playlistHandler;
    public static YouTubeApiHandler youtubeApiHandler;
    public static CacheManager cacheManager;

    public interface OnSongsLoadedListener {
        void onSongsLoaded(ArrayList<Song> songs);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
    }

    public interface OnAlbumsLoadedListener {
        void onAlbumsLoaded(ArrayList<Album> albums);
    }

    public interface OnPlaylistsLoadedListener {
        void onPlaylistsLoaded(ArrayList<Playlist> playlists);
    }

    public interface OnFavouriteSongsLoadedListener {
        void onFavouriteSongsLoaded(ArrayList<Song> favouriteSongs);
    }

    public interface OnDownloadsLoadedListener {
        void onDownloadsLoaded(ArrayList<Song> downloadedSongs);
    }

    public interface OnFavouriteArtistsLoadedListener {
        void onFavouriteArtistsLoaded(ArrayList<Artist> favouriteArtists);
    }

    private static final ArrayList<OnSongsLoadedListener> songListeners = new ArrayList<>();
    private static final ArrayList<OnUserLoadedListener> userListeners = new ArrayList<>();
    private static final ArrayList<OnAlbumsLoadedListener> albumListeners = new ArrayList<>();
    private static final ArrayList<OnPlaylistsLoadedListener> playlistListeners = new ArrayList<>();
    private static final ArrayList<OnFavouriteSongsLoadedListener> favouriteSongsListeners = new ArrayList<>();
    private static final ArrayList<OnDownloadsLoadedListener> downloadListeners = new ArrayList<>();
    private static final ArrayList<OnFavouriteArtistsLoadedListener> favouriteArtistListeners = new ArrayList<>();

    public static void subscribe(OnSongsLoadedListener listener) {
        if (!songListeners.contains(listener)) {
            songListeners.add(listener);
        }
        
        if (!newReleases.isEmpty()) {
            listener.onSongsLoaded(new ArrayList<>(newReleases));
        }
    }

    public static void unsubscribe(OnSongsLoadedListener listener) {
        songListeners.remove(listener);
    }

    public static void subscribeUser(OnUserLoadedListener listener) {
        userListeners.add(listener);
        if (currentUserInfo != null) {
            listener.onUserLoaded(currentUserInfo);
        }
    }

    public static void unsubscribeUser(OnUserLoadedListener listener) {
        userListeners.remove(listener);
    }

    public static void subscribeAlbums(OnAlbumsLoadedListener listener) {
        if (!albumListeners.contains(listener)) {
            albumListeners.add(listener);
        }
        if (!allAlbums.isEmpty()) {
            listener.onAlbumsLoaded(new ArrayList<>(allAlbums));
        }
    }

    public static void unsubscribeAlbums(OnAlbumsLoadedListener listener) {
        albumListeners.remove(listener);
    }

    public static void subscribePlaylists(OnPlaylistsLoadedListener listener) {
        if (!playlistListeners.contains(listener)) {
            playlistListeners.add(listener);
        }
        if (!favouritePlaylists.isEmpty()) {
            listener.onPlaylistsLoaded(new ArrayList<>(favouritePlaylists));
        }
    }

    public static void unsubscribePlaylists(OnPlaylistsLoadedListener listener) {
        playlistListeners.remove(listener);
    }

    public static void subscribeFavouriteSongs(OnFavouriteSongsLoadedListener listener) {
        if (!favouriteSongsListeners.contains(listener)) {
            favouriteSongsListeners.add(listener);
        }
        if (!favouriteSongs.isEmpty()) {
            listener.onFavouriteSongsLoaded(new ArrayList<>(favouriteSongs));
        }
    }

    public static void unsubscribeFavouriteSongs(OnFavouriteSongsLoadedListener listener) {
        favouriteSongsListeners.remove(listener);
    }

    public static void subscribeDownloads(OnDownloadsLoadedListener listener) {
        if (!downloadListeners.contains(listener)) {
            downloadListeners.add(listener);
        }
        if (!downloadedSongs.isEmpty()) {
            listener.onDownloadsLoaded(new ArrayList<>(downloadedSongs));
        }
    }

    public static void unsubscribeDownloads(OnDownloadsLoadedListener listener) {
        downloadListeners.remove(listener);
    }

    public static void subscribeFavouriteArtists(OnFavouriteArtistsLoadedListener listener) {
        if (!favouriteArtistListeners.contains(listener)) {
            favouriteArtistListeners.add(listener);
        }
        if (!favouriteArtists.isEmpty()) {
            listener.onFavouriteArtistsLoaded(new ArrayList<>(favouriteArtists));
        }
    }

    public static void unsubscribeFavouriteArtists(OnFavouriteArtistsLoadedListener listener) {
        favouriteArtistListeners.remove(listener);
    }

    public static void notifySongsLoaded() {
        for (Playlist p : favouritePlaylists) {
            p.calculateAndSetDuration(songs);
        }
        
        ArrayList<Song> copy = new ArrayList<>(newReleases);
        for (OnSongsLoadedListener listener : songListeners) {
            listener.onSongsLoaded(copy);
        }
        
        notifyPlaylistsLoaded();
    }

    public static void notifyUserLoaded() {
        if (currentUserInfo != null) {
            for (OnUserLoadedListener listener : userListeners) {
                listener.onUserLoaded(currentUserInfo);
            }
        }
    }

    public static void notifyAlbumsLoaded() {
        ArrayList<Album> copy = new ArrayList<>(allAlbums);
        for (OnAlbumsLoadedListener listener : albumListeners) {
            listener.onAlbumsLoaded(copy);
        }
    }

    public static void notifyPlaylistsLoaded() {
        ArrayList<Playlist> copy = new ArrayList<>(favouritePlaylists);
        for (OnPlaylistsLoadedListener listener : playlistListeners) {
            listener.onPlaylistsLoaded(copy);
        }
    }

    public static void notifyFavouriteSongsLoaded() {
        ArrayList<Song> copy = new ArrayList<>(favouriteSongs);
        for (OnFavouriteSongsLoadedListener listener : favouriteSongsListeners) {
            listener.onFavouriteSongsLoaded(copy);
        }
    }

    public static void notifyDownloadsLoaded() {
        ArrayList<Song> copy = new ArrayList<>(downloadedSongs);
        for (OnDownloadsLoadedListener listener : downloadListeners) {
            listener.onDownloadsLoaded(copy);
        }
    }

    public static void notifyFavouriteArtistsLoaded() {
        ArrayList<Artist> copy = new ArrayList<>(favouriteArtists);
        for (OnFavouriteArtistsLoadedListener listener : favouriteArtistListeners) {
            listener.onFavouriteArtistsLoaded(copy);
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }
    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        
        // Start MusicService early to initialize YouTube player and foreground state
        android.content.Intent serviceIntent = new android.content.Intent(this, MusicService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        sessionSeed = new Random().nextLong();
        
        songs.clear();
        newReleases.clear();
        allAlbums.clear();
        downloadedSongs.clear();

        songsHandler = new FirebaseSongsHandler();
        albumHandler = new FirebaseAlbumsHandler();
        youtubeApiHandler = new YouTubeApiHandler();
        cacheManager = new CacheManager(this);

        // Load cached data
        newReleases.addAll(cacheManager.loadNewReleases());
        allAlbums.addAll(cacheManager.loadAllAlbums());
        songs.addAll(cacheManager.loadTopSongs()); // We use 'songs' as base for Top Songs
        exploreSongs.addAll(cacheManager.loadExploreSongs());

        if (!newReleases.isEmpty()) notifySongsLoaded();
        if (!allAlbums.isEmpty()) notifyAlbumsLoaded();

        DBManager dbManager = new DBManager(this);
        dbManager.Open();
        downloadedSongs.addAll(dbManager.getAllDownloadedSongs());
        dbManager.Close();

        syncData();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            initHandlers(currentUser.getUid());
        }
    }

    public static void prefetchAlbumSongs(Album album) {
        if (album == null || album.getId() == null) return;
        
        // Already prefetched?
        for (Song s : songs) {
            if (album.getId().equals(s.getAlbumId())) return;
        }

        String query = album.getTitle() + " " + album.getArtist();
        youtubeApiHandler.searchImmediate(query, new YouTubeApiHandler.YouTubeCallback<List<Song>>() {
            @Override
            public void onSuccess(List<Song> result) {
                for (Song s : result) {
                    s.setAlbumId(album.getId());
                    s.setAlbum(album.getTitle());
                    if (!songs.contains(s)) {
                        songs.add(s);
                    }
                }
            }
            @Override public void onError(Exception e) {}
        });
    }

    private void syncData() {
        albumHandler.syncLocalAlbums(new ArrayList<>(allAlbums), albumIdMap -> {

            for (Song song : songs) {
                if (song.getAlbumId() != null && albumIdMap.containsKey(song.getAlbumId())) {
                    song.setAlbumId(albumIdMap.get(song.getAlbumId()));
                }
            }

            songsHandler.syncLocalSongs(new ArrayList<>(songs), songIdMap -> {

                for (Album album : allAlbums) {
                    List<String> newSongIds = new ArrayList<>();
                    boolean changed = false;
                    for (String oldSongId : album.getSongIds()) {
                        if (songIdMap.containsKey(oldSongId)) {
                            newSongIds.add(songIdMap.get(oldSongId));
                            changed = true;
                        } else {
                            newSongIds.add(oldSongId);
                        }
                    }
                    if (changed) {
                        album.setSongIds(newSongIds);
                        albumHandler.updateAlbum(album);
                    }
                }

                songsHandler.loadSongs();
                albumHandler.loadAlbums();
                
                youtubeApiHandler.searchImmediate("Latest Hit Songs 2025", new YouTubeApiHandler.YouTubeCallback<List<Song>>() {
                    @Override
                    public void onSuccess(List<Song> result) {
                        newReleases.clear();
                        newReleases.addAll(result);
                        
                        for (Song s : result) {
                            if (!songs.contains(s)) {
                                songs.add(s);
                            }
                        }

                        cacheManager.saveNewReleases(newReleases);
                        notifySongsLoaded();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("MyApplication", "Failed to load YouTube trending", e);
                    }
                });

                youtubeApiHandler.searchAlbums("Official Music Albums 2025", new YouTubeApiHandler.YouTubeCallback<List<Album>>() {
                    @Override
                    public void onSuccess(List<Album> result) {
                        for (Album a : result) {
                            if (!allAlbums.contains(a)) {
                                allAlbums.add(a);
                            }
                        }
                        
                        // Prefetch songs for the top albums
                        for (int i = 0; i < Math.min(10, allAlbums.size()); i++) {
                            prefetchAlbumSongs(allAlbums.get(i));
                        }

                        cacheManager.saveAllAlbums(allAlbums);
                        notifyAlbumsLoaded();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("MyApplication", "Failed to load YouTube albums", e);
                    }
                });
            });
        });
    }

    public static void initHandlers(String userId) {
        userHandler = new FirebaseUserHandler(userId);
        recentSearchHandler = new FirebaseRecentSearchHandler(userId);
        favouriteSongsHandler = new FirebaseFavouriteSongsHandler(userId);
        favouriteAlbumsHandler = new FirebaseFavouriteAlbumsHandler(userId);
        favouriteArtistHandler = new FirebaseFavouriteArtistHandler(userId);
        playlistHandler = new FirebasePlaylistHandler(userId);
        albumHandler = new FirebaseAlbumsHandler();
    }
}
