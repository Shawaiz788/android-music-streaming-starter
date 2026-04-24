package com.example.musicplayer;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity
        implements PlayerManager.OnPlayerStateChangedListener {

    BottomNavigationView bottomNav;
    NavController navController;

    CardView miniPlayer;
    TextView miniTitle, miniArtist;
    ImageButton miniBtnMaximize, miniBtnClose;
    ProgressBar miniProgress;

    Handler handler = new Handler();
    Runnable progressUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mini player setup
        miniPlayer = findViewById(R.id.mini_player);
        miniTitle = findViewById(R.id.mini_tv_title);
        miniArtist = findViewById(R.id.mini_tv_artist);
        miniBtnMaximize = findViewById(R.id.mini_btn_maximize);
        miniBtnClose = findViewById(R.id.mini_btn_close);
        miniProgress = findViewById(R.id.mini_progress);

        PlayerManager.getInstance().setListener(this);

        miniBtnClose.setOnClickListener(v -> PlayerManager.getInstance().stop());
        miniBtnMaximize.setOnClickListener(v -> reopenFullPlayer());

        // Nav setup
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
            } else if (id == R.id.topFragment) {
                navController.navigate(R.id.topFragment, null, options);
                return true;
            } else if (id == R.id.favoritesFragment) {
                navController.navigate(R.id.favoritesFragment, null, options);
                return true;
            } else if (id == R.id.searchFragment) {
                navController.navigate(R.id.searchFragment, null, options);
                return true;
            }

            return false;
        });
    }

    private void startProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                PlayerManager pm = PlayerManager.getInstance();
                if (pm.getDuration() > 0) {
                    int progress = (int) ((pm.getCurrentPosition() / (float) pm.getDuration()) * 100);
                    miniProgress.setProgress(progress);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(progressUpdater);
    }

    private void stopProgressUpdater() {
        if (progressUpdater != null) handler.removeCallbacks(progressUpdater);
    }

    private void showMiniPlayer(Song song) {
        miniTitle.setText(song.getTitle());
        miniArtist.setText(song.getArtist());
        miniPlayer.setAlpha(0f);
        miniPlayer.setVisibility(View.VISIBLE);
        miniPlayer.animate().alpha(1f).setDuration(300).start();
        startProgressUpdater();
    }

    private void hideMiniPlayer() {
        miniPlayer.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> miniPlayer.setVisibility(View.GONE))
                .start();
        stopProgressUpdater();
    }

    private void reopenFullPlayer() {
        NavHostFragment nhf = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (nhf == null) return;
        Fragment current = nhf.getChildFragmentManager().getPrimaryNavigationFragment();
        Song song = PlayerManager.getInstance().getCurrentSong();
        if (song == null) return;
        if (current instanceof FavoritesFragment) {
            ((FavoritesFragment) current).showPlayerDialog(song, true);
        }
        // Add other fragments here as you expand
    }

    @Override public void onSongChanged(Song song) { showMiniPlayer(song); }
    @Override public void onPlayStateChanged(boolean isPlaying) {}
    @Override public void onStopped() { hideMiniPlayer(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdater();
    }
}