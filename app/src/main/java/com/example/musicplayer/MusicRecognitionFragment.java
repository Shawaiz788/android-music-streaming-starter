package com.example.musicplayer;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MusicRecognitionFragment extends Fragment {

    private static final String TAG = "MusicRecognition";
    private static final int PERMISSION_REQUEST_CODE = 101;
    // For demo/test purposes. Get a real token at https://audd.io/
    private static final String AUDD_API_TOKEN = "21888d16d53c17b1820ec1026206428c";

    private ImageView btnBack;
    private TextView tvStatus, tvResultTitle, tvResultArtist;
    private CardView btnIdentify;
    private View pulseView1, pulseView2;
    private LinearLayout resultLayout;
    private MaterialButton btnPlayResult;
    private boolean isListening = false;
    private AnimatorSet pulseAnimatorSet;

    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private final OkHttpClient client = new OkHttpClient();

    public MusicRecognitionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_recognition, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        tvStatus = view.findViewById(R.id.tvStatus);
        btnIdentify = view.findViewById(R.id.btnIdentify);
        pulseView1 = view.findViewById(R.id.pulseView1);
        pulseView2 = view.findViewById(R.id.pulseView2);
        resultLayout = view.findViewById(R.id.resultLayout);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultArtist = view.findViewById(R.id.tvResultArtist);
        btnPlayResult = view.findViewById(R.id.btnPlayResult);

        audioFilePath = requireContext().getCacheDir().getAbsolutePath() + "/recognition_sample.mp4";

        btnBack.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(MusicRecognitionFragment.this);
            navController.navigate(R.id.favoritesFragment);
        });

        btnIdentify.setOnClickListener(v -> {
            if (!isListening) {
                checkPermissionAndStart();
            } else {
                stopListening();
            }
        });
    }

    private void checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED) {
            startListening();
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(requireContext(), "Microphone permission is required to identify music", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startListening() {
        isListening = true;
        tvStatus.setText("Listening...");
        resultLayout.setVisibility(View.GONE);
        
        pulseView1.setVisibility(View.VISIBLE);
        pulseView2.setVisibility(View.VISIBLE);
        startPulseAnimation();

        try {
            startRecording();
            // Automatically stop and identify after 10 seconds of recording
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isListening && isAdded()) {
                    stopRecordingAndIdentify();
                }
            }, 10000);
        } catch (IOException e) {
            Log.e(TAG, "Recording failed", e);
            stopListening();
            Toast.makeText(requireContext(), "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void startRecording() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.prepare();
        mediaRecorder.start();
    }

    private void stopRecordingAndIdentify() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.e(TAG, "MediaRecorder stop failed", e);
            }
            mediaRecorder.release();
            mediaRecorder = null;
        }
        
        tvStatus.setText("Identifying...");
        performAuddRequest();
    }

    private void performAuddRequest() {
        File file = new File(audioFilePath);
        if (!file.exists()) {
            stopListening();
            return;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_token", AUDD_API_TOKEN)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(file, MediaType.parse("audio/mp4")))
                .build();

        Request request = new Request.Builder()
                .url("https://api.audd.io/")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        stopListening();
                        Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String responseData = response.body().string();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (isAdded()) {
                        parseAuddResponse(responseData);
                    }
                });
            }
        });
    }

    private void parseAuddResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String status = jsonObject.optString("status", "error");

            if ("success".equals(status) && !jsonObject.isNull("result")) {
                JSONObject result = jsonObject.getJSONObject("result");
                String title = result.getString("title");
                String artist = result.getString("artist");
                showResult(title, artist);
            } else {
                stopListening();
                tvStatus.setText("No match found");
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parsing error", e);
            stopListening();
            tvStatus.setText("Recognition failed");
        }
    }

    private void stopListening() {
        isListening = false;
        tvStatus.setText("Tap to Identify");
        if (pulseAnimatorSet != null) {
            pulseAnimatorSet.cancel();
        }
        pulseView1.setVisibility(View.INVISIBLE);
        pulseView2.setVisibility(View.INVISIBLE);
        
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (Exception ignored) {}
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void startPulseAnimation() {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.5f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.5f);
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.0f);

        ValueAnimator animator1 = ValueAnimator.ofPropertyValuesHolder(scaleX, scaleY, alpha);
        animator1.setDuration(2000);
        animator1.setRepeatCount(ValueAnimator.INFINITE);
        animator1.setInterpolator(new AccelerateDecelerateInterpolator());
        animator1.addUpdateListener(animation -> {
            float s = (float) animation.getAnimatedValue("scaleX");
            float a = (float) animation.getAnimatedValue("alpha");
            pulseView1.setScaleX(s);
            pulseView1.setScaleY(s);
            pulseView1.setAlpha(a);
        });

        ValueAnimator animator2 = ValueAnimator.ofPropertyValuesHolder(scaleX, scaleY, alpha);
        animator2.setDuration(2000);
        animator2.setStartDelay(1000);
        animator2.setRepeatCount(ValueAnimator.INFINITE);
        animator2.setInterpolator(new AccelerateDecelerateInterpolator());
        animator2.addUpdateListener(animation -> {
            float s = (float) animation.getAnimatedValue("scaleX");
            float a = (float) animation.getAnimatedValue("alpha");
            pulseView2.setScaleX(s);
            pulseView2.setScaleY(s);
            pulseView2.setAlpha(a);
        });

        pulseAnimatorSet = new AnimatorSet();
        pulseAnimatorSet.playTogether(animator1, animator2);
        pulseAnimatorSet.start();
    }

    private void showResult(String title, String artist) {
        stopListening();
        tvStatus.setText("Match Found!");
        tvResultTitle.setText(title);
        tvResultArtist.setText(artist);
        resultLayout.setVisibility(View.VISIBLE);
        
        btnPlayResult.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("initialQuery", title);
            NavController navController = NavHostFragment.findNavController(MusicRecognitionFragment.this);
            navController.navigate(R.id.searchFragment, bundle);
        });

        resultLayout.setAlpha(0f);
        resultLayout.setTranslationY(50f);
        resultLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (pulseAnimatorSet != null) {
            pulseAnimatorSet.cancel();
        }
    }
}
