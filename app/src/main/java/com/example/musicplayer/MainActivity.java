package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
        ImageView ivSongMenu = view.findViewById(R.id.iv_song_menu);
        TextView tvSongNameMenu = view.findViewById(R.id.tv_song_name);
        TextView tvArtistNameMenu = view.findViewById(R.id.tv_artist_name);
        TextView tvDurationMenu = view.findViewById(R.id.tv_duration);
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

        // Initial Favorite State for Menu
        ImageView ivFavMenu = view.findViewById(R.id.iv_fav_menu);
        ivFavMenu.setImageResource(isFav ? R.drawable.icon_favorites : R.drawable.ic_fav);

        view.findViewById(R.id.fav_btn).setOnClickListener(v -> {
            boolean currentlyFav = false;
            for (Song s : MyApplication.favouriteSongs) {
                if (s.getId() != null && s.getId().equals(song.getId())) {
                    currentlyFav = true;
                    break;
                }
            }
            MyApplication.favouriteSongsHandler.toggleFavourite(song);
            boolean newFav = !currentlyFav;
            ivFav.setImageResource(newFav ? R.drawable.icon_favorites : R.drawable.ic_fav);
            ivFavMenu.setImageResource(newFav ? R.drawable.icon_favorites : R.drawable.ic_fav);
        });

        // Favorite Button in Menu (Sync)
        view.findViewById(R.id.fav_btn_menu_container).setOnClickListener(v -> {
            boolean currentlyFav = false;
            for (Song s : MyApplication.favouriteSongs) {
                if (s.getId() != null && s.getId().equals(song.getId())) {
                    currentlyFav = true;
                    break;
                }
            }
            MyApplication.favouriteSongsHandler.toggleFavourite(song);
            boolean newFav = !currentlyFav;
            ivFav.setImageResource(newFav ? R.drawable.icon_favorites : R.drawable.ic_fav);
            ivFavMenu.setImageResource(newFav ? R.drawable.icon_favorites : R.drawable.ic_fav);
        });

        // --- Download / Delete Button Logic ---
        com.google.android.material.button.MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        DBManager dbManager = new DBManager(this);
        dbManager.Open();

        Runnable updateDownloadButton = () -> {
            boolean downloaded = dbManager.isDownloaded(song.getId());
            btnDownload.setText(downloaded ? "Delete" : "Download");
            btnDownload.setIconResource(downloaded ? android.R.drawable.ic_menu_delete : android.R.drawable.stat_sys_download);
        };
        updateDownloadButton.run();

        btnDownload.setOnClickListener(v -> {
            if (dbManager.isDownloaded(song.getId())) {
                // Delete Logic
                String[] paths = dbManager.getSongPaths(song.getId());
                if (paths[0] != null) new java.io.File(paths[0]).delete();
                if (paths[1] != null) new java.io.File(paths[1]).delete();
                dbManager.deleteSong(song.getId());
                Toast.makeText(this, "Song deleted from device", Toast.LENGTH_SHORT).show();
                updateDownloadButton.run();
            } else {
                // Download Logic
                btnDownload.setEnabled(false);
                btnDownload.setText("Downloading...");
                dbManager.AddSongs(song, new DBManager.OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        runOnUiThread(() -> {
                            updateDownloadButton.run();
                            btnDownload.setEnabled(true);
                            Toast.makeText(MainActivity.this, "Song downloaded successfully", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {
                        runOnUiThread(() -> {
                            updateDownloadButton.run();
                            btnDownload.setEnabled(true);
                        });
                    }
                });            }
        });

        PlayerManager playerManager = PlayerManager.getInstance();

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        tvSongNameMenu.setText(song.getTitle());
        tvArtistNameMenu.setText(song.getArtist());

        // Check for local cover image
        Object imageSource = song.getImageUrl();
        if (dbManager.isDownloaded(song.getId())) {
            String[] paths = dbManager.getSongPaths(song.getId());
            if (paths[1] != null && new java.io.File(paths[1]).exists()) {
                imageSource = new java.io.File(paths[1]);
            }
        }

        if (imageSource != null) { //dynamic color set here
            Glide.with(this)
                    .asBitmap()
                    .load(imageSource)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            ivSong.setImageBitmap(resource);
                            ivSongMenu.setImageBitmap(resource);
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
            // Offline Playback Logic: Check if song exists locally
            String originalUrl = song.getSongUrl();
            if (dbManager.isDownloaded(song.getId())) {
                String[] paths = dbManager.getSongPaths(song.getId());
                if (paths[0] != null && new java.io.File(paths[0]).exists()) {
                    song.setSongUrl(paths[0]); // Temporarily use local path
                }
            }

            playerManager.play(this, song);
            song.setSongUrl(originalUrl); // Restore original URL for metadata consistency

            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            android.util.Log.d("ICON_DEBUG", "Setting PAUSE icon, resumeOnly=false");
            tvCurrentTime.setText("0:00");
            tvTotalTime.setText("0:00");
            tvDurationMenu.setText("0:00");
            seekBar.setMax(0);
        } else {
            seekBar.setMax(playerManager.getDuration());
            String totalTime = formatTime(playerManager.getDuration());
            tvTotalTime.setText(totalTime);
            tvDurationMenu.setText(totalTime);
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
                        String totalTime = formatTime(playerManager.getDuration());
                        tvTotalTime.setText(totalTime);
                        tvDurationMenu.setText(totalTime);
                    }
                    if (playerManager.isPlaying()) {
                        int position = playerManager.getCurrentPosition();
                        int duration = playerManager.getDuration();

                        int clampedPosition = Math.min(position, duration);
                        seekBar.setProgress(clampedPosition);
                        tvCurrentTime.setText(formatTime(clampedPosition));
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

        // --- Bottom Sheet Setup ---
        View moreMenuSheet = view.findViewById(R.id.more_menu_sheet);
        View equalizerSheet = view.findViewById(R.id.equalizer_sheet);
        View lyricsSheet = view.findViewById(R.id.lyrics_sheet);
        View playerContent = view.findViewById(R.id.player_content);

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(moreMenuSheet);
        BottomSheetBehavior<View> eqBehavior = BottomSheetBehavior.from(equalizerSheet);
        BottomSheetBehavior<View> lyricsBehavior = BottomSheetBehavior.from(lyricsSheet);

        // Calculate 40% of screen height for the intermediate stop
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int targetPeekHeight = (int) (displayMetrics.heightPixels * 0.4);

        // Configure Menu Behavior
        behavior.setFitToContents(true);
        behavior.setPeekHeight(targetPeekHeight);
        behavior.setHideable(true);
        behavior.setSkipCollapsed(false);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Configure Equalizer Behavior
        eqBehavior.setFitToContents(true);
        eqBehavior.setHideable(true);
        eqBehavior.setSkipCollapsed(true);
        eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Configure Lyrics Behavior
        lyricsBehavior.setFitToContents(true);
        lyricsBehavior.setHideable(true);
        lyricsBehavior.setSkipCollapsed(true);
        lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        BottomSheetBehavior.BottomSheetCallback commonCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    // Check if any other sheet is visible before showing player content
                    if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN &&
                        eqBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN &&
                        lyricsBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                        playerContent.animate().alpha(1f).setDuration(200).start();
                    }
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float alpha = 1.0f - (slideOffset + 1.0f) / 2.0f;
                if (alpha < 0) alpha = 0;
                if (alpha > 1) alpha = 1;
                playerContent.setAlpha(alpha);
            }
        };

        behavior.addBottomSheetCallback(commonCallback);
        eqBehavior.addBottomSheetCallback(commonCallback);
        lyricsBehavior.addBottomSheetCallback(commonCallback);

        view.findViewById(R.id.menu_more).setOnClickListener(v -> {
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            int state = behavior.getState();
            if (state == BottomSheetBehavior.STATE_HIDDEN) {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        // --- Share Functionality ---
        view.findViewById(R.id.share_btn).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareMessage = "Check out this song: " + song.getTitle() + " by " + song.getArtist() + "\n" + song.getSongUrl();
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        });

        // --- Equalizer Integration ---
        view.findViewById(R.id.equalizer).setOnClickListener(v -> {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            
            // Setup Equalizer View
            SwitchCompat eqSwitch = equalizerSheet.findViewById(R.id.equalizer_switch);
            LinearLayout container = equalizerSheet.findViewById(R.id.equalizer_container);
            container.removeAllViews(); // Clear previous views if any

            int sessionId = PlayerManager.getInstance().getAudioSessionId();
            if (sessionId != 0) {
                final Equalizer equalizer = new Equalizer(0, sessionId);
                eqSwitch.setChecked(equalizer.getEnabled());
                eqSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> equalizer.setEnabled(isChecked));

                short bands = equalizer.getNumberOfBands();
                final short minEQLevel = equalizer.getBandLevelRange()[0];
                final short maxEQLevel = equalizer.getBandLevelRange()[1];

                for (short i = 0; i < bands; i++) {
                    final short band = i;
                    TextView freqText = new TextView(this);
                    freqText.setTextColor(Color.WHITE);
                    freqText.setPadding(0, 20, 0, 10);
                    freqText.setText((equalizer.getCenterFreq(band) / 1000) + " Hz");
                    container.addView(freqText);

                    SeekBar bar = new SeekBar(this);
                    bar.setMax(maxEQLevel - minEQLevel);
                    bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);
                    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) equalizer.setBandLevel(band, (short) (progress + minEQLevel));
                        }
                        @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                    });
                    container.addView(bar);
                }
            }
            eqBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        // --- Go to Artist ---
        view.findViewById(R.id.btn_go_to_artist).setOnClickListener(v -> {
            dialog.dismiss();
            // Since we don't have an ArtistDetailsFragment yet, we can filter search or show a toast
            // If we have an Artist object, we could navigate. For now, let's toast.
            Toast.makeText(this, "Going to artist: " + song.getArtist(), Toast.LENGTH_SHORT).show();
        });

        // --- Go to Album ---
        view.findViewById(R.id.btn_go_to_album).setOnClickListener(v -> {
            Album targetAlbum = null;
            for (Album a : MyApplication.allAlbums) {
                if (a.getTitle().equalsIgnoreCase(song.getAlbum())) {
                    targetAlbum = a;
                    break;
                }
            }
            if (targetAlbum != null) {
                dialog.dismiss();
                Bundle bundle = new Bundle();
                bundle.putSerializable("album", targetAlbum);
                navController.navigate(R.id.albumDetailsFragment, bundle);
            } else {
                Toast.makeText(this, "Album details not available", Toast.LENGTH_SHORT).show();
            }
        });

        // --- Song Lyrics ---
        view.findViewById(R.id.btn_lyrics).setOnClickListener(v -> {
            String lyrics = song.getLyrics();
            if (lyrics == null || lyrics.isEmpty()) {
                lyrics = "Lyrics not available for this song.";
            }

            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_title)).setText(song.getTitle());
            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_artist)).setText(song.getArtist());
            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_content)).setText(lyrics);

            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

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
                // If any bottom sheet is showing, ignore the dialog-dismiss swipe
                if (behavior.getState() != BottomSheetBehavior.STATE_HIDDEN ||
                    eqBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN ||
                    lyricsBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                    return false;
                }

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

    private void showEqualizerDialog() {
        View eqView = LayoutInflater.from(this).inflate(R.layout.dialog_equalizer, null);
        BottomSheetDialog eqDialog = new BottomSheetDialog(this, R.style.TransparentDialog);
        eqDialog.setContentView(eqView);

        // Ensure it opens at the bottom and is swipeable
        BottomSheetBehavior<?> behavior = eqDialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setHideable(true);
        behavior.setSkipCollapsed(true);

        SwitchCompat eqSwitch = eqView.findViewById(R.id.equalizer_switch);
        LinearLayout container = eqView.findViewById(R.id.equalizer_container);

        int sessionId = PlayerManager.getInstance().getAudioSessionId();
        if (sessionId == 0) {
            Toast.makeText(this, "No active audio session", Toast.LENGTH_SHORT).show();
            return;
        }

        final Equalizer equalizer = new Equalizer(0, sessionId);
        eqSwitch.setChecked(equalizer.getEnabled());
        eqSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> equalizer.setEnabled(isChecked));

        short bands = equalizer.getNumberOfBands();
        final short minEQLevel = equalizer.getBandLevelRange()[0];
        final short maxEQLevel = equalizer.getBandLevelRange()[1];

        for (short i = 0; i < bands; i++) {
            final short band = i;

            TextView freqText = new TextView(this);
            freqText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            freqText.setGravity(Gravity.CENTER);
            freqText.setTextColor(Color.WHITE);
            freqText.setPadding(0, 20, 0, 10);
            freqText.setText((equalizer.getCenterFreq(band) / 1000) + " Hz");
            container.addView(freqText);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView minDb = new TextView(this);
            minDb.setText((minEQLevel / 100) + " dB");
            minDb.setTextColor(Color.WHITE);
            minDb.setTextSize(10);

            SeekBar bar = new SeekBar(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            bar.setLayoutParams(params);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);

            TextView maxDb = new TextView(this);
            maxDb.setText((maxEQLevel / 100) + " dB");
            maxDb.setTextColor(Color.WHITE);
            maxDb.setTextSize(10);

            row.addView(minDb);
            row.addView(bar);
            row.addView(maxDb);

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    equalizer.setBandLevel(band, (short) (progress + minEQLevel));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            container.addView(row);
        }

        eqDialog.show();
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
