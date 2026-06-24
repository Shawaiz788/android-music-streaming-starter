package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

public class MusicService extends Service {

    public static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "MusicService";

    private MediaSessionCompat mediaSession;
    private PlayerManager playerManager;
    private YouTubePlayerView youTubePlayerView;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean isFocusGranted = false;

    /**
     * Returns true if the currently loaded song is played back through the
     * embedded YouTube WebView player rather than our own MediaPlayer.
     *
     * The embedded YouTube IFrame player runs its own internal audio/video
     * client inside the WebView and will request (and sometimes immediately
     * release) Android audio focus on its own, independent of our app's
     * AudioManager request. That causes spurious AUDIOFOCUS_LOSS callbacks
     * to fire on our listener a moment after we've already been granted
     * focus ourselves -- which would otherwise incorrectly pause playback.
     * For YouTube-backed songs we ignore focus-loss signals since the
     * "competing" focus holder is actually our own embedded player, not a
     * real external app.
     */
    private boolean isCurrentSongYouTube() {
        Song song = playerManager.getCurrentSong();
        return song != null && song.getId() != null && song.getId().startsWith("youtube_");
    }

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener = focusChange -> {
        Log.d(TAG, "onAudioFocusChange: " + focusChange);

        if (isCurrentSongYouTube()) {
            // Ignore focus loss/gain churn caused by the embedded YouTube
            // WebView's own internal player. We still want to know if we
            // gained focus so notifications/UI reflect that, but we never
            // pause YouTube playback based on these signals.
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                isFocusGranted = true;
            }
            Log.d(TAG, "Ignoring focus change (" + focusChange + ") for embedded YouTube playback");
            return;
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                isFocusGranted = false;
                if (playerManager.isPlaying()) playerManager.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (playerManager.isPlaying()) playerManager.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Duck instead of pausing -- a transient duck request (e.g. a
                // notification sound) shouldn't stop playback outright.
                if (playerManager.getMediaPlayer() != null) {
                    try {
                        playerManager.getMediaPlayer().setVolume(0.2f, 0.2f);
                    } catch (Exception ignored) {}
                }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                isFocusGranted = true;
                if (playerManager.getMediaPlayer() != null) {
                    try {
                        playerManager.getMediaPlayer().setVolume(1f, 1f);
                    } catch (Exception ignored) {}
                }
                playerManager.resume();
                break;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        playerManager = PlayerManager.getInstance();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initialize YouTube player in the service for background playback.
        // We use a 1x1 hidden WindowManager view to keep the WebView active in background.
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                youTubePlayerView = new YouTubePlayerView(this);
                youTubePlayerView.setEnableAutomaticInitialization(false);

                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE;

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        1, 1, layoutType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT);
                params.gravity = Gravity.TOP | Gravity.START;
                params.x = 0;
                params.y = 0;

                try {
                    windowManager.addView(youTubePlayerView, params);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to add YouTubePlayerView to WindowManager", e);
                }

                youTubePlayerView.initialize(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(@NonNull YouTubePlayer initializedYouTubePlayer) {
                        Log.d(TAG, "YouTube Player Ready in Service");
                        playerManager.setYouTubePlayer(initializedYouTubePlayer);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error initializing YouTubePlayerView", e);
            }
        });

        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() { playerManager.resume(); }
            @Override
            public void onPause() { playerManager.pause(); }
            @Override
            public void onSkipToNext() { playerManager.playNext(MusicService.this); }
            @Override
            public void onSkipToPrevious() { playerManager.playPrevious(MusicService.this); }
            @Override
            public void onStop() {
                isFocusGranted = false;
                playerManager.stop();
                stopSelf();
            }
            @Override
            public void onSeekTo(long pos) {
                playerManager.seekTo((int) pos);
            }
        });
        mediaSession.setActive(true);

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + (intent != null ? intent.getAction() : "null"));

        if (intent != null && Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
            return START_STICKY;
        }

        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case "PLAY_PAUSE":
                    playerManager.togglePlayPause();
                    break;
                case "NEXT":
                    playerManager.playNext(this);
                    break;
                case "PREVIOUS":
                    playerManager.playPrevious(this);
                    break;
                case "STOP":
                    playerManager.stop();
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        // Request focus proactively whenever a song is loaded, rather than
        // gating on isPlaying() (which can still be false while the media
        // is preparing/buffering, causing focus to be requested too late).
        if (playerManager.getCurrentSong() != null) {
            requestAudioFocus();
        }

        updateNotification();
        return START_STICKY;
    }

    private void requestAudioFocus() {
        if (isFocusGranted) return;

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build();
            result = audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            result = audioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            isFocusGranted = true;
            Log.d(TAG, "Audio Focus Granted");
        } else {
            Log.d(TAG, "Audio Focus Request Failed: " + result);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void updateNotification() {
        Song song = playerManager.getCurrentSong();
        boolean isPlaying = playerManager.isPlaying();

        String title = (song != null) ? song.getTitle() : "Amplify";
        String artist = (song != null) ? song.getArtist() : "Music Player";

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist);

        if (song != null) {
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playerManager.getDuration());
        }

        mediaSession.setMetadata(metadataBuilder.build());

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_SEEK_TO)
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        playerManager.getCurrentPosition(), 1.0f);

        mediaSession.setPlaybackState(stateBuilder.build());

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_search_music)
                .setContentTitle(title)
                .setContentText(artist)
                .setOngoing(isPlaying || (song != null && !playerManager.isCompleted()))
                .setContentIntent(pendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Previous button
        builder.addAction(new NotificationCompat.Action(
                android.R.drawable.ic_media_previous, "Previous",
                getPendingIntent("PREVIOUS")));

        // Play/Pause button
        builder.addAction(new NotificationCompat.Action(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                isPlaying ? "Pause" : "Play",
                getPendingIntent("PLAY_PAUSE")));

        // Next button
        builder.addAction(new NotificationCompat.Action(
                android.R.drawable.ic_media_next, "Next",
                getPendingIntent("NEXT")));

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (song != null && song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(song.getImageUrl())
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            builder.setLargeIcon(resource);
                            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource);
                            mediaSession.setMetadata(metadataBuilder.build());
                            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                            if (manager != null) {
                                manager.notify(NOTIFICATION_ID, builder.build());
                            }
                        }
                        @Override public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                    });
        }
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        isFocusGranted = false;
        if (youTubePlayerView != null) {
            try {
                WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
                windowManager.removeView(youTubePlayerView);
            } catch (Exception ignored) {}
            youTubePlayerView.release();
        }
        playerManager.setYouTubePlayer(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(focusChangeListener);
        }
        mediaSession.release();
        super.onDestroy();
    }
}