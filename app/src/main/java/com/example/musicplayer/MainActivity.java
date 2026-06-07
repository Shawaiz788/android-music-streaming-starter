package com.example.musicplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.Equalizer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.ArrayList;

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
    private AlertDialog playerDialog;
    private View playerDialogView;
    private boolean wasPlayingBeforePause = false;

    private final Song[] currentSongRef = {null};
    private YouTubePlayer youTubePlayer;
    private YouTubePlayerView youTubePlayerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 102);
            }
        }

        miniPlayer = findViewById(R.id.mini_player);
        miniTitle = findViewById(R.id.mini_tv_title);
        miniArtist = findViewById(R.id.mini_tv_artist);
        miniBtnMaximize = findViewById(R.id.mini_btn_maximize);
        miniBtnClose = findViewById(R.id.mini_btn_close);
        miniProgress = findViewById(R.id.mini_progress);

        PlayerManager.getInstance().setListener(this);

        miniBtnClose.setOnClickListener(v -> PlayerManager.getInstance().stop());
        miniBtnMaximize.setOnClickListener(v -> reopenFullPlayer());

        youTubePlayerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer initializedYouTubePlayer) {
                youTubePlayer = initializedYouTubePlayer;
                PlayerManager.getInstance().setYouTubePlayer(initializedYouTubePlayer);
            }
        });

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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        showPlayerDialog(song, resumeOnly, java.util.Collections.singletonList(song), 0, null);
    }

    public void showPlayerDialog(Song song, boolean resumeOnly, java.util.List<Song> queue, int index) {
        showPlayerDialog(song, resumeOnly, queue, index, null);
    }

    public void showPlayerDialog(Song song, boolean resumeOnly, java.util.List<Song> queue, int index, String queueTitle) {
        if (playerDialog != null && playerDialog.isShowing()) {
            if (!resumeOnly) {
                PlayerManager.getInstance().play(this, queue, index, queueTitle);
            }
            currentSongRef[0] = PlayerManager.getInstance().getCurrentSong();
            updatePlayerDialogUI(currentSongRef[0]);
            return;
        }

        currentSongRef[0] = song;

        playerDialogView = LayoutInflater.from(this).inflate(R.layout.dialog_player, null);
        View view = playerDialogView;

        TextView tvTopStatus       = view.findViewById(R.id.tv_top_status_internal);
        TextView tvTitle            = view.findViewById(R.id.tv_title);
        TextView tvArtist           = view.findViewById(R.id.tv_artist);
        TextView tvTotalTime        = view.findViewById(R.id.tv_total_time);
        TextView tvCurrentTime      = view.findViewById(R.id.tv_current_time);
        ImageView ivSong            = view.findViewById(R.id.iv_song);
        ImageView ivSongMenu        = view.findViewById(R.id.iv_song_menu);
        TextView tvSongNameMenu     = view.findViewById(R.id.tv_song_name);
        TextView tvArtistNameMenu   = view.findViewById(R.id.tv_artist_name);
        TextView tvDurationMenu     = view.findViewById(R.id.tv_duration);
        ivPlayPause                 = view.findViewById(R.id.iv_play_pause);
        CardView btnPlayPause       = view.findViewById(R.id.btn_play_pause);
        SeekBar seekBar             = view.findViewById(R.id.seek_bar);
        ImageView ivFav             = view.findViewById(R.id.iv_fav);
        ImageView ivFavMenu         = view.findViewById(R.id.iv_fav_menu);

        if (queueTitle != null && !queueTitle.isEmpty()) {
            String displayTitle = queueTitle.startsWith(getString(R.string.album)) ? queueTitle : getString(R.string.playlist_format, queueTitle);
            if (PlayerManager.getInstance().isShuffleEnabled()) {
                displayTitle += getString(R.string.shuffled_suffix);
            }
            tvTopStatus.setText(getString(R.string.play_now_format, displayTitle));
            tvTopStatus.setVisibility(View.VISIBLE);
        } else {
            tvTopStatus.setVisibility(View.GONE);
        }

        Runnable refreshFavIcons = () -> {
            boolean fav = false;
            for (Song s : MyApplication.favouriteSongs) {
                if (s.getId().equals(currentSongRef[0].getId())) { fav = true; break; }
            }
            ivFav.setImageResource(fav ? R.drawable.icon_favorites : R.drawable.ic_fav);
            if (ivFavMenu != null)
                ivFavMenu.setImageResource(fav ? R.drawable.icon_favorites : R.drawable.ic_fav);
        };
        refreshFavIcons.run();

        view.findViewById(R.id.fav_btn).setOnClickListener(v -> {
            MyApplication.favouriteSongsHandler.toggleFavourite(currentSongRef[0], refreshFavIcons);
        });

        View favBtnMenu = view.findViewById(R.id.fav_btn_menu_container);
        if (favBtnMenu != null) {
            favBtnMenu.setOnClickListener(v -> {
                MyApplication.favouriteSongsHandler.toggleFavourite(currentSongRef[0], refreshFavIcons);
                refreshFavIcons.run();
            });
        }

        MaterialButton btnDownload = view.findViewById(R.id.btn_download);
        DBManager dbManager = new DBManager(this);
        dbManager.Open();

        Runnable updateDownloadButton = () -> {
            boolean downloaded = dbManager.isDownloaded(currentSongRef[0].getId());
            btnDownload.setText(downloaded ? getString(R.string.delete) : getString(R.string.download));
            btnDownload.setIconResource(downloaded
                    ? android.R.drawable.ic_menu_delete
                    : android.R.drawable.stat_sys_download);
        };
        updateDownloadButton.run();

        btnDownload.setOnClickListener(v -> {
            Song activeSong = currentSongRef[0];
            if (dbManager.isDownloaded(activeSong.getId())) {
                String[] paths = dbManager.getSongPaths(activeSong.getId());
                if (paths[0] != null) new java.io.File(paths[0]).delete();
                if (paths[1] != null) new java.io.File(paths[1]).delete();
                dbManager.deleteSong(activeSong.getId());
                Toast.makeText(this, getString(R.string.song_deleted), Toast.LENGTH_SHORT).show();
                updateDownloadButton.run();
            } else {
                btnDownload.setEnabled(false);
                btnDownload.setText(getString(R.string.downloading));
                dbManager.AddSongs(activeSong, new DBManager.OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        runOnUiThread(() -> {
                            updateDownloadButton.run();
                            btnDownload.setEnabled(true);
                            Toast.makeText(MainActivity.this, getString(R.string.song_downloaded), Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onDownloadFailed(Exception e) {
                        runOnUiThread(() -> {
                            updateDownloadButton.run();
                            btnDownload.setEnabled(true);
                        });
                    }
                });
            }
        });

        PlayerManager playerManager = PlayerManager.getInstance();

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        tvSongNameMenu.setText(song.getTitle());
        tvArtistNameMenu.setText(song.getArtist());

        Object imageSource = song.getImageUrl();
        if (dbManager.isDownloaded(song.getId())) {
            String[] paths = dbManager.getSongPaths(song.getId());
            if (paths[1] != null && new java.io.File(paths[1]).exists()) {
                imageSource = new java.io.File(paths[1]);
            }
        }

        if (imageSource != null) {
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
                                            new int[]{dominantColor, darkMutedColor});
                                    gd.setCornerRadius(0f);
                                    view.setBackground(gd);
                                }
                            });
                        }
                        @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
        }

        if (!resumeOnly) {
            String originalUrl = song.getSongUrl();
            if (dbManager.isDownloaded(song.getId())) {
                String[] paths = dbManager.getSongPaths(song.getId());
                if (paths[0] != null && new java.io.File(paths[0]).exists()) {
                    song.setSongUrl(paths[0]);
                }
            }
            playerManager.play(this, queue, index, queueTitle);
            song.setSongUrl(originalUrl);

            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            tvCurrentTime.setText(getString(R.string.time_zero));
            tvTotalTime.setText(getString(R.string.time_zero));
            tvDurationMenu.setText(getString(R.string.time_zero));
            seekBar.setMax(0);
        } else {
            tvTitle.setText(song.getTitle());
            tvArtist.setText(song.getArtist());
            tvSongNameMenu.setText(song.getTitle());
            tvArtistNameMenu.setText(song.getArtist());

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
                if (playerManager.getCurrentSong() != null) {
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
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) playerManager.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnPlayPause.setOnClickListener(v -> {
            playerManager.togglePlayPause();
            ivPlayPause.setImageResource(playerManager.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
        });

        ImageView btnPrev = view.findViewById(R.id.btn_prev);
        ImageView btnNext = view.findViewById(R.id.btn_next);
        ImageView btnRepeat = view.findViewById(R.id.btn_repeat);
        ImageView btnShuffle = view.findViewById(R.id.btn_shuffle);

        Runnable updateRepeatButton = () -> {
            boolean repeat = playerManager.isRepeatEnabled();
            btnRepeat.setAlpha(repeat ? 1.0f : 0.4f);
        };
        updateRepeatButton.run();

        Runnable updateShuffleButton = () -> {
            boolean shuffle = playerManager.isShuffleEnabled();
            btnShuffle.setAlpha(shuffle ? 1.0f : 0.4f);
        };
        updateShuffleButton.run();

        Runnable updateNavigationButtons = () -> {
            boolean hasPrev = playerManager.hasPrevious();
            boolean hasNext = playerManager.hasNext();
            btnPrev.setEnabled(hasPrev);
            btnPrev.setAlpha(hasPrev ? 1.0f : 0.3f);
            btnNext.setEnabled(hasNext);
            btnNext.setAlpha(hasNext ? 1.0f : 0.3f);
        };

        btnRepeat.setOnClickListener(v -> {
            playerManager.setRepeatEnabled(!playerManager.isRepeatEnabled());
            updateRepeatButton.run();
        });

        btnShuffle.setOnClickListener(v -> {
            playerManager.setShuffleEnabled(!playerManager.isShuffleEnabled());
            updateShuffleButton.run();
            updateNavigationButtons.run();
            updatePlayerDialogUI(currentSongRef[0]);
        });

        updateNavigationButtons.run();

        playerDialog = new AlertDialog.Builder(this, R.style.TransparentDialog)
                .setView(view)
                .create();
        AlertDialog dialog = playerDialog;

        btnPrev.setOnClickListener(v -> {
            playerManager.playPrevious(this);
            updateNavigationButtons.run();
            currentSongRef[0] = playerManager.getCurrentSong();
            updatePlayerDialogUI(currentSongRef[0]);
            updateDownloadButton.run();
            refreshFavIcons.run();
        });

        btnNext.setOnClickListener(v -> {
            playerManager.playNext(this);
            updateNavigationButtons.run();
            currentSongRef[0] = playerManager.getCurrentSong();
            updatePlayerDialogUI(currentSongRef[0]);
            updateDownloadButton.run();
            refreshFavIcons.run();
        });

        View moreMenuSheet  = view.findViewById(R.id.more_menu_sheet);
        View equalizerSheet = view.findViewById(R.id.equalizer_sheet);
        View lyricsSheet    = view.findViewById(R.id.lyrics_sheet);
        View playlistSheet  = view.findViewById(R.id.playlist_sheet);
        View queueSheet     = view.findViewById(R.id.queue_sheet);
        View playerContent  = view.findViewById(R.id.player_content);

        BottomSheetBehavior<View> behavior        = BottomSheetBehavior.from(moreMenuSheet);
        BottomSheetBehavior<View> eqBehavior      = BottomSheetBehavior.from(equalizerSheet);
        BottomSheetBehavior<View> lyricsBehavior  = BottomSheetBehavior.from(lyricsSheet);
        BottomSheetBehavior<View> playlistBehavior = BottomSheetBehavior.from(playlistSheet);
        BottomSheetBehavior<View> queueBehavior    = BottomSheetBehavior.from(queueSheet);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int targetPeekHeight = (int) (displayMetrics.heightPixels * 0.4);

        behavior.setFitToContents(true);
        behavior.setPeekHeight(targetPeekHeight);
        behavior.setHideable(true);
        behavior.setSkipCollapsed(false);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        eqBehavior.setFitToContents(true);
        eqBehavior.setHideable(true);
        eqBehavior.setSkipCollapsed(true);
        eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        lyricsBehavior.setFitToContents(true);
        lyricsBehavior.setHideable(true);
        lyricsBehavior.setSkipCollapsed(true);
        lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        playlistBehavior.setFitToContents(true);
        playlistBehavior.setHideable(true);
        playlistBehavior.setSkipCollapsed(true);
        playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        queueBehavior.setFitToContents(true);
        queueBehavior.setHideable(true);
        queueBehavior.setSkipCollapsed(true);
        queueBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        BottomSheetBehavior.BottomSheetCallback commonCallback = new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (behavior.getState()        == BottomSheetBehavior.STATE_HIDDEN &&
                            eqBehavior.getState()      == BottomSheetBehavior.STATE_HIDDEN &&
                            lyricsBehavior.getState()  == BottomSheetBehavior.STATE_HIDDEN &&
                            playlistBehavior.getState()== BottomSheetBehavior.STATE_HIDDEN &&
                            queueBehavior.getState()   == BottomSheetBehavior.STATE_HIDDEN) {
                        playerContent.animate().alpha(1f).setDuration(200).start();
                    }
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                float alpha = 1.0f - (slideOffset + 1.0f) / 2.0f;
                alpha = Math.max(0, Math.min(1, alpha));
                playerContent.setAlpha(alpha);
            }
        };

        behavior.addBottomSheetCallback(commonCallback);
        eqBehavior.addBottomSheetCallback(commonCallback);
        lyricsBehavior.addBottomSheetCallback(commonCallback);
        playlistBehavior.addBottomSheetCallback(commonCallback);
        queueBehavior.addBottomSheetCallback(commonCallback);

        view.findViewById(R.id.btn_music_list).setOnClickListener(v -> {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            RecyclerView rvQueue = queueSheet.findViewById(R.id.rv_queue_list);
            rvQueue.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            QueueAdapter queueAdapter = new QueueAdapter(this, playerManager.getCurrentQueue(), (selectedSong, position) -> {
                playerManager.setCurrentSongIndex(position);
                playerManager.play(this, playerManager.getCurrentQueue(), position, playerManager.getQueueTitle());
                currentSongRef[0] = selectedSong;
                updatePlayerDialogUI(selectedSong);
                queueBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                updateNavigationButtons.run();
                updateDownloadButton.run();
                refreshFavIcons.run();
            });
            rvQueue.setAdapter(queueAdapter);
            queueBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        view.findViewById(R.id.menu_more).setOnClickListener(v -> {
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            int state = behavior.getState();
            if (state == BottomSheetBehavior.STATE_HIDDEN) {
                behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        view.findViewById(R.id.btn_add_to_playlist).setOnClickListener(v -> {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            showAddToPlaylistDialog(currentSongRef[0], playlistBehavior, playlistSheet);
        });

        view.findViewById(R.id.share_btn).setOnClickListener(v -> {
            wasPlayingBeforePause = PlayerManager.getInstance().isPlaying();
            if (wasPlayingBeforePause) {
                PlayerManager.getInstance().pause();
            }

            Song activeSong = currentSongRef[0];
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareMessage = getString(R.string.share_message_format,
                    activeSong.getTitle(), activeSong.getArtist(), activeSong.getSongUrl());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        });

        view.findViewById(R.id.equalizer).setOnClickListener(v -> {
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            SwitchCompat eqSwitch = equalizerSheet.findViewById(R.id.equalizer_switch);
            LinearLayout container = equalizerSheet.findViewById(R.id.equalizer_container);
            container.removeAllViews();

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
                    freqText.setText(getString(R.string.hz_format, (equalizer.getCenterFreq(band) / 1000)));
                    container.addView(freqText);

                    SeekBar bar = new SeekBar(this);
                    bar.setMax(maxEQLevel - minEQLevel);
                    bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);
                    bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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



        view.findViewById(R.id.btn_go_to_album).setOnClickListener(v -> {
            Album targetAlbum = null;
            for (Album a : MyApplication.allAlbums) {
                if (a.getTitle().equalsIgnoreCase(currentSongRef[0].getAlbum())) {
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
                Toast.makeText(this, getString(R.string.album_not_available), Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_lyrics).setOnClickListener(v -> {
            Song activeSong = currentSongRef[0];
            String lyrics = activeSong.getLyrics();
            if (lyrics == null || lyrics.isEmpty()) {
                lyrics = getString(R.string.lyrics_not_available);
            }

            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_title)).setText(activeSong.getTitle());
            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_artist)).setText(activeSong.getArtist());
            ((TextView) lyricsSheet.findViewById(R.id.tv_lyrics_content)).setText(lyrics);

            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            eqBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            lyricsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        view.setOnTouchListener(new View.OnTouchListener() {
            float touchStartY = 0;
            float totalDeltaY = 0;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (behavior.getState()        != BottomSheetBehavior.STATE_HIDDEN ||
                        eqBehavior.getState()      != BottomSheetBehavior.STATE_HIDDEN ||
                        lyricsBehavior.getState()  != BottomSheetBehavior.STATE_HIDDEN ||
                        playlistBehavior.getState()!= BottomSheetBehavior.STATE_HIDDEN ||
                        queueBehavior.getState()   != BottomSheetBehavior.STATE_HIDDEN) {
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
                        if (totalDeltaY < 20) {
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

        dialog.getWindow().setNavigationBarColor(Color.TRANSPARENT);
        dialog.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        dialog.show();

        view.setTranslationY(2000f);
        view.animate().translationY(0f).setDuration(350)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    private void updatePlayerDialogUI(Song song) {
        if (playerDialogView == null || song == null) return;

        currentSongRef[0] = song;

        TextView tvTopStatus      = playerDialogView.findViewById(R.id.tv_top_status_internal);
        TextView tvTitle          = playerDialogView.findViewById(R.id.tv_title);
        TextView tvArtist         = playerDialogView.findViewById(R.id.tv_artist);
        TextView tvTotalTime      = playerDialogView.findViewById(R.id.tv_total_time);
        TextView tvCurrentTime    = playerDialogView.findViewById(R.id.tv_current_time);
        ImageView ivSong          = playerDialogView.findViewById(R.id.iv_song);
        ImageView ivSongMenu      = playerDialogView.findViewById(R.id.iv_song_menu);
        TextView tvSongNameMenu   = playerDialogView.findViewById(R.id.tv_song_name);
        TextView tvArtistNameMenu = playerDialogView.findViewById(R.id.tv_artist_name);
        TextView tvDurationMenu   = playerDialogView.findViewById(R.id.tv_duration);
        SeekBar seekBar           = playerDialogView.findViewById(R.id.seek_bar);
        ImageView ivFav           = playerDialogView.findViewById(R.id.iv_fav);
        ImageView ivFavMenu       = playerDialogView.findViewById(R.id.iv_fav_menu);

        PlayerManager playerManager = PlayerManager.getInstance();
        String queueTitle = playerManager.getQueueTitle();
        if (queueTitle != null && !queueTitle.isEmpty()) {
            String displayTitle = queueTitle.startsWith(getString(R.string.album)) ? queueTitle : getString(R.string.playlist_format, queueTitle);
            if (playerManager.isShuffleEnabled()) {
                displayTitle += getString(R.string.shuffled_suffix);
            }
            tvTopStatus.setText(getString(R.string.play_now_format, displayTitle));
            tvTopStatus.setVisibility(View.VISIBLE);
        } else {
            tvTopStatus.setVisibility(View.GONE);
        }

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        tvSongNameMenu.setText(song.getTitle());
        tvArtistNameMenu.setText(song.getArtist());

        boolean isFav = false;
        for (Song s : MyApplication.favouriteSongs) {
            if (s.getId().equals(song.getId())) { isFav = true; break; }
        }
        ivFav.setImageResource(isFav ? R.drawable.icon_favorites : R.drawable.ic_fav);
        if (ivFavMenu != null)
            ivFavMenu.setImageResource(isFav ? R.drawable.icon_favorites : R.drawable.ic_fav);

        if (!playerManager.isPrepared()) {
            tvCurrentTime.setText(getString(R.string.time_zero));
            tvTotalTime.setText(getString(R.string.time_zero));
            tvDurationMenu.setText(getString(R.string.time_zero));
            seekBar.setMax(0);
            seekBar.setProgress(0);
        } else {
            seekBar.setMax(playerManager.getDuration());
            String totalTime = formatTime(playerManager.getDuration());
            tvTotalTime.setText(totalTime);
            tvDurationMenu.setText(totalTime);
            seekBar.setProgress(playerManager.getCurrentPosition());
            tvCurrentTime.setText(formatTime(playerManager.getCurrentPosition()));
        }

        ImageView btnPrev = playerDialogView.findViewById(R.id.btn_prev);
        ImageView btnNext = playerDialogView.findViewById(R.id.btn_next);
        ImageView btnRepeat = playerDialogView.findViewById(R.id.btn_repeat);
        ImageView btnShuffle = playerDialogView.findViewById(R.id.btn_shuffle);
        if (btnPrev != null && btnNext != null) {
            boolean hasPrev = playerManager.hasPrevious();
            boolean hasNext = playerManager.hasNext();
            btnPrev.setEnabled(hasPrev);
            btnPrev.setAlpha(hasPrev ? 1.0f : 0.3f);
            btnNext.setEnabled(hasNext);
            btnNext.setAlpha(hasNext ? 1.0f : 0.3f);
        }
        if (btnRepeat != null) {
            btnRepeat.setAlpha(playerManager.isRepeatEnabled() ? 1.0f : 0.4f);
        }
        if (btnShuffle != null) {
            btnShuffle.setAlpha(playerManager.isShuffleEnabled() ? 1.0f : 0.4f);
        }

        DBManager dbManager = new DBManager(this);
        dbManager.Open();
        Object imageSource = song.getImageUrl();
        if (dbManager.isDownloaded(song.getId())) {
            String[] paths = dbManager.getSongPaths(song.getId());
            if (paths[1] != null && new java.io.File(paths[1]).exists()) {
                imageSource = new java.io.File(paths[1]);
            }
        }
        dbManager.Close();

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
                                        new int[]{dominantColor, darkMutedColor});
                                gd.setCornerRadius(0f);
                                playerDialogView.setBackground(gd);
                            }
                        });
                    }
                    @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
                });

        if (ivPlayPause != null) {
            ivPlayPause.setImageResource(playerManager.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
        }

        RecyclerView rvQueue = playerDialogView.findViewById(R.id.rv_queue_list);
        if (rvQueue != null && rvQueue.getAdapter() != null) {
            rvQueue.getAdapter().notifyDataSetChanged();
        }
    }

    private void showAddToPlaylistDialog(Song song, BottomSheetBehavior<View> playlistBehavior, View playlistSheet) {
        RecyclerView rvPlaylists = playlistSheet.findViewById(R.id.rv_existing_playlists);
        MaterialButton btnCreate = playlistSheet.findViewById(R.id.btn_create_new);

        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        PlaylistSmallAdapter adapter = new PlaylistSmallAdapter(this, MyApplication.favouritePlaylists, playlist -> {
            if (!playlist.getSongIds().contains(song.getId())) {
                playlist.getSongIds().add(song.getId());
                playlist.setTrackCount(playlist.getSongIds().size());
                MyApplication.playlistHandler.updatePlaylist(playlist);
                Toast.makeText(this, getString(R.string.added_to_playlist, playlist.getTitle()), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.song_already_in_playlist), Toast.LENGTH_SHORT).show();
            }
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        });
        rvPlaylists.setAdapter(adapter);

        btnCreate.setOnClickListener(v -> {
            playlistBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            showCreatePlaylistDialog(song);
        });

        playlistBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showCreatePlaylistDialog(Song song) {
        BottomSheetDialog createDialog = new BottomSheetDialog(this, R.style.TransparentDialog);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_playlist, null);
        createDialog.setContentView(dialogView);

        android.widget.FrameLayout bottomSheet = createDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.getLayoutParams().height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            createDialog.getWindow().setNavigationBarColor(getResources().getColor(R.color.charcoal));
        }

        EditText input       = dialogView.findViewById(R.id.et_playlist_name);
        MaterialButton btnCreate = dialogView.findViewById(R.id.btn_create);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnCreate.setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                Playlist newPlaylist = new Playlist(null, name, 1, "0 min");
                newPlaylist.getSongIds().add(song.getId());
                MyApplication.playlistHandler.addPlaylist(newPlaylist);
                Toast.makeText(this, getString(R.string.playlist_created), Toast.LENGTH_SHORT).show();
                createDialog.dismiss();
            } else {
                input.setError(getString(R.string.playlist_name_empty));
            }
        });

        btnCancel.setOnClickListener(v -> createDialog.dismiss());
        createDialog.show();
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

        BottomSheetBehavior<?> behavior = eqDialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        behavior.setHideable(true);
        behavior.setSkipCollapsed(true);

        SwitchCompat eqSwitch   = eqView.findViewById(R.id.equalizer_switch);
        LinearLayout container  = eqView.findViewById(R.id.equalizer_container);

        int sessionId = PlayerManager.getInstance().getAudioSessionId();
        if (sessionId == 0) {
            Toast.makeText(this, getString(R.string.no_audio_session), Toast.LENGTH_SHORT).show();
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
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            freqText.setGravity(Gravity.CENTER);
            freqText.setTextColor(Color.WHITE);
            freqText.setPadding(0, 20, 0, 10);
            freqText.setText(getString(R.string.hz_format, (equalizer.getCenterFreq(band) / 1000)));
            container.addView(freqText);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            TextView minDb = new TextView(this);
            minDb.setText(getString(R.string.db_format, (minEQLevel / 100)));
            minDb.setTextColor(Color.WHITE);
            minDb.setTextSize(10);

            SeekBar bar = new SeekBar(this);
            bar.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(equalizer.getBandLevel(band) - minEQLevel);

            TextView maxDb = new TextView(this);
            maxDb.setText(getString(R.string.db_format, (maxEQLevel / 100)));
            maxDb.setTextColor(Color.WHITE);
            maxDb.setTextSize(10);

            row.addView(minDb);
            row.addView(bar);
            row.addView(maxDb);

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    equalizer.setBandLevel(band, (short) (progress + minEQLevel));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            container.addView(row);
        }

        eqDialog.show();
    }

    @Override
    public void onSongChanged(Song song) {
        showMiniPlayer(song);
        if (playerDialog != null && playerDialog.isShowing()) {
            currentSongRef[0] = song;
            updatePlayerDialogUI(song);
        }
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        if (ivPlayPause != null) {
            if (isPlaying) {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else if (PlayerManager.getInstance().isCompleted()
                    && !PlayerManager.getInstance().isPlaying()) {
                ivPlayPause.setImageResource(R.drawable.ic_replay);
            } else {
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    @Override public void onStopped() { hideMiniPlayer();
        }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdater();
    }
}
