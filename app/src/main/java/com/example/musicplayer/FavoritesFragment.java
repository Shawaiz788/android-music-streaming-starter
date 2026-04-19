package com.example.musicplayer;

import android.app.AlertDialog;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import java.io.IOException;

public class FavoritesFragment extends Fragment {

    LinearLayout LLTracks;
    MediaPlayer mediaPlayer;
    Handler handler = new Handler();
    Runnable updateSeekBar;

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
                showPlayerDialog(app.songs.get(0));
            }
        });
    }

    private void showPlayerDialog(Song song) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_player, null);
        
        TextView tvTitle = view.findViewById(R.id.tv_title);
        TextView tvArtist = view.findViewById(R.id.tv_artist);
        TextView tvTotalTime = view.findViewById(R.id.tv_total_time);
        TextView tvCurrentTime = view.findViewById(R.id.tv_current_time);
        ImageView ivSong = view.findViewById(R.id.iv_song);
        ImageView ivPlayPause = view.findViewById(R.id.iv_play_pause);
        CardView btnPlayPause = view.findViewById(R.id.btn_play_pause);
        SeekBar seekBar = view.findViewById(R.id.seek_bar);

        tvTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        
        // Load Image using Uri (works for android.resource://)
        if (song.getImageUrl() != null) {
            ivSong.setImageURI(Uri.parse(song.getImageUrl()));
        }

        // Initialize Media Player
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(requireContext(), Uri.parse(song.getSongUrl()));
            mediaPlayer.prepare();
            mediaPlayer.start();
            ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error playing song", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Setup SeekBar
        seekBar.setMax(mediaPlayer.getDuration());
        tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));

        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                }
                handler.postDelayed(this, 1000); //update it every second
            }
        };
        handler.post(updateSeekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Play/Pause Button
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                ivPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                ivPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view)
                .create();

        view.findViewById(R.id.btn_music_list).setOnClickListener(v1 -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            handler.removeCallbacks(updateSeekBar);
            dialog.dismiss();
        });

        dialog.show();
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
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBar);
    }
}
