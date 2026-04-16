package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Splash extends AppCompatActivity {
SharedPreferences srPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
        new Handler().postDelayed(()->{
            checkLoggedIn();
        },2000);
    }

    private void checkLoggedIn() {
        if(srPref.getBoolean("is_loggedin",false)==false){
            startActivity(new Intent(Splash.this, LoginSignup.class));
            finish();
        }else{
            startActivity(new Intent(Splash.this, MainActivity.class));
            finish();
        }
    }

    public void init(){
        srPref=getSharedPreferences("MyPref",MODE_PRIVATE);

    }
}