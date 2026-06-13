package com.example.musicplayer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FirebaseAlbumsHandler {
    private final DatabaseReference databaseReference;

    public interface OnSyncCompleteListener {
        void onSyncComplete(Map<String, String> idMap);
    }

    public FirebaseAlbumsHandler() {
        databaseReference = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("albums");
    }

    public void loadAlbums() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Keep Deezer and YouTube albums
                ArrayList<Album> nonFirebaseAlbums = new ArrayList<>();
                for (Album a : MyApplication.allAlbums) {
                    if (a.getId() != null && (a.getId().startsWith("deezer_") || a.getId().startsWith("yt_playlist_"))) {
                        nonFirebaseAlbums.add(a);
                    }
                }
                
                MyApplication.allAlbums.clear();
                MyApplication.allAlbums.addAll(nonFirebaseAlbums);

                for (DataSnapshot bucketSnapshot : snapshot.getChildren()) {
                    for (DataSnapshot albumSnapshot : bucketSnapshot.getChildren()) {
                        Album cloudAlbum = albumSnapshot.getValue(Album.class);
                        if (cloudAlbum != null) {
                            cloudAlbum.setId(albumSnapshot.getKey());
                            if (!MyApplication.allAlbums.contains(cloudAlbum)) {
                                MyApplication.allAlbums.add(cloudAlbum);
                            }
                        }
                    }
                }

                if (MyApplication.cacheManager != null) {
                    MyApplication.cacheManager.saveAllAlbums(MyApplication.allAlbums);
                }

                MyApplication.notifyAlbumsLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseAlbumsHandler", "Load failed: " + error.getMessage());
            }
        });
    }

    public void syncLocalAlbums(ArrayList<Album> localAlbums, OnSyncCompleteListener listener) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> idMap = new HashMap<>();
                int[] remaining = {localAlbums.size()};
                
                if (localAlbums.isEmpty()) {
                    if (listener != null) listener.onSyncComplete(idMap);
                    return;
                }

                for (Album localAlbum : localAlbums) {
                    String oldId = localAlbum.getId();
                    boolean existsInCloud = false;
                    String bucket = getBucket(localAlbum.getTitle());
                    DataSnapshot bucketSnapshot = snapshot.child(bucket);
                    
                    for (DataSnapshot cloudSnapshot : bucketSnapshot.getChildren()) {
                        Album cloudAlbum = cloudSnapshot.getValue(Album.class);
                        if (cloudAlbum != null && cloudAlbum.getTitle() != null && cloudAlbum.getArtist() != null 
                                && cloudAlbum.getTitle().equalsIgnoreCase(localAlbum.getTitle()) 
                                && cloudAlbum.getArtist().equalsIgnoreCase(localAlbum.getArtist())) {
                            existsInCloud = true;
                            String cloudId = cloudSnapshot.getKey();
                            localAlbum.setId(cloudId);
                            idMap.put(oldId, cloudId);
                            break;
                        }
                    }

                    if (!existsInCloud) {
                        addNewAlbum(localAlbum, (newId) -> {
                            idMap.put(oldId, newId);
                            remaining[0]--;
                            if (remaining[0] == 0 && listener != null) {
                                listener.onSyncComplete(idMap);
                            }
                        });
                    } else {
                        remaining[0]--;
                        if (remaining[0] == 0 && listener != null) {
                            listener.onSyncComplete(idMap);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseAlbumsHandler", "Sync failed: " + error.getMessage());
                if (listener != null) listener.onSyncComplete(new HashMap<>());
            }
        });
    }

    public interface OnAlbumAddedListener {
        void onAdded(String newId);
    }

    public void addNewAlbum(Album album, OnAlbumAddedListener listener) {
        if (album.getId() == null || album.getId().isEmpty() || album.getId().startsWith("local_")) {
            album.setId(databaseReference.push().getKey());
        }
        String bucket = getBucket(album.getTitle());
        databaseReference.child(bucket).child(album.getId()).setValue(album)
                .addOnSuccessListener(aVoid -> {
                    Log.d("FirebaseAlbums", "Saved to bucket " + bucket + ": " + album.getTitle());
                    if (listener != null) listener.onAdded(album.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseAlbums", "Failed saving: " + e.getMessage());
                    if (listener != null) listener.onAdded(album.getId());
                });
    }

    public void updateAlbum(Album album) {
        String bucket = getBucket(album.getTitle());
        databaseReference.child(bucket).child(album.getId()).setValue(album);
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
