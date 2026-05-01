package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FavouriteTracksFragment extends Fragment {

    private RecyclerView rvFavourites;
    private TextView tvNoFavourites;
    private ImageView ivBack;
    private SearchAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourite_tracks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFavourites = view.findViewById(R.id.rvFavourites);
        tvNoFavourites = view.findViewById(R.id.tvNoFavourites);
        ivBack = view.findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        ArrayList<Song> favouriteSongs = MyApplication.favouriteSongs;

        if (favouriteSongs == null || favouriteSongs.isEmpty()) {
            tvNoFavourites.setVisibility(View.VISIBLE);
            rvFavourites.setVisibility(View.GONE);
        } else {
            tvNoFavourites.setVisibility(View.GONE);
            rvFavourites.setVisibility(View.VISIBLE);
            
            adapter = new SearchAdapter(requireContext(), favouriteSongs);
            rvFavourites.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFavourites.setAdapter(adapter);
        }
    }
}