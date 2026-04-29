package com.example.musicplayer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyApplication extends Application {
    private static MyApplication instance;
    // Global Data Lists
    public static final ArrayList<Song> songs = new ArrayList<>();
    public static ArrayList<Song> recentSearches = new ArrayList<>();
    public static ArrayList<Song> favouriteSongs = new ArrayList<>();
    public static ArrayList<Album> favouriteAlbums = new ArrayList<>();
    public static ArrayList<Artist> favouriteArtists = new ArrayList<>();
    public static User currentUserInfo;

    // Handlers
    public static FirebaseSongsHandler songsHandler;
    public static FirebaseRecentSearchHandler recentSearchHandler;
    public static FirebaseRecentSearchHandler recentSearchHandler_older; // keeping unused as requested
    public static FirebaseFavouriteSongsHandler favouriteSongsHandler;
    public static FirebaseFavouriteAlbumsHandler favouriteAlbumsHandler;
    public static FirebaseFavouriteArtistHandler favouriteArtistHandler;
    public static FirebaseUserHandler userHandler;


    public interface OnSongsLoadedListener {
        void onSongsLoaded(ArrayList<Song> songs);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(User user);
    }

    private static final ArrayList<OnSongsLoadedListener> songListeners = new ArrayList<>();
    private static final ArrayList<OnUserLoadedListener> userListeners = new ArrayList<>();

    public static void subscribe(OnSongsLoadedListener listener) {
        if (!songListeners.contains(listener)) {
            songListeners.add(listener);
        }
        
        if (!songs.isEmpty()) {
            listener.onSongsLoaded(new ArrayList<>(songs));
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

    public static void notifySongsLoaded() {
        ArrayList<Song> copy = new ArrayList<>(songs);
        for (OnSongsLoadedListener listener : songListeners) {
            listener.onSongsLoaded(copy);
        }
    }

    public static void notifyUserLoaded() {
        if (currentUserInfo != null) {
            for (OnUserLoadedListener listener : userListeners) {
                listener.onUserLoaded(currentUserInfo);
            }
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }
    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        
        // Ensure we start with a fresh list to trigger the shimmer
        songs.clear();
        
        // 1. Initialize Songs Handler
        songsHandler = new FirebaseSongsHandler();

        // 2. Add local songs
        // addLocalSongs();

        // 3. Sync local songs with Firebase and Load from Cloud
        songsHandler.syncLocalSongs(new ArrayList<>(songs));
        songsHandler.loadSongs();

        // 4. Initialize user-specific handlers if logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            initHandlers(currentUser.getUid());
        }
    }

    public static void initHandlers(String userId) {
        userHandler = new FirebaseUserHandler(userId);
        recentSearchHandler = new FirebaseRecentSearchHandler(userId);
        favouriteSongsHandler = new FirebaseFavouriteSongsHandler(userId);
        favouriteAlbumsHandler = new FirebaseFavouriteAlbumsHandler(userId);
        favouriteArtistHandler = new FirebaseFavouriteArtistHandler(userId);
    }

    private void addLocalSongs() {
        songs.add(new Song(
                "local_1",
                "RUDE",
                "AnimeVibe",
                "Chill",
                "Pop",
                "No lyrics available",
                206000,
                getResourceUri(R.raw.rude),
                getResourceUri(R.drawable.rude)
        ));

        songs.add(new Song(
                "local_2",
                "hungama",
                "Hassan Raheem",
                "Hungama",
                "Pop",
                "دکھے مجھ میں کیا...\n (lyrics truncated)",
                179000,
                getResourceUri(R.raw.hungama),
                getResourceUri(R.drawable.hungama)
        ));
    }

    public String getResourceUri(int resId) {
        try {
            String type = getResources().getResourceTypeName(resId);
            String name = getResources().getResourceEntryName(resId);
            return "android.resource://" + getPackageName() + "/" + type + "/" + name;
        } catch (Exception e) {
            return "android.resource://" + getPackageName() + "/" + resId;
        }
    }

    public static ArrayList<Song> searchSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(recentSearches);
        }

        String lowerQuery = query.toLowerCase().trim();
        ArrayList<ScoredSong> scoredSongs = new ArrayList<>();

        for (Song song : songs) {
            int score = calculateScore(song, lowerQuery);
            if (score > 0) {
                scoredSongs.add(new ScoredSong(song, score));
            }
        }

        Collections.sort(scoredSongs, (o1, o2) -> Integer.compare(o2.score, o1.score));

        ArrayList<Song> results = new ArrayList<>();
        for (ScoredSong ss : scoredSongs) {
            results.add(ss.song);
        }
        return results;
    }

    private static int calculateScore(Song song, String query) {
        int score = 0;
        String title = (song.getTitle() != null ? song.getTitle() : "").toLowerCase();
        String artist = (song.getArtist() != null ? song.getArtist() : "").toLowerCase();
        String album = (song.getAlbum() != null ? song.getAlbum() : "").toLowerCase();

        if (title.equals(query)) score += 100;
        else if (title.startsWith(query)) score += 80;
        else if (title.contains(query)) score += 40;

        if (artist.equals(query)) score += 70;
        else if (artist.startsWith(query)) score += 50;
        else if (artist.contains(query)) score += 20;

        if (album.equals(query)) score += 30;
        else if (album.contains(query)) score += 10;

        return score;
    }

    private static class ScoredSong {
        Song song;
        int score;
        ScoredSong(Song song, int score) {
            this.song = song;
            this.score = score;
        }
    }
}
