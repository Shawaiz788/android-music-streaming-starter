package com.example.musicplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;
import java.io.IOException;

public class PlayerManager {

    private static PlayerManager instance;
    private MediaPlayer mediaPlayer;
    private Song currentSong;
    private OnPlayerStateChangedListener listener;
    private boolean completed = false;
    private boolean prepared = false;
    private boolean playWhenReady = true;

    public interface OnPlayerStateChangedListener {
        void onSongChanged(Song song);
        void onPlayStateChanged(boolean isPlaying);
        void onStopped();
    }

    private PlayerManager() {}

    public static PlayerManager getInstance() {
        if (instance == null) instance = new PlayerManager();
        return instance;
    }

    public void setListener(OnPlayerStateChangedListener listener) {
        this.listener = listener;
    }

    public void play(Context context, Song song) {
        completed = false;
        prepared = false;
        playWhenReady = true;

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.release();
            mediaPlayer = null;
        }

        currentSong = song;
        mediaPlayer = new MediaPlayer();

        try {
            String songUrl = song.getSongUrl();
            if (songUrl.startsWith("/")) {
                // It's a local file path
                mediaPlayer.setDataSource(songUrl);
            } else {
                // It's an internet URL
                mediaPlayer.setDataSource(context, Uri.parse(songUrl));
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                prepared = true;
                if (playWhenReady) {
                    mp.start();
                }
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onSongChanged(song);
                        listener.onPlayStateChanged(isPlaying());
                    });
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                completed = true;
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            listener.onPlayStateChanged(false)
                    );
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                completed = false;
                prepared = false;
                Toast.makeText(context, "Playback error: " + what + ", " + extra, Toast.LENGTH_LONG).show();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (!prepared) {
            playWhenReady = !playWhenReady;
            if (listener != null) listener.onPlayStateChanged(playWhenReady);
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
            completed = false;
        }
        if (listener != null) listener.onPlayStateChanged(isPlaying());
    }

    public void stop() {
        completed = false;
        prepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentSong = null;
        if (listener != null) listener.onStopped();
    }

    public boolean isPlaying() {
        return mediaPlayer != null && prepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null && prepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null && prepared) ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (mediaPlayer != null && prepared) mediaPlayer.seekTo(ms);
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getAudioSessionId() {
        if (mediaPlayer != null) {
            return mediaPlayer.getAudioSessionId();
        }
        return 0;
    }

    public boolean isCompleted() {
        return completed;
    }

    public boolean isPrepared() {
        return prepared;
    }

    public boolean isPlayWhenReady() {
        return playWhenReady;
    }
}
