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

    LinearLayout LLTracks, LLArtists, LLAlbums, LLPlaylists, LLDownload;
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
        LLAlbums = view.findViewById(R.id.LLAlbums);
        LLPlaylists = view.findViewById(R.id.LLPlaylists);
        LLDownload = view.findViewById(R.id.LLDownload);
        cvProfile = view.findViewById(R.id.cvProfile);
        
        LLTracks.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.favouriteTracksFragment);
        });

        LLAlbums.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.FavouriteAlbumsFragment);
        });

        LLPlaylists.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.favouritePlaylistFragment);
        });

        LLDownload.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.downloadsFragment);
        });

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
    }
}
