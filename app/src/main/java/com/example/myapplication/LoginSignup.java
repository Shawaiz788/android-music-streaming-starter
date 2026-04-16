package com.example.myapplication;

import android.app.ActionBar;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LoginSignup extends AppCompatActivity {
    TabLayout tablayout;
    ViewPager2 viewpager2;
    ViewPagerLoginAdapter adapter;
    TabLayoutMediator mediator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();

    }

    public void init(){
        tablayout=findViewById(R.id.tablayout);
        viewpager2=findViewById(R.id.viewpager2);
        adapter=new ViewPagerLoginAdapter(this);
        viewpager2.setAdapter(adapter);
        mediator=new TabLayoutMediator(tablayout, viewpager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int i) {
                if(i==0){
                    tab.setText("Sign Up");
                }else{
                    tab.setText("Log In");
                }
            }
        });
        mediator.attach();
    }
}