package com.example.musicplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadManager {
    private static DownloadManager instance;
    private final Map<String, Integer> activeDownloadsProgress = new HashMap<>();
    private final Map<String, Song> activeDownloadsSongs = new HashMap<>();
    private final List<DownloadProgressListener> listeners = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface DownloadProgressListener {
        void onDownloadProgress(String songId, int progress);
        void onDownloadCompleted(String songId);
        void onDownloadFailed(String songId, Exception e);
    }

    private DownloadManager() {}

    public static synchronized DownloadManager getInstance() {
        if (instance == null) {
            instance = new DownloadManager();
        }
        return instance;
    }

    public void addListener(DownloadProgressListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(DownloadProgressListener listener) {
        listeners.remove(listener);
    }

    public void startDownload(Context context, Song song) {
        if (activeDownloadsSongs.containsKey(song.getId())) return;

        activeDownloadsSongs.put(song.getId(), song);
        activeDownloadsProgress.put(song.getId(), 0);
        notifyProgress(song.getId(), 0);

        DBManager dbManager = new DBManager(context);
        dbManager.Open();
        dbManager.AddSongsWithProgress(song, new DBManager.OnDownloadProgressListener() {
            @Override
            public void onProgress(int progress) {
                activeDownloadsProgress.put(song.getId(), progress);
                notifyProgress(song.getId(), progress);
            }

            @Override
            public void onDownloadComplete() {
                // Update MyApplication downloaded songs list first to avoid flickering in UI
                MyApplication.downloadedSongs.clear();
                MyApplication.downloadedSongs.addAll(dbManager.getAllDownloadedSongs());

                activeDownloadsSongs.remove(song.getId());
                activeDownloadsProgress.remove(song.getId());
                notifyCompleted(song.getId());
                
                MyApplication.notifyDownloadsLoaded();
                dbManager.Close();
            }

            @Override
            public void onDownloadFailed(Exception e) {
                activeDownloadsSongs.remove(song.getId());
                activeDownloadsProgress.remove(song.getId());
                notifyFailed(song.getId(), e);
                dbManager.Close();
            }
        });
    }

    public boolean isDownloading(String songId) {
        return activeDownloadsSongs.containsKey(songId);
    }

    public int getProgress(String songId) {
        return activeDownloadsProgress.getOrDefault(songId, 0);
    }

    public List<Song> getDownloadingSongs() {
        return new ArrayList<>(activeDownloadsSongs.values());
    }

    private void notifyProgress(String songId, int progress) {
        mainHandler.post(() -> {
            for (DownloadProgressListener listener : listeners) {
                listener.onDownloadProgress(songId, progress);
            }
        });
    }

    private void notifyCompleted(String songId) {
        mainHandler.post(() -> {
            for (DownloadProgressListener listener : listeners) {
                listener.onDownloadCompleted(songId);
            }
        });
    }

    private void notifyFailed(String songId, Exception e) {
        mainHandler.post(() -> {
            for (DownloadProgressListener listener : listeners) {
                listener.onDownloadFailed(songId, e);
            }
        });
    }
}
