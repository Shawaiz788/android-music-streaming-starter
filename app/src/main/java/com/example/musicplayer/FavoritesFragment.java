package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

public class FavoritesFragment extends Fragment {

    LinearLayout LLTracks;
    CardView cvProfile;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LLTracks = view.findViewById(R.id.LLTracks);
        cvProfile=view.findViewById(R.id.cvProfile);
        
        LLTracks.setOnClickListener(v -> {

            if (MyApplication.songs != null && !MyApplication.songs.isEmpty()) {
                Song song = MyApplication.songs.get(0);
                ((MainActivity) requireActivity()).showPlayerDialog(song, false);
            }
        });

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
    }
}
