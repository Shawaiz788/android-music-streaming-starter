package com.example.musicplayer;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.MotionEvent;

import java.io.IOException;

public class FavoritesFragment extends Fragment {

    LinearLayout LLTracks;
    MediaPlayer mediaPlayer;
    Handler handler = new Handler();
    Runnable updateSeekBar;
    float touchStartY = 0;
    float totalDeltaY = 0;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LLTracks = view.findViewById(R.id.LLTracks);
        
        LLTracks.setOnClickListener(v -> {
            MyApplication app = (MyApplication) requireActivity().getApplication();
            if (app.songs != null && !app.songs.isEmpty()) {
                showPlayerDialog(app.songs.get(1), false);
            }
        });
    }

    public void showPlayerDialog(Song song, boolean resumeOnly) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_player, null);
        
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvArtist = view.findViewById(R.id.tv_artist);
        TextView tvTotalTime = view.findViewById(R.id.tv_total_time);
        TextView tvCurrentTime = view.findViewById(R.id.tv_current_time);
        ImageView ivSong = view.findViewById(R.id.iv_song);
        ImageView ivPlayPause = view.findViewById(R.id.iv_play_pause);
        CardView btnPlayPause = view.findViewById(R.id.btn_play_pause);
        SeekBar seekBar = view.findViewById(R.id.seek_bar);

        PlayerManager playerManager = PlayerManager.getInstance();

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        
        // Load Image using Uri (works for android.resource://)
        if (song.getImageUrl() != null) {
            ivSong.setImageURI(Uri.parse(song.getImageUrl()));
        }

        if (!resumeOnly) {
            playerManager.play(requireContext(), song);
        }

        ivPlayPause.setImageResource(playerManager.isPlaying() ? 
                android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);

        // Setup SeekBar
        seekBar.setMax(playerManager.getDuration());
        tvTotalTime.setText(formatTime(playerManager.getDuration()));

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (playerManager.getMediaPlayer() != null && playerManager.isPlaying()) {
                    seekBar.setProgress(playerManager.getCurrentPosition());
                    tvCurrentTime.setText(formatTime(playerManager.getCurrentPosition()));
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

        // Play/Pause Button
        btnPlayPause.setOnClickListener(v -> {
            playerManager.togglePlayPause();
            ivPlayPause.setImageResource(playerManager.isPlaying() ? 
                    android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialog)
                .setView(view)
                .create();

        view.findViewById(R.id.btn_music_list).setOnClickListener(v1 -> {
            playerManager.stop();
            handler.removeCallbacks(updateSeekBar);
            dialog.dismiss();
        });

        view.setOnTouchListener((v, event) -> {
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
                        // Minimize — don't stop playback
                        view.animate()
                                .translationY(view.getHeight())
                                .alpha(0f)
                                .setDuration(250)
                                .withEndAction(dialog::dismiss) // mini player stays visible
                                .start();
                    } else {
                        view.animate().translationY(0f).alpha(1f).setDuration(150).start();
                    }
                    return true;
            }
            return false;
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
    }
}
