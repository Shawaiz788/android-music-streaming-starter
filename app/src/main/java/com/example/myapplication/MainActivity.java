package com.example.myapplication;

import android.os.Bundle;
import android.widget.TableLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
TabLayout tablayout;
ViewPager2 viewpager;
TabLayoutMediator mediator;

ViewPagerAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        init();
    }
    public void init(){
        tablayout=findViewById(R.id.tablayout);
        viewpager=findViewById(R.id.viewpager2);
        adapter=new ViewPagerAdapter(this);
        viewpager.setAdapter(adapter);
        mediator =new TabLayoutMediator(tablayout,viewpager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int i) {
                switch (i){
                    case 0:
                       tab.setText("Home");
                       tab.setIcon(R.drawable.icon_home);

                       break;

                    case 1:
                        tab.setText("Top");
                        tab.setIcon(R.drawable.icon_music);

                        break;

                    case 2:
                        tab.setText("Favorites");
                        tab.setIcon(R.drawable.icon_favorites);

                        break;

                    case 3:
                        tab.setText("Search");
                        tab.setIcon(R.drawable.icon_search);
                        break;
                }
            }
        });
        mediator.attach();
    }
}