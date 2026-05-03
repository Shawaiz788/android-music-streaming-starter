package com.example.musicplayer;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FirebasePlaylistHandler {
    private final DatabaseReference ref;

    public FirebasePlaylistHandler(String userId) {
        this.ref = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users").child(userId).child("playlists");
        initListener();
    }

    private void initListener() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Playlist> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Playlist playlist = ds.getValue(Playlist.class);
                    if (playlist != null) {
                        playlist.setId(ds.getKey());
                        playlist.calculateAndSetDuration(MyApplication.songs);
                        list.add(playlist);
                    }
                }
                MyApplication.favouritePlaylists.clear();
                MyApplication.favouritePlaylists.addAll(list);
                MyApplication.notifyPlaylistsLoaded();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PlaylistHandler", "Load failed: " + error.getMessage());
            }
        });
    }

    public void addPlaylist(Playlist playlist) {
        String id = ref.push().getKey();
        if (id != null) {
            playlist.setId(id);
            playlist.calculateAndSetDuration(MyApplication.songs);
            ref.child(id).setValue(playlist);
            if (!MyApplication.favouritePlaylists.contains(playlist)) {
                MyApplication.favouritePlaylists.add(playlist);
            }
            MyApplication.notifyPlaylistsLoaded();
        }
    }

    public void deletePlaylist(String playlistId) {
        MyApplication.favouritePlaylists.removeIf(p -> p.getId() != null && p.getId().equals(playlistId));
        MyApplication.notifyPlaylistsLoaded();
        ref.child(playlistId).removeValue();
    }

    public void updatePlaylist(Playlist playlist) {
        if (playlist.getId() != null) {
            playlist.calculateAndSetDuration(MyApplication.songs);
            ref.child(playlist.getId()).setValue(playlist);
            MyApplication.notifyPlaylistsLoaded();
        }
    }
}
