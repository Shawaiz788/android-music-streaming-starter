package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class MusicService extends Service {

    public static final String CHANNEL_ID = "music_player_channel";
    private static final int NOTIFICATION_ID = 1;

    private MediaSessionCompat mediaSession;
    private PlayerManager playerManager;

    @Override
    public void onCreate() {
        super.onCreate();
        playerManager = PlayerManager.getInstance();
        
        mediaSession = new MediaSessionCompat(this, "MusicService");
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                playerManager.resume();
            }

            @Override
            public void onPause() {
                playerManager.pause();
            }

            @Override
            public void onSkipToNext() {
                playerManager.playNext(MusicService.this);
            }

            @Override
            public void onSkipToPrevious() {
                playerManager.playPrevious(MusicService.this);
            }

            @Override
            public void onStop() {
                playerManager.stop();
                stopSelf();
            }
        });
        mediaSession.setActive(true);
        
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        
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
            }
        }
        
        updateNotification();
        return START_NOT_STICKY;
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
        if (song == null) {
            stopForeground(true);
            return;
        }

        boolean isPlaying = playerManager.isPlaying();

        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist());
        
        mediaSession.setMetadata(metadataBuilder.build());

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .setState(isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        playerManager.getCurrentPosition(), 1.0f);
        
        mediaSession.setPlaybackState(stateBuilder.build());

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_search_music)
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setOngoing(isPlaying)
                .setContentIntent(pendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .setPriority(NotificationCompat.PRIORITY_LOW);

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

        // Always call startForeground first to satisfy Android requirements
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIFICATION_ID, builder.build());
        }

        // Then optionally update the large icon when Glide finishes
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
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

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
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
        mediaSession.release();
        super.onDestroy();
    }
}
