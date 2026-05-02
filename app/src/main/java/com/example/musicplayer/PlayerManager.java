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
    private java.util.List<Song> currentQueue = new java.util.ArrayList<>();
    private int currentIndex = -1;
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
        play(context, java.util.Collections.singletonList(song), 0);
    }

    public void play(Context context, java.util.List<Song> queue, int index) {
        if (queue == null || queue.isEmpty() || index < 0 || index >= queue.size()) {
            return;
        }

        this.currentQueue = new java.util.ArrayList<>(queue);
        this.currentIndex = index;
        Song song = currentQueue.get(currentIndex);

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

        // Increment Global Play Count on Firebase
        if (MyApplication.songsHandler != null) {
            MyApplication.songsHandler.incrementPlayCount(song.getId());
        }

        try {
            DBManager dbManager = new DBManager(context);
            dbManager.Open();
            String songUrl = song.getSongUrl();
            if (dbManager.isDownloaded(song.getId())) {
                String[] paths = dbManager.getSongPaths(song.getId());
                if (paths[0] != null && new java.io.File(paths[0]).exists()) {
                    songUrl = paths[0];
                }
            }
            dbManager.Close();

            if (songUrl.startsWith("/")) {
                mediaPlayer.setDataSource(songUrl);
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(songUrl));
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                prepared = true;
                if (playWhenReady) {
                    mp.start();
                }
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        listener.onSongChanged(currentSong);
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
                // Auto play next
                if (hasNext()) {
                    playNext(context);
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

    public void playNext(Context context) {
        if (hasNext()) {
            play(context, currentQueue, currentIndex + 1);
        }
    }

    public void playPrevious(Context context) {
        if (hasPrevious()) {
            play(context, currentQueue, currentIndex - 1);
        }
    }

    public boolean hasNext() {
        return currentIndex < currentQueue.size() - 1;
    }

    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    public java.util.List<Song> getCurrentQueue() {
        return currentQueue;
    }

    public int getCurrentIndex() {
        return currentIndex;
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

    public void pause() {
        if (mediaPlayer != null && prepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) listener.onPlayStateChanged(false);
        }
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
