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

    private static final OkHttpClient client = buildHttpClient();

    private static OkHttpClient buildHttpClient() {
        try {
            javax.net.ssl.TrustManager[] trustAll = new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager() {
                @Override public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                @Override public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[]{}; }
            }};
            javax.net.ssl.SSLContext ctx = javax.net.ssl.SSLContext.getInstance("SSL");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(ctx.getSocketFactory(), (javax.net.ssl.X509TrustManager) trustAll[0])
                    .hostnameVerifier((h, s) -> true)
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            return new OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }
    }

    public interface DownloadCallback {
        void onDownloadComplete(String localPath);
        void onDownloadFailed(Exception e);
        default void onProgress(int progress) {}
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
        // Clean the URL if it was passed with extra brackets or spaces
        String cleanUrl = urlString.trim();
        
        Request request = new Request.Builder()
                .url(cleanUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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
                    Log.e(TAG, "Server returned error: " + response.code() + " " + response.message());
                    callback.onDownloadFailed(new IOException("Unexpected code " + response));
                    return;
                }

                okhttp3.ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "Response body is null");
                    callback.onDownloadFailed(new IOException("Empty response body"));
                    return;
                }

                long totalBytes = body.contentLength();
                Log.d(TAG, "Starting download, total bytes: " + totalBytes);

                try (InputStream inputStream = body.byteStream();
                     FileOutputStream outputStream = new FileOutputStream(destination)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long totalBytesRead = 0;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        if (totalBytes > 0) {
                            int progress = (int) ((totalBytesRead * 100) / totalBytes);
                            callback.onProgress(progress);
                        }
                    }
                    outputStream.flush();
                    Log.d(TAG, "Download finished: " + destination.getAbsolutePath());
                    callback.onDownloadComplete(destination.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Error during file write", e);
                    if (destination.exists()) destination.delete();
                    callback.onDownloadFailed(e);
                } finally {
                    response.close();
                }
            }
        });
    }
}
