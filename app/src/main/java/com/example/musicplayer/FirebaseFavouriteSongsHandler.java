package com.example.musicplayer;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class FirebaseFavouriteSongsHandler {
    private final DatabaseReference ref;

    public FirebaseFavouriteSongsHandler(String userId) {
        this.ref = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users").child(userId).child("favouriteSongs");
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
                MyApplication.favouriteSongs = list;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void toggleFavourite(Song song) {

        ref.child(song.getId()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ref.child(song.getId()).removeValue()
                            .addOnSuccessListener(unused -> {})
                            .addOnFailureListener(e ->
                                    Log.e("FAV_E", "Remove failed: " + e.getMessage()));
                } else {
                    ref.child(song.getId()).setValue(song)
                            .addOnSuccessListener(unused ->{})
                            .addOnFailureListener(e ->
                                    Log.e("FAV_E", "Failed: " + e.getMessage()));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyApplication.getInstance(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
