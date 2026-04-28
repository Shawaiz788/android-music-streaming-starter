package com.example.musicplayer;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FirebaseFavouriteAlbumsHandler {
    private final DatabaseReference ref;

    public FirebaseFavouriteAlbumsHandler(String userId) {
        this.ref = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(userId).child("favouriteAlbums");
        initListener();
    }

    private void initListener() {
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Album> list = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Album album = ds.getValue(Album.class);
                    if (album != null) list.add(album);
                }
                MyApplication.favouriteAlbums = list;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void toggleFavourite(Album album) {
        ref.child(album.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ref.child(album.getId()).removeValue();
                } else {
                    ref.child(album.getId()).setValue(album);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
