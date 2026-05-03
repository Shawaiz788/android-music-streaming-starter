package com.example.musicplayer;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DriveUploader {

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzoE2PJc2zX9pZJOTFLugw5PKJuOVDxcFHGcJHvmVX8dZEfE14180iHIkdAfX_SS9wpnw/exec";
    private static final String FOLDER_ID = "1MQbfnIx629erETZWkxokVT-oGk5OZh1A";

    public interface UploadCallback {
        void onSuccess(String directLink);
        void onFailure(Exception e);
    }

    public static void uploadImage(Bitmap bitmap, String fileName, UploadCallback callback) {
        OkHttpClient client = new OkHttpClient();

        int maxWidth = 500;
        int maxHeight = 500;
        float scale = Math.min(((float) maxWidth / bitmap.getWidth()), ((float) maxHeight / bitmap.getHeight()));
        Bitmap resizedBitmap = bitmap;
        if (scale < 1.0f) {
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true);
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] byteArray = stream.toByteArray();

        String base64Image = Base64.encodeToString(byteArray, Base64.NO_WRAP);
        Log.d("DriveUploader", "Base64 Length: " + base64Image.length());

        RequestBody requestBody = new FormBody.Builder()
                .add("base64", base64Image)
                .add("fileName", fileName + ".jpg")
                .add("folderId", FOLDER_ID)
                .build();

        Request request = new Request.Builder()
                .url(SCRIPT_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("DriveUploader", "Network Error: ", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string().trim();
                    Log.d("DriveUploader", "Raw Response: " + responseData);

                    boolean isLikelyError = responseData.contains(" ") || 
                                           responseData.contains(":") || 
                                           responseData.toLowerCase().contains("error") || 
                                           responseData.toLowerCase().contains("exception") ||
                                           responseData.length() < 10;

                    if (isLikelyError) {
                        Log.e("DriveUploader", "Upload Failed: " + responseData);
                        callback.onFailure(new Exception(responseData));
                        return;
                    }

                    String fileId = responseData;
                    String directLink = "https://drive.google.com/uc?export=download&id=" + fileId;
                    
                    Log.d("DriveUploader", "Upload Successful! ID: " + fileId);
                    callback.onSuccess(directLink);
                } else {
                    callback.onFailure(new IOException("HTTP Error " + response.code()));
                }
            }
        });
    }
}
