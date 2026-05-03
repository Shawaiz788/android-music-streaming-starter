package com.example.musicplayer;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUtils {

    private static final String TAG = "DownloadUtils";
    private static final String AUDIO_DIR = "downloaded_audio";
    private static final String COVER_DIR = "downloaded_covers";

    public interface DownloadCallback {
        void onDownloadComplete(String localPath);
        void onDownloadFailed(Exception e);
    }



    public static void getLocalPath(Context context, Song song, DownloadCallback callback) {
        String fileName = song.getId() + ".mp3";
        File dir = new File(context.getFilesDir(), AUDIO_DIR);
        if (!dir.exists())
            dir.mkdirs();

        File file = new File(dir, fileName);
        if (file.exists()) {
            callback.onDownloadComplete(file.getAbsolutePath());
            return;
        }

        downloadFile(song.getSongUrl(), file, callback);
    }

    public static void getCoverPath(Context context, Song song, DownloadCallback callback) {
        String fileName = song.getId() + ".jpg";
        File dir = new File(context.getFilesDir(), COVER_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        if (file.exists()) {
            callback.onDownloadComplete(file.getAbsolutePath());
            return;
        }
        downloadFile(song.getImageUrl(), file, callback);
    }

    private static void downloadFile(String urlString, File destination, DownloadCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    callback.onDownloadFailed(new Exception("Server returned HTTP " + connection.getResponseCode()));
                    return;
                }

                try (InputStream input = new BufferedInputStream(url.openStream());
                     FileOutputStream output = new FileOutputStream(destination)) {

                    byte[] data = new byte[8192];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                    output.flush();
                }

                callback.onDownloadComplete(destination.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Download error: " + e.getMessage());
                if (destination.exists()) destination.delete();
                callback.onDownloadFailed(e);
            }
        }).start();
    }
}
