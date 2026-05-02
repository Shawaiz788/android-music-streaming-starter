package com.example.musicplayer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class SignInSignup extends AppCompatActivity {
    FragmentManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_signup);
        
        ThemeHelper.applyTheme(findViewById(R.id.main));
        
        init();
    }

    public void init(){
       manager=getSupportFragmentManager();
       manager.beginTransaction().replace(R.id.fragment_container,new SigninFragment()).addToBackStack(null).commit();
    }
}
