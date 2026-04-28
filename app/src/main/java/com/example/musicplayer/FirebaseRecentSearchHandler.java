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
    private final String userId;

    public FirebaseRecentSearchHandler(String userId) {
        this.userId = userId;
        this.ref = FirebaseDatabase.getInstance().getReference("users").child(userId).child("recentSearches");
        initListener();
    }

    private void initListener() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Song> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Song song = ds.getValue(Song.class);
                    if (song != null) list.add(song);
                }
                MyApplication.recentSearches = list;
                // You might want to notify listeners here too if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void addRecentSearch(Song song) {
        // Limit to say top 20 recent searches
        ref.child(song.getId()).setValue(song);
    }

    public void clearRecentSearches() {
        ref.removeValue();
    }
}
