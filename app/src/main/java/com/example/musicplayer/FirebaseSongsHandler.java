package com.example.musicplayer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseSongsHandler {
    private final DatabaseReference databaseReference;
    private final DatabaseReference playCountsReference;

    public interface OnSyncCompleteListener {
        void onSyncComplete(Map<String, String> idMap);
    }

    public interface OnPlayCountsLoadedListener {
        void onPlayCountsLoaded(Map<String, Integer> playCounts);
    }

    private final Map<String, Song> firebaseSongsMap = new HashMap<>();

    public FirebaseSongsHandler() {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/");
        databaseReference = db.getReference("songs");
        playCountsReference = db.getReference("play_counts");
    }

    public void loadSongs() {
        // Independent listener for standard Firebase songs (now including played YouTube songs)
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                firebaseSongsMap.clear();
                // Support both bucketed and non-bucketed structures
                for (DataSnapshot node : snapshot.getChildren()) {
                    if (node.hasChild("title") || node.hasChild("artist")) {
                        // Likely a direct song node
                        Song s = node.getValue(Song.class);
                        if (s != null) {
                            s.setId(node.getKey());
                            firebaseSongsMap.put(s.getId(), s);
                        }
                    } else {
                        // Likely a bucket (e.g. "A", "B")
                        for (DataSnapshot songSnapshot : node.getChildren()) {
                            Song s = songSnapshot.getValue(Song.class);
                            if (s != null) {
                                s.setId(songSnapshot.getKey());
                                firebaseSongsMap.put(s.getId(), s);
                            }
                        }
                    }
                }
                mergeAndNotify();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseSongsHandler", "Songs load failed: " + error.getMessage());
            }
        });
    }

    private synchronized void mergeAndNotify() {
        Map<String, Song> merged = new HashMap<>();
        
        // 1. First, include songs that are in the "newReleases" (Trending) list
        for (Song s : MyApplication.newReleases) {
            if (s.getId() != null) {
                merged.put(s.getId(), s);
            }
        }
        
        // 2. Add all standard Firebase songs (this node includes buckets and saved YouTube songs)
        for (Song s : firebaseSongsMap.values()) {
            if (s.getId() != null) {
                merged.put(s.getId(), s);
            }
        }
        
        // Update the global songs list
        ArrayList<Song> finalSongs = new ArrayList<>(merged.values());
        
        // Update the static list in a safe way
        MyApplication.songs.clear();
        MyApplication.songs.addAll(finalSongs);
        
        Log.d("FirebaseSongsHandler", "Merged songs count: " + MyApplication.songs.size());

        MyApplication.notifySongsLoaded();
    }

    public void incrementPlayCount(Song song) {
        if (song == null || song.getId() == null) return;
        
        // Increment count
        playCountsReference.child(song.getId()).setValue(ServerValue.increment(1));
        
        // If it's a YouTube song, also save its metadata to the main 'songs' node
        // so it can be retrieved and shown in the Top Songs list later.
        if (song.getId().startsWith("youtube_")) {
            String bucket = getBucket(song.getTitle());
            databaseReference.child(bucket).child(song.getId()).setValue(song);
        }
    }

    public void loadPlayCounts(OnPlayCountsLoadedListener listener) {
        playCountsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Integer> playCounts = new HashMap<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Object val = ds.getValue();
                    if (val instanceof Long) {
                        playCounts.put(ds.getKey(), ((Long) val).intValue());
                    } else if (val instanceof Integer) {
                        playCounts.put(ds.getKey(), (Integer) val);
                    }
                }
                if (listener != null) listener.onPlayCountsLoaded(playCounts);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseSongsHandler", "Play counts load failed: " + error.getMessage());
                if (listener != null) listener.onPlayCountsLoaded(new HashMap<>());
            }
        });
    }

    public void syncLocalSongs(ArrayList<Song> localSongs, OnSyncCompleteListener listener) {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, String> idMap = new HashMap<>();
                int[] remaining = {localSongs.size()};

                if (localSongs.isEmpty()) {
                    if (listener != null) listener.onSyncComplete(idMap);
                    return;
                }

                for (Song localSong : localSongs) {
                    String oldId = localSong.getId();
                    boolean existsInCloud = false;
                    String bucket = getBucket(localSong.getTitle());
                    DataSnapshot bucketSnapshot = snapshot.child(bucket);
                    
                    for (DataSnapshot cloudSnapshot : bucketSnapshot.getChildren()) {
                        Song cloudSong = cloudSnapshot.getValue(Song.class);
                        if (cloudSong != null && cloudSong.getTitle() != null && cloudSong.getArtist() != null 
                                && cloudSong.getTitle().equalsIgnoreCase(localSong.getTitle()) 
                                && cloudSong.getArtist().equalsIgnoreCase(localSong.getArtist())) {
                            existsInCloud = true;
                            String cloudId = cloudSnapshot.getKey();
                            localSong.setId(cloudId);
                            idMap.put(oldId, cloudId);
                            break;
                        }
                    }

                    if (!existsInCloud) {
                        addNewSong(localSong, (newId) -> {
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
                Log.e("FirebaseSongsHandler", "Sync failed: " + error.getMessage());
                if (listener != null) listener.onSyncComplete(new HashMap<>());
            }
        });
    }

    public interface OnSongAddedListener {
        void onAdded(String newId);
    }

    public void addNewSong(Song song, OnSongAddedListener listener) {
        if (song.getId() == null || song.getId().isEmpty() || song.getId().startsWith("local_")) {
            song.setId(databaseReference.push().getKey());
        }
        String bucket = getBucket(song.getTitle());
        databaseReference.child(bucket).child(song.getId()).setValue(song)
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "Saved to bucket " + bucket + ": " + song.getTitle());
                    if (listener != null) listener.onAdded(song.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firebase", "Failed saving: " + e.getMessage());
                    if (listener != null) listener.onAdded(song.getId());
                });
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
