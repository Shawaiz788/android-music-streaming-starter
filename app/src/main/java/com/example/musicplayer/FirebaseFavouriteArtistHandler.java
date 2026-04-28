package com.example.musicplayer;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FirebaseFavouriteArtistHandler {
    private final DatabaseReference ref;

    public FirebaseFavouriteArtistHandler(String userId) {
        this.ref = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(userId).child("favouriteArtists");
        initListener();
    }

    private void initListener() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Artist> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Artist artist = ds.getValue(Artist.class);
                    if (artist != null) list.add(artist);
                }
                MyApplication.favouriteArtists = list;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void toggleFavourite(Artist artist) {
        ref.child(artist.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ref.child(artist.getId()).removeValue();
                } else {
                    ref.child(artist.getId()).setValue(artist);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
