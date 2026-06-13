package com.example.musicplayer;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;

import java.io.IOException;

public class PlayerManager {

    private static final String TAG = "PlayerManager";
    private static PlayerManager instance;
    private MediaPlayer mediaPlayer;
    private YouTubePlayer youTubePlayer;
    private Song currentSong;
    private String queueTitle;
    private java.util.List<Song> currentQueue = new java.util.ArrayList<>();
    private int currentIndex = -1;
    private OnPlayerStateChangedListener listener;
    private boolean completed = false;
    private boolean prepared = false;
    private boolean playWhenReady = true;
    private boolean repeatEnabled = false;
    private boolean shuffleEnabled = false;
    private java.util.List<Song> originalQueue = new java.util.ArrayList<>();
    private float youtubeCurrentPosition = 0;
    private float youtubeDuration = 0;

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

    public void setYouTubePlayer(YouTubePlayer youTubePlayer) {
        this.youTubePlayer = youTubePlayer;
        if (this.youTubePlayer != null) {
            this.youTubePlayer.addListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onStateChange(YouTubePlayer youTubePlayer, PlayerConstants.PlayerState state) {
                    if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
                        if (state == PlayerConstants.PlayerState.ENDED) {
                            completed = true;
                            if (listener != null) listener.onPlayStateChanged(false);
                            notifyService();
                            if (repeatEnabled) {
                                play(MyApplication.getInstance(), currentQueue, currentIndex);
                            } else if (hasNext()) {
                                playNext(MyApplication.getInstance());
                            }
                        } else if (state == PlayerConstants.PlayerState.PLAYING) {
                            prepared = true;
                            completed = false;
                            if (listener != null) listener.onPlayStateChanged(true);
                            notifyService();
                        } else if (state == PlayerConstants.PlayerState.PAUSED) {
                            if (listener != null) listener.onPlayStateChanged(false);
                            notifyService();
                        }
                    }
                }

                @Override
                public void onVideoDuration(YouTubePlayer youTubePlayer, float duration) {
                    if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
                        youtubeDuration = duration;
                    }
                }

