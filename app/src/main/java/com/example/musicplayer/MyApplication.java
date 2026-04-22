package com.example.musicplayer;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MyApplication extends Application {
    public static ArrayList<Song> songs;
    private DatabaseReference databaseReference;

    @Override
    public void onCreate() {
        super.onCreate();
        songs = new ArrayList<>();
        
        // 1. Initialize Firebase Database reference with your specific URL from the screenshot
        databaseReference = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("songs");

        // 2. Add local songs to the local list
        addLocalSongs();

        // 3. Sync local songs with Firebase (add only if they don't exist)
        syncLocalSongsWithFirebase();

        // 4. Start fetching all songs from Firebase
        loadSongsFromFirebase();
    }

    private void addLocalSongs() {
//        songs.add(new Song(
//                "local_1",
//                "RUDE",
//                "AnimeVibe",
//                "Chill",
//                "Pop",
//                "No lyrics available",
//                206000,
//                getResourceUri(R.raw.rude),
//                getResourceUri(R.drawable.rude)
//        ));
//
//        songs.add(new Song(
//                "local_2",
//                "hungama",
//                "Hassan Raheem",
//                "Hungama",
//                "Pop",
//                "دکھے مجھ میں کیا...\n (lyrics truncated)",
//                179000,
//                getResourceUri(R.raw.hungama),
//                getResourceUri(R.drawable.hungama)
//        ));
    }

    private void syncLocalSongsWithFirebase() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Song localSong : songs) {
                    boolean existsInCloud = false;
                    for (DataSnapshot cloudSnapshot : snapshot.getChildren()) {
                        Song cloudSong = cloudSnapshot.getValue(Song.class);
                        if (cloudSong != null && cloudSong.getTitle() != null && cloudSong.getArtist() != null 
                                && cloudSong.getTitle().equals(localSong.getTitle()) 
                                && cloudSong.getArtist().equals(localSong.getArtist())) {
                            existsInCloud = true;
                            localSong.setId(cloudSong.getId());
                            break;
                        }
                    }

                    if (!existsInCloud) {
                        addNewSongToFirebase(localSong);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Sync failed: " + error.getMessage());
            }
        });
    }

    public void loadSongsFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Song cloudSong = postSnapshot.getValue(Song.class);
                    if (cloudSong != null) {
                        boolean alreadyPresent = false;
                        for (int i = 0; i < songs.size(); i++) {
                            Song s = songs.get(i);
                            if (s.getTitle().equals(cloudSong.getTitle()) && s.getArtist().equals(cloudSong.getArtist())) {
                                alreadyPresent = true;
                                songs.set(i, cloudSong);
                                break;
                            }
                        }
                        if (!alreadyPresent) {
                            songs.add(cloudSong);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Load failed: " + error.getMessage());
            }
        });
    }

    public void addNewSongToFirebase(Song song) {
        if (song.getId() == null || song.getId().isEmpty() || song.getId().startsWith("local_")) {
            String id = databaseReference.push().getKey();
            song.setId(id);
        }

        databaseReference.child(song.getId()).setValue(song)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Success: " + song.getTitle()))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed: " + e.getMessage()));
    }

    public void addSongToFirebase(String title, String artist, String album, String genre, String lyrics, int duration, String songUrl, String imageUrl) {
        String id = databaseReference.push().getKey();
        Song song = new Song(id, title, artist, album, genre, lyrics, duration, songUrl, imageUrl);
        addNewSongToFirebase(song);
    }

    public String getResourceUri(int resId) {
        return "android.resource://" + getPackageName() + "/" + resId;
    }
}
