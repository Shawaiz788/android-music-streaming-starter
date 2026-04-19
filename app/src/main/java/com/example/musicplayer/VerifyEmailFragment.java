package com.example.musicplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailFragment extends Fragment {

    FirebaseAuth auth;
    FirebaseUser user;
    Button btnContinueLogin;

    public VerifyEmailFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return  inflater.inflate(R.layout.fragment_verify_email, container, false);


    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        user= auth.getCurrentUser();
        btnContinueLogin=view.findViewById(R.id.btnContinueLogin);
        btnContinueLogin.setOnClickListener(v -> {
            checkEmailVerification();
        });

        view.findViewById(R.id.tvResendEmail).setOnClickListener(v -> {
            resendVerificationEmail();
        });
    }

    private void checkEmailVerification() {
        if (user != null) {
            // reload() is crucial to get the latest status from Firebase servers
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(getContext(), "Email Verified!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                    requireActivity().finish();
                } else {
                    Toast.makeText(getContext(), "Please verify your email first.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resendVerificationEmail() {
        if (user != null) {
            user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(getContext(), "Verification email sent to " + user.getEmail(), Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        } else {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }
}