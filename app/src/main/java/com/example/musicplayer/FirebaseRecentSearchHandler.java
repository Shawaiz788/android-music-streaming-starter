package com.example.musicplayer;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FirebaseRecentSearchHandler {
    private final DatabaseReference ref;

    public FirebaseRecentSearchHandler(String userId) {
        this.ref = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users").child(userId).child("recentSearches");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Song> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Song song = ds.getValue(Song.class);
                    if (song != null) {
                        list.add(song);
                    }
                }
                MyApplication.recentSearches.clear();
                MyApplication.recentSearches.addAll(list);
                
                SearchFragment.refreshRecentSearches();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void addRecentSearch(Song song) {
        ref.child(song.getId()).setValue(song);
    }

    public void removeRecentSearch(String songId) {
        ref.child(songId).removeValue();
    }

    public void clearRecentSearches() {
        ref.removeValue();
    }

    public DatabaseReference getRef() {
        return ref;
    }
}
