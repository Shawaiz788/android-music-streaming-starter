package com.example.musicplayer;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseUserHandler {
    private final DatabaseReference userRef;

    public FirebaseUserHandler(String userId) {
        this.userRef = FirebaseDatabase.getInstance("https://musicplayer-33db9-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("users").child(userId).child("info");
        initListener();
    }

    private void initListener() {
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    MyApplication.currentUserInfo = user;
                    MyApplication.notifyUserLoaded();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void saveUser(User user) {
        userRef.setValue(user);
    }

    public void setName(String name) {
        userRef.child("name").setValue(name);
    }

    public void setEmail(String email) {
        userRef.child("email").setValue(email);
    }

    public void setProfileImageUrl(String profileImageUrl) {
        userRef.child("profileImageUrl").setValue(profileImageUrl);
    }
}
