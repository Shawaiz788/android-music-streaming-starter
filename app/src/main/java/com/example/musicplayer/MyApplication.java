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
        
        sessionSeed = new Random().nextLong();
        
        songs.clear();
        newReleases.clear();
        allAlbums.clear();
        downloadedSongs.clear();

        songsHandler = new FirebaseSongsHandler();
        albumHandler = new FirebaseAlbumsHandler();

        addLocal();

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

    private void addLocal() {

        String cokeStudioId = "local_album_1";
        String anuvJainId = "local_artist_1";

        Album cokeStudioAlbum = new Album(
                cokeStudioId,
                "Coke Studio Bharat (Season 3)",
                "Anuv Jain",
                getResourceUri(R.drawable.bharat3),
                "2025"
        );

        Song song1 = new Song(
                "local_song_1",
                "Arz Kiya Hai",
                "Anuv Jain",
                "Coke Studio Bharat (Season 3)",
                "Indie-Pop",
                "No lyrics available",
                294000,
                "https://drive.google.com/uc?export=download&id=1TCmh2XdGRj0_I9BumsGfT7Sr6WVKHEYI",
                "https://drive.google.com/uc?export=download&id=1ZoFJ27h9PKXQi3ufe-FOJiqx-ipOoFyo",
                cokeStudioId,
                anuvJainId
        );

        Song song2 = new Song(
                "local_song_2",
                "Alag Aasmaan",
                "Anuv Jain",
                "Coke Studio Bharat (Season 3)",
                "Indie-Pop",
                "No lyrics available",
                213000,
                "https://drive.google.com/uc?export=download&id=18oKabt5zEsmt9EIDsdtVF6591M_c6-VW",
                "https://drive.google.com/uc?export=download&id=1MlwcQy2uLcKAIjZlsa9exjWC0169JjGd",
                cokeStudioId,
                anuvJainId
        );

        allAlbums.add(cokeStudioAlbum);
        songs.add(song1);
        songs.add(song2);

        cokeStudioAlbum.getSongIds().add(song1.getId());
        cokeStudioAlbum.getSongIds().add(song2.getId());

        String album2Id = "local_album_2";
        String artist2Id = "local_artist_2";
        Album album2 = new Album(album2Id, "P-Pop culture", "Karan Aujila & Ikky", "https://drive.google.com/uc?export=download&id=1FdRbkB2gfQgtk0f9x4HcgHgJ2BBUESrz", "2025");
        Song s3 = new Song("local_song_3", "For A Reason", "Karan Aujila & Ikky", "P-Pop culture", "P-Pop", "No lyrics", 309000, "https://drive.google.com/uc?export=download&id=1QND4XxpHAbV0p_rRdB9e63j7FvoL8d-4", "https://drive.google.com/uc?export=download&id=1mYRf5SjqWhhsqdYeo8LxyIo5Jmz6Lukr", album2Id, artist2Id);
        Song s4 = new Song("local_song_4", "Boyfriend", "Karan Aujila & Ikky", "P-Pop culture", "P-Pop", "No lyrics", 242000, "https://drive.google.com/uc?export=download&id=19xLDf2xDQOga1I3nBhNH61S83FAEJzC7", "https://drive.google.com/uc?export=download&id=1BQpaKQTJmiJIy-oKRDQVaUV8WYGMiHOh", album2Id, artist2Id);
        allAlbums.add(album2); songs.add(s3); songs.add(s4);
        album2.getSongIds().add(s3.getId()); album2.getSongIds().add(s4.getId());

        String album3Id = "local_album_3";
        String artist3Id = "local_artist_3";
        Album album3 = new Album(album3Id, "Open Letter", "Talha Anjum", "https://drive.google.com/uc?export=download&id=1qMrBo520q_QkYBZ1LbEfZtIn3Hjp-tl_", "2023");
        Song s5 = new Song("local_song_5", "Happy Hour", "Talha Anjum", "Open Letter", "Urdu Hip Hop", "No lyrics", 315000, "https://drive.google.com/uc?export=download&id=13O4BX4TZPAvXSVTGNTU28torYT_Bnfm6", "https://drive.google.com/uc?export=download&id=1qMrBo520q_QkYBZ1LbEfZtIn3Hjp-tl_", album3Id, artist3Id);
        Song s6 = new Song("local_song_6", "Studio Gangsters", "Talha Anjum", "Open Letter", "Urdu Hip Hop", "No lyrics", 408000, "https://drive.google.com/uc?export=download&id=1jF5SW4VQLWoRIhtH7Bw3VTOaOG7tC5mW", "https://drive.google.com/uc?export=download&id=1qMrBo520q_QkYBZ1LbEfZtIn3Hjp-tl_", album3Id, artist3Id);
        allAlbums.add(album3); songs.add(s5); songs.add(s6);
        album3.getSongIds().add(s5.getId()); album3.getSongIds().add(s6.getId());
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
        if (query == null || query.isBlank()) return 0;

        String q      = query.trim().toLowerCase();
        String title  = song.getTitle()  != null ? song.getTitle().trim().toLowerCase()  : "";
        String artist = song.getArtist() != null ? song.getArtist().trim().toLowerCase() : "";
        String album  = song.getAlbum()  != null ? song.getAlbum().trim().toLowerCase()  : "";

        int score = 0;

        if (title.equals(q))  score += 100;
        if (artist.equals(q)) score += 70;
        if (album.equals(q))  score += 30;

        if (title.contains(q)  || q.contains(title))  score += title.startsWith(q)  || q.startsWith(title)  ? 80 : 40;
        if (artist.contains(q) || q.contains(artist)) score += artist.startsWith(q) || q.startsWith(artist) ? 50 : 20;
        if (album.contains(q)  || q.contains(album))  score += 10;

        score += (int) (fuzzy(title,  q) * 80);
        score += (int) (fuzzy(artist, q) * 50);
        score += (int) (fuzzy(album,  q) * 20);

        for (String word : q.split("\\s+")) {
            if (word.length() < 3) continue;
            score += (int) (fuzzy(title,  word) * 20);
            score += (int) (fuzzy(artist, word) * 15);
            score += (int) (fuzzy(album,  word) *  5);
        }

        return score;
    }


    private static double fuzzy(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;

        int m = a.length(), n = b.length();

        if (Math.abs(m - n) > Math.max(m, n) / 2) return 0.0;

        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;

        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                curr[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? prev[j - 1]
                        : 1 + Math.min(prev[j - 1], Math.min(prev[j], curr[j - 1]));
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }

        int distance = prev[n];
        double similarity = 1.0 - (double) distance / Math.max(m, n);

        return similarity >= 0.6 ? similarity : 0.0;
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
