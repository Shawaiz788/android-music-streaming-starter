package com.example.musicplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Splash extends AppCompatActivity {
SharedPreferences srPref;
    FirebaseAuth auth;
    FirebaseUser user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        init();
        new Handler().postDelayed(()->{
            checkLoggedIn();
        },1);
    }

    private void checkLoggedIn() {
        if(user==null){
            startActivity(new Intent(Splash.this, SignInSignup.class));
           finish();
       }else{
            startActivity(new Intent(Splash.this, MainActivity.class));
            finish();
        }
    }

    public void init(){
        srPref=getSharedPreferences("MyPref",MODE_PRIVATE);
       auth=FirebaseAuth.getInstance();
        user=auth.getCurrentUser();
    }
}