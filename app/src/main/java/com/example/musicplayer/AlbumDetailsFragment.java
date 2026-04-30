package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class AlbumDetailsFragment extends Fragment {

    private ImageView ivAlbumArt;
    private TextView tvAlbumTitle, tvAlbumInfo;
    private RecyclerView rvSongs;
    private ImageButton btnShare, btnFavorite;
    private FloatingActionButton btnPlay;
    private Toolbar toolbar;
    private Album currentAlbum;
    private AlbumSongAdapter adapter;
    private List<Song> albumSongs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            currentAlbum = (Album) getArguments().getSerializable("album");
            if (currentAlbum != null) {
                setupUI();
                loadAlbumSongs();
            }
        }

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        btnPlay.setOnClickListener(v -> {
            if (!albumSongs.isEmpty()) {
                ((MainActivity) requireActivity()).showPlayerDialog(albumSongs.get(0), true);
            } else {
                Toast.makeText(getContext(), "No songs in this album", Toast.LENGTH_SHORT).show();
            }
        });

        btnFavorite.setOnClickListener(v -> {
            // Handle favorite album logic
            Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews(View view) {
        ivAlbumArt = view.findViewById(R.id.ivAlbumArt);
        tvAlbumTitle = view.findViewById(R.id.tvAlbumTitle);
        tvAlbumInfo = view.findViewById(R.id.tvAlbumInfo);
        rvSongs = view.findViewById(R.id.rvSongs);
        btnShare = view.findViewById(R.id.btnShare);
        btnPlay = view.findViewById(R.id.btnPlay);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        toolbar = view.findViewById(R.id.toolbar);
    }

    private void setupUI() {
        tvAlbumTitle.setText(currentAlbum.getTitle());
        String info = currentAlbum.getArtist() + " • " + (currentAlbum.getYear() != null ? currentAlbum.getYear() : "Unknown");
        tvAlbumInfo.setText(info);

        Glide.with(this)
                .load(currentAlbum.getImageUrl())
                .placeholder(R.drawable.hungama)
                .into(ivAlbumArt);
    }

    private void loadAlbumSongs() {
        albumSongs.clear();
        // Filter global songs list for songs belonging to this album
        for (Song song : MyApplication.songs) {
            if (song.getAlbumId() != null && song.getAlbumId().equals(currentAlbum.getId())) {
                albumSongs.add(song);
            }
        }

        adapter = new AlbumSongAdapter(requireContext(), albumSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);
    }
}
