package com.example.musicplayer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtils {

    private static final String TAG = "DownloadUtils";
    private static final String AUDIO_DIR = "downloaded_audio";
    private static final String COVER_DIR = "downloaded_covers";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    public interface DownloadCallback {
        void onDownloadComplete(String localPath);
        void onDownloadFailed(Exception e);
    }

    public static void getLocalPath(Context context, Song song, DownloadCallback callback) {
        String fileName = song.getId() + ".mp3";
        File dir = new File(context.getFilesDir(), AUDIO_DIR);
        if (!dir.exists()) dir.mkdirs();

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
        
        String imageUrl = song.getImageUrl();
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onDownloadFailed(new Exception("No cover image URL available"));
            return;
        }

        downloadFile(imageUrl, file, callback);
    }

    private static void downloadFile(String urlString, File destination, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(urlString)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Download failed: " + e.getMessage());
                callback.onDownloadFailed(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onDownloadFailed(new IOException("Unexpected code " + response));
                    return;
                }

                try (InputStream inputStream = response.body().byteStream();
                     FileOutputStream outputStream = new FileOutputStream(destination)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.flush();
                    callback.onDownloadComplete(destination.getAbsolutePath());
                } catch (Exception e) {
                    if (destination.exists()) destination.delete();
                    callback.onDownloadFailed(e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
