package com.example.musicplayer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNav;
    NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        bottomNav.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == id) {
                return true;
            }

            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                    .build();

            if (id == R.id.homeFragment) {
                navController.navigate(R.id.homeFragment, null, options);
                return true;
            }
            else if (id == R.id.topFragment) {
                navController.navigate(R.id.topFragment, null, options);
                return true;
            }
            else if (id == R.id.favoritesFragment) {
                navController.navigate(R.id.favoritesFragment, null, options);
                return true;
            }
            else if (id == R.id.searchFragment) {
                navController.navigate(R.id.searchFragment, null, options);
                return true;
            }

            return false;
        });
    }
}