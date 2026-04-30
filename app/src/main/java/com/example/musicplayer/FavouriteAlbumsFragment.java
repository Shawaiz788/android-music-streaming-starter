package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FavouriteAlbumsFragment extends Fragment {

    private RecyclerView rvAlbums;
    private AlbumAdapter adapter;
    private ImageView btnBack;

    public FavouriteAlbumsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourite_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAlbums = view.findViewById(R.id.rvAlbums);
        btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(FavouriteAlbumsFragment.this);
            navController.navigate(R.id.homeFragment);
        });

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        adapter = new AlbumAdapter(requireContext(), MyApplication.favouriteAlbums);
        rvAlbums.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlbums.setAdapter(adapter);
    }
}