                @Override
                public void onCurrentSecond(YouTubePlayer youTubePlayer, float second) {
                    if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
                        youtubeCurrentPosition = second;
                    }
                }

                @Override
                public void onError(YouTubePlayer youTubePlayer, PlayerConstants.PlayerError error) {
                    Log.e(TAG, "YouTube Player Error: " + error.name());
                    String msg = "Playback error: " + error.name();
                    if (error == PlayerConstants.PlayerError.VIDEO_NOT_FOUND) msg = "Video not found";
                    else if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) msg = "Video restricted by YouTube";

                    final String finalMsg = msg;
                    if (MyApplication.getInstance() != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                Toast.makeText(MyApplication.getInstance(), finalMsg, Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            });
        }
    }

    public void play(Context context, Song song) {
        play(context, java.util.Collections.singletonList(song), 0, null);
    }

    public void play(Context context, java.util.List<Song> queue, int index) {
        play(context, queue, index, null);
    }

    public void play(Context context, java.util.List<Song> queue, int index, String queueTitle) {
        if (queue == null || queue.isEmpty() || index < 0 || index >= queue.size()) {
            return;
        }

        boolean isNewRequest = (queue != this.currentQueue);

        if (isNewRequest) {
            this.originalQueue = new java.util.ArrayList<>(queue);
            if (shuffleEnabled) {
                this.currentQueue = new java.util.ArrayList<>(queue);
                Song selectedSong = this.currentQueue.remove(index);
                java.util.Collections.shuffle(this.currentQueue);
                this.currentQueue.add(0, selectedSong);
                this.currentIndex = 0;
            } else {
                this.currentQueue = new java.util.ArrayList<>(queue);
                this.currentIndex = index;
            }
        } else {
            this.currentIndex = index;
        }

        this.queueTitle = queueTitle;
        Song song = currentQueue.get(currentIndex);

        completed = false;
        prepared = false;
        playWhenReady = true;
        youtubeCurrentPosition = 0;
        youtubeDuration = 0;

        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        currentSong = song;

        if (song.getId() != null && song.getId().startsWith("youtube_")) {
            if (youTubePlayer != null) {
                String videoId = song.getId().replace("youtube_", "").trim();
                Log.d(TAG, "Playing YouTube Video ID: [" + videoId + "]");

                prepared = false;
                youTubePlayer.loadVideo(videoId, 0);
                if (listener != null) {
                    listener.onSongChanged(currentSong);
                }
                notifyService();
            } else {
                Toast.makeText(context, "YouTube Player not ready", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        DBManager dbManager = new DBManager(context);
        dbManager.Open();
        boolean isDownloaded = dbManager.isDownloaded(song.getId());
        String songUrl = song.getSongUrl();
        if (isDownloaded) {
            String[] paths = dbManager.getSongPaths(song.getId());
            if (paths[0] != null && new java.io.File(paths[0]).exists()) {
                songUrl = paths[0];
            }
        }
        dbManager.Close();

        if (songUrl == null || songUrl.isEmpty()) {
            Toast.makeText(context, "Invalid song URL", Toast.LENGTH_SHORT).show();
            return;
        }
        startMediaPlayer(context, song, songUrl);
    }

    private void startMediaPlayer(Context context, Song song, String url) {
        if (url == null || url.isEmpty()) {
            Log.e(TAG, "Attempted to start player with empty URL");
            return;
        }

        if (youTubePlayer != null) {
            youTubePlayer.pause();
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK);

        if (MyApplication.songsHandler != null) {
            MyApplication.songsHandler.incrementPlayCount(song);
        }

        notifyService();

        try {
            if (url.startsWith("/")) {
                mediaPlayer.setDataSource(url);
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(url));
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
                notifyService();
                preResolveNext();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                completed = true;
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            listener.onPlayStateChanged(false)
                    );
                }
                notifyService();
                
                if (repeatEnabled) {
                    play(context, currentQueue, currentIndex);
                } else if (hasNext()) {
                    playNext(context);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer Error: " + what + ", " + extra);
                completed = false;
                prepared = false;
                Toast.makeText(context, "Playback error (" + what + ")", Toast.LENGTH_SHORT).show();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "IOException in startMediaPlayer", e);
            Toast.makeText(context, "Failed to load song", Toast.LENGTH_SHORT).show();
        }
    }

    public void playNext(Context context) {
        if (hasNext()) {
            play(context, currentQueue, currentIndex + 1, queueTitle);
        }
    }

    public void playPrevious(Context context) {
        if (hasPrevious()) {
            play(context, currentQueue, currentIndex - 1, queueTitle);
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
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            if (youTubePlayer != null) {
                if (prepared) {
                    if (playWhenReady) {
                        youTubePlayer.pause();
                        playWhenReady = false;
                    } else {
                        youTubePlayer.play();
                        playWhenReady = true;
                    }
                } else {
                    playWhenReady = !playWhenReady;
                }
                if (listener != null) listener.onPlayStateChanged(playWhenReady);
                notifyService();
            }
            return;
        }
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
        notifyService();
    }

    public void resume() {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            if (youTubePlayer != null) {
                youTubePlayer.play();
                playWhenReady = true;
                if (listener != null) listener.onPlayStateChanged(true);
                notifyService();
            }
            return;
        }
        if (mediaPlayer != null && prepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            completed = false;
            if (listener != null) listener.onPlayStateChanged(true);
            notifyService();
        }
    }

    public void pause() {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            if (youTubePlayer != null) {
                youTubePlayer.pause();
                playWhenReady = false;
                if (listener != null) listener.onPlayStateChanged(false);
                notifyService();
            }
            return;
        }
        if (mediaPlayer != null && prepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (listener != null) listener.onPlayStateChanged(false);
            notifyService();
        }
    }

    public void stop() {
        completed = false;
        prepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnErrorListener(null);
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (youTubePlayer != null) {
            youTubePlayer.pause();
        }
        currentSong = null;
        if (listener != null) listener.onStopped();
        
        Context context = MyApplication.getInstance();
        if (context != null) {
            Intent serviceIntent = new Intent(context, MusicService.class);
            context.stopService(serviceIntent);
        }
    }

    private void notifyService() {
        Context context = MyApplication.getInstance();
        if (context == null) return;
        Intent serviceIntent = new Intent(context, MusicService.class);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start MusicService", e);
        }
    }

    public boolean isPlaying() {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            return playWhenReady; // Approximation for YouTube
        }
        return mediaPlayer != null && prepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            return (int) (youtubeCurrentPosition * 1000);
        }
        return (mediaPlayer != null && prepared) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            return (int) (youtubeDuration * 1000);
        }
        return (mediaPlayer != null && prepared) ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int ms) {
        if (currentSong != null && currentSong.getId() != null && currentSong.getId().startsWith("youtube_")) {
            if (youTubePlayer != null) {
                youTubePlayer.seekTo(ms / 1000f);
            }
            return;
        }
        if (mediaPlayer != null && prepared) mediaPlayer.seekTo(ms);
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public String getQueueTitle() {
        return queueTitle;
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

    public boolean isRepeatEnabled() {
        return repeatEnabled;
    }

    public void setRepeatEnabled(boolean repeatEnabled) {
        this.repeatEnabled = repeatEnabled;
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public void setShuffleEnabled(boolean shuffleEnabled) {
        this.shuffleEnabled = shuffleEnabled;

        if (shuffleEnabled) {
            java.util.List<Song> shuffled = new java.util.ArrayList<>(originalQueue);
            if (currentSong != null) {
                int currentSongIdx = -1;
                for (int i = 0; i < shuffled.size(); i++) {
                    if (shuffled.get(i).getId().equals(currentSong.getId())) {
                        currentSongIdx = i;
                        break;
                    }
                }
                if (currentSongIdx != -1) {
                    Song songToKeep = shuffled.remove(currentSongIdx);
                    java.util.Collections.shuffle(shuffled);
                    shuffled.add(0, songToKeep);
                    this.currentIndex = 0;
                } else {
                    java.util.Collections.shuffle(shuffled);
                }
            } else {
                java.util.Collections.shuffle(shuffled);
            }
            this.currentQueue = shuffled;
        } else {
            this.currentQueue = new java.util.ArrayList<>(originalQueue);
            if (currentSong != null) {
                for (int i = 0; i < currentQueue.size(); i++) {
                    if (currentQueue.get(i).getId().equals(currentSong.getId())) {
                        this.currentIndex = i;
                        break;
                    }
                }
            }
        }
    }

    public void setCurrentSongIndex(int index) {
        if (index >= 0 && index < currentQueue.size()) {
            this.currentIndex = index;
        }
    }

    public void playSong(Context context, Song song) {
        if (song != null) {
            play(context, currentQueue, currentIndex, queueTitle);
        }
    }

    private void preResolveNext() {
        if (hasNext()) {
            Log.d(TAG, "Next song available in queue");
        }
    }
}
