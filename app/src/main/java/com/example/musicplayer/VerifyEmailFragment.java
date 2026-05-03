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
            user.reload().addOnCompleteListener(task -> {
                if (user.isEmailVerified()) {
                    Toast.makeText(getContext(), R.string.email_verified, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getActivity(), MainActivity.class));
                    requireActivity().finish();
                } else {
                    Toast.makeText(getContext(), R.string.verify_email_first, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resendVerificationEmail() {
        if (user != null) {
            user.sendEmailVerification().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Toast.makeText(getContext(), getString(R.string.verification_email_sent, user.getEmail()), Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        } else {
            Toast.makeText(getContext(), R.string.session_expired, Toast.LENGTH_SHORT).show();
        }
    }
}