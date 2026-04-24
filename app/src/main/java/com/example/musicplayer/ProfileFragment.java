package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ProfileFragment extends Fragment {

    ConstraintLayout btnQuit;
    FirebaseAuth auth;
    FirebaseUser user;

    ImageView btnClose;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnQuit=view.findViewById(R.id.btnQuit);
        btnClose = view.findViewById(R.id.closeBtn);
        auth= FirebaseAuth.getInstance();
        user=auth.getCurrentUser();

        btnClose.setOnClickListener(v -> {

            NavController navController =
                    NavHostFragment.findNavController(ProfileFragment.this);

            navController.navigate(R.id.homeFragment);

        });
        btnQuit.setOnClickListener((v)->{

            auth.signOut();
            Intent i=new Intent(requireContext(),SignInSignup.class);
            startActivity(i);
            requireActivity().finish();
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);

    }
    
}