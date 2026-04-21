package com.example.musicplayer;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class SignInSignup extends AppCompatActivity {
   FragmentManager manager;
Fragment signin_frag,signup_frag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_signup);
        init();

    }

    public void init(){
       manager=getSupportFragmentManager();
       manager.beginTransaction().replace(R.id.fragment_container,new SigninFragment()).addToBackStack(null).commit();




    }
}