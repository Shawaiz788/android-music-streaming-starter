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

public class FavouritePlaylistFragment extends Fragment {

    private RecyclerView rvPlaylists;
    private ImageView ivBack;
    private TextView tvNoPlaylists;
    private PlaylistAdapter adapter;
    private MyApplication.OnPlaylistsLoadedListener playlistsLoadedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourite_playlist, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPlaylists = view.findViewById(R.id.rvPlaylists);
        ivBack = view.findViewById(R.id.ivBack);
        tvNoPlaylists = view.findViewById(R.id.tvNoPlaylists);

        ivBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        setupRecyclerView();

        playlistsLoadedListener = playlists -> {
            if (isAdded()) {
                getActivity().runOnUiThread(this::updateUI);
            }
        };

        MyApplication.subscribePlaylists(playlistsLoadedListener);
        updateUI();
    }

    private void setupRecyclerView() {
        adapter = new PlaylistAdapter(requireContext(), MyApplication.favouritePlaylists);
        rvPlaylists.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPlaylists.setAdapter(adapter);
    }

    private void updateUI() {
        if (MyApplication.favouritePlaylists.isEmpty()) {
            if (tvNoPlaylists != null) tvNoPlaylists.setVisibility(View.VISIBLE);
            rvPlaylists.setVisibility(View.GONE);
        } else {
            if (tvNoPlaylists != null) tvNoPlaylists.setVisibility(View.GONE);
            rvPlaylists.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playlistsLoadedListener != null) {
            MyApplication.unsubscribePlaylists(playlistsLoadedListener);
        }
    }
}
