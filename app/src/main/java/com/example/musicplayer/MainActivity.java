package com.example.musicplayer;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
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
    Runnable updateSeekBar;
    ImageView ivPlayPause;

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
        Song song = PlayerManager.getInstance().getCurrentSong();
        if (song != null) {
            showPlayerDialog(song, true);
        }
    }

    public void showPlayerDialog(Song song, boolean resumeOnly) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_player, null);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvArtist = view.findViewById(R.id.tv_artist);
        TextView tvTotalTime = view.findViewById(R.id.tv_total_time);
        TextView tvCurrentTime = view.findViewById(R.id.tv_current_time);
        ImageView ivSong = view.findViewById(R.id.iv_song);
        ivPlayPause = view.findViewById(R.id.iv_play_pause);
        CardView btnPlayPause = view.findViewById(R.id.btn_play_pause);
        SeekBar seekBar = view.findViewById(R.id.seek_bar);
        CardView btnFav = view.findViewById(R.id.fav_btn);
        ImageView ivFav = view.findViewById(R.id.iv_fav);

        // Set initial fav icon based on current favourites list
        boolean isFav = false;
        for (Song s : MyApplication.favouriteSongs) {
            if (s.getId().equals(song.getId())) {
                isFav = true;
                break;
            }
        }
        ivFav.setImageResource(isFav ? R.drawable.icon_favorites : R.drawable.ic_fav);

        btnFav.setOnClickListener(v -> {
            Log.d("FAV_DEBUG", "Fav button clicked");

            // Check current state BEFORE toggling
            boolean currentlyFav = false;
            for (Song s : MyApplication.favouriteSongs) {
                if (s.getId() != null && s.getId().equals(song.getId())) {
                    currentlyFav = true;
                    break;
                }
            }

            MyApplication.favouriteSongsHandler.toggleFavourite(song);

            // Flip icon based on what state it WAS (now it'll be the opposite)
            ivFav.setImageResource(currentlyFav ? R.drawable.ic_fav : R.drawable.icon_favorites);
        });

        PlayerManager playerManager = PlayerManager.getInstance();

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());

        if (song.getImageUrl() != null) { //dynamic color set here
            Glide.with(this)
                    .asBitmap()
                    .load(song.getImageUrl())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ivSong.setImageBitmap(resource);
                            Palette.from(resource).generate(palette -> {
                                if (palette != null) {
                                    int dominantColor = palette.getDominantColor(0xFFA67B5B);
                                    int darkMutedColor = palette.getDarkMutedColor(0xFF2E2016);

                                    GradientDrawable gd = new GradientDrawable(
                                            GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[] {dominantColor, darkMutedColor}
                                    );
                                    gd.setCornerRadius(0f);
                                    view.setBackground(gd);
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) { }
                    });
        }

        if (!resumeOnly) {
            playerManager.play(this, song);
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            android.util.Log.d("ICON_DEBUG", "Setting PAUSE icon, resumeOnly=false");
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            seekBar.setMax(0);
        } else {
            seekBar.setMax(playerManager.getDuration());
            tvTotalTime.setText(formatTime(playerManager.getDuration()));
            if (playerManager.isPlaying()) {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else if (playerManager.isCompleted()) {
                ivPlayPause.setImageResource(R.drawable.ic_replay);
            } else {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        }

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (playerManager.getMediaPlayer() != null) {
                    // Always update max/total time dynamically
                    if (seekBar.getMax() == 0 && playerManager.getDuration() > 0) {
                        seekBar.setMax(playerManager.getDuration());
                        tvTotalTime.setText(formatTime(playerManager.getDuration()));
                    }
                    if (playerManager.isPlaying()) {
                        seekBar.setProgress(playerManager.getCurrentPosition());
                        tvCurrentTime.setText(formatTime(playerManager.getCurrentPosition()));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateSeekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    playerManager.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayPause.setOnClickListener(v -> {
            playerManager.togglePlayPause();
            ivPlayPause.setImageResource(playerManager.isPlaying() ?
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.TransparentDialog)
                .setView(view)
                .create();

        view.findViewById(R.id.btn_music_list).setOnClickListener(v1 -> {
            playerManager.stop();
            handler.removeCallbacks(updateSeekBar);
            dialog.dismiss();
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            float touchStartY = 0;
            float totalDeltaY = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartY = event.getRawY();
                        totalDeltaY = 0;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getRawY() - touchStartY;
                        if (deltaY > 0) {
                            totalDeltaY = deltaY;
                            view.setTranslationY(deltaY);
                            view.setAlpha(1f - (deltaY / 800f));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (totalDeltaY < 10) {
                            v.performClick();
                        } else if (totalDeltaY > 300) {
                            view.animate()
                                    .translationY(view.getHeight())
                                    .alpha(0f)
                                    .setDuration(250)
                                    .withEndAction(dialog::dismiss)
                                    .start();
                        } else {
                            view.animate().translationY(0f).alpha(1f).setDuration(150).start();
                        }
                        return true;
                }
                return false;
            }
        });

        dialog.show();

        view.setTranslationY(2000f);
        view.animate().translationY(0f).setDuration(350)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private String formatTime(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    @Override public void onSongChanged(Song song) { showMiniPlayer(song); }
    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        if (ivPlayPause != null) {
            if (isPlaying) {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else if (PlayerManager.getInstance().isCompleted()
                    && !PlayerManager.getInstance().isPlaying()) { // add this guard
                ivPlayPause.setImageResource(R.drawable.ic_replay);
            } else {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }

        }
    }
    @Override public void onStopped() { hideMiniPlayer(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdater();
    }
}
