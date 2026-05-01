package com.example.musicplayer;

import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class MusicRecognitionFragment extends Fragment {

    private ImageView btnBack;
    private TextView tvStatus, tvResultTitle, tvResultArtist;
    private CardView btnIdentify;
    private View pulseView1, pulseView2;
    private LinearLayout resultLayout;
    private boolean isListening = false;
    private AnimatorSet pulseAnimatorSet;

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

        btnBack.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(MusicRecognitionFragment.this);
            navController.navigate(R.id.favoritesFragment);
        });

        btnIdentify.setOnClickListener(v -> {
            if (!isListening) {
                startListening();
            } else {
                stopListening();
            }
        });
    }

    private void startListening() {
        isListening = true;
        tvStatus.setText("Listening...");
        resultLayout.setVisibility(View.GONE);
        
        // Start pulse animation
        pulseView1.setVisibility(View.VISIBLE);
        pulseView2.setVisibility(View.VISIBLE);
        
        startPulseAnimation();

        // Simulate recognition process
        new Handler().postDelayed(() -> {
            if (isAdded()) {
                showResult("Burning", "Podval Caplella");
            }
        }, 5000);
    }

    private void stopListening() {
        isListening = false;
        tvStatus.setText("Tap to Identify");
        if (pulseAnimatorSet != null) {
            pulseAnimatorSet.cancel();
        }
        pulseView1.setVisibility(View.INVISIBLE);
        pulseView2.setVisibility(View.INVISIBLE);
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
        
        // Optional: Animation for result layout
        resultLayout.setAlpha(0f);
        resultLayout.setTranslationY(50f);
        resultLayout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start();
    }
}
