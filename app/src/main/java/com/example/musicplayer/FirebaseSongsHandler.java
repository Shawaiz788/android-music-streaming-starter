package com.example.musicplayer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FirebaseSongsHandler {
    private final DatabaseReference databaseReference;

    public FirebaseSongsHandler() {
        databaseReference = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("songs");
    }

    public void loadSongs() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot bucketSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot songSnapshot : bucketSnapshot.getChildren()) {
                        Song cloudSong = songSnapshot.getValue(Song.class);
                        if (cloudSong != null) {
                            cloudSong.setId(songSnapshot.getKey());
                            updateOrAddSong(cloudSong);
                        }
                    }
                }
                MyApplication.notifySongsLoaded();
            }

            private void updateOrAddSong(Song cloudSong) {
                boolean alreadyPresent = false;
                for (int i = 0; i < MyApplication.songs.size(); i++) {
                    Song s = MyApplication.songs.get(i);
                    if (s.getTitle().equalsIgnoreCase(cloudSong.getTitle()) && s.getArtist().equalsIgnoreCase(cloudSong.getArtist())) {
                        alreadyPresent = true;
                        MyApplication.songs.set(i, cloudSong);
                        break;
                    }
                }
                if (!alreadyPresent) {
                    MyApplication.songs.add(cloudSong);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseSongsHandler", "Load failed: " + error.getMessage());
            }
        });
    }

    public void syncLocalSongs(ArrayList<Song> localSongs) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (Song localSong : localSongs) {
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
                        addNewSong(localSong);
                    }
                }
                MyApplication.notifySongsLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseSongsHandler", "Sync failed: " + error.getMessage());
            }
        });
    }

    public void addNewSong(Song song) {
        if (song.getId() == null || song.getId().isEmpty() || song.getId().startsWith("local_")) {
            song.setId(databaseReference.push().getKey());
        }
        String bucket = getBucket(song.getTitle());
        databaseReference.child(bucket).child(song.getId()).setValue(song)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Saved to bucket " + bucket + ": " + song.getTitle()))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed saving: " + e.getMessage()));
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
}
