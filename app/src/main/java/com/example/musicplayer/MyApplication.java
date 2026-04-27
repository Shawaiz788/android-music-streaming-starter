package com.example.musicplayer;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyApplication extends Application {
    // Made final for consistent synchronization
    public static final ArrayList<Song> songs = new ArrayList<>();
    private DatabaseReference databaseReference;

    public interface OnSongsLoadedListener {
        void onSongsLoaded(ArrayList<Song> songs);
    }

    private static final ArrayList<OnSongsLoadedListener> listeners = new ArrayList<>();

    public static void subscribe(OnSongsLoadedListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        synchronized (songs) {
            if (!songs.isEmpty()) {
                listener.onSongsLoaded(new ArrayList<>(songs));
            }
        }
    }

    public static void unsubscribe(OnSongsLoadedListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 1. Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("songs");

        // 2. Add local songs
        addLocalSongs();

        // 3. Sync local songs with Firebase (alphabet-aware)
        syncLocalSongsWithFirebase();

        // 4. Start fetching all songs from Firebase (alphabet-aware)
        loadSongsFromFirebase();
    }

    private void addLocalSongs() {
        synchronized (songs) {
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
    }


    private String getBucket(String title) {
        if (title == null || title.trim().isEmpty()) return "Other";
        char firstChar = Character.toUpperCase(title.trim().charAt(0));
        if (firstChar >= 'A' && firstChar <= 'Z') {
            return String.valueOf(firstChar);
        } else {
            return "Other";
        }
    }

    private void syncLocalSongsWithFirebase() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                synchronized (songs) {
                    for (Song localSong : songs) {
                        boolean existsInCloud = false;
                        String bucket = getBucket(localSong.getTitle());
                        DataSnapshot bucketSnapshot = snapshot.child(bucket);
                        
                        for (DataSnapshot cloudSnapshot : bucketSnapshot.getChildren()) {
                            Song cloudSong = cloudSnapshot.getValue(Song.class);
                            if (cloudSong != null && cloudSong.getTitle() != null && cloudSong.getArtist() != null 
                                    && cloudSong.getTitle().equalsIgnoreCase(localSong.getTitle()) 
                                    && cloudSong.getArtist().equalsIgnoreCase(localSong.getArtist())) {
                                existsInCloud = true;
                                localSong.setId(cloudSnapshot.getKey());
                                break;
                            }
                        }

                        if (!existsInCloud) {
                            addNewSongToFirebase(localSong);
                        }
                    }
                }
                notifyListeners();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Sync failed: " + error.getMessage());
            }
        });
    }

    public void loadSongsFromFirebase() {
        // Listening to the root node to catch all buckets
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                synchronized (songs) {
                    for (DataSnapshot bucketSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot songSnapshot : bucketSnapshot.getChildren()) {
                            Song cloudSong = songSnapshot.getValue(Song.class);
                            if (cloudSong != null) {
                                cloudSong.setId(songSnapshot.getKey());
                                updateOrAddSong(cloudSong);
                            }
                        }
                    }
                }
                notifyListeners();
            }

            private void updateOrAddSong(Song cloudSong) {
                boolean alreadyPresent = false;
                for (int i = 0; i < songs.size(); i++) {
                    Song s = songs.get(i);
                    if (s.getTitle().equalsIgnoreCase(cloudSong.getTitle()) && s.getArtist().equalsIgnoreCase(cloudSong.getArtist())) {
                        alreadyPresent = true;
                        songs.set(i, cloudSong);
                        break;
                    }
                }
                if (!alreadyPresent) {
                    songs.add(cloudSong);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Load failed: " + error.getMessage());
            }
        });
    }

    public static List<Song> searchSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            synchronized (songs) {
                return new ArrayList<>(songs);
            }
        }

        String lowerQuery = query.toLowerCase().trim();
        List<ScoredSong> scoredSongs = new ArrayList<>();

        synchronized (songs) {
            for (Song song : songs) {
                int score = calculateScore(song, lowerQuery);
                if (score > 0) {
                    scoredSongs.add(new ScoredSong(song, score));
                }
            }
        }

        Collections.sort(scoredSongs, (o1, o2) -> Integer.compare(o2.score, o1.score));

        List<Song> results = new ArrayList<>();
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

    public void addNewSongToFirebase(Song song) {
        if (song.getId() == null || song.getId().isEmpty() || song.getId().startsWith("local_")) {
            String id = databaseReference.push().getKey();
            song.setId(id);
        }

        // Wisdom: Bucket songs by starting letter to keep the tree shallow and organized.
        // This is much more efficient than a flat list for large datasets.
        String bucket = getBucket(song.getTitle());
        databaseReference.child(bucket).child(song.getId()).setValue(song)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Saved to bucket " + bucket + ": " + song.getTitle()))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed saving: " + e.getMessage()));
    }

    public void addSongToFirebase(String title, String artist, String album, String genre, String lyrics, int duration, String songUrl, String imageUrl) {
        Song song = new Song(null, title, artist, album, genre, lyrics, duration, songUrl, imageUrl);
        addNewSongToFirebase(song);
    }

    public String getResourceUri(int resId) {
        return "android.resource://" + getPackageName() + "/" + resId;
    }

    private void notifyListeners() {
        List<OnSongsLoadedListener> currentListeners;
        synchronized (listeners) {
            currentListeners = new ArrayList<>(listeners);
        }
        synchronized (songs) {
            ArrayList<Song> copy = new ArrayList<>(songs);
            for (OnSongsLoadedListener listener : currentListeners) {
                listener.onSongsLoaded(copy);
            }
        }
    }
}
