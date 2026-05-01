package com.example.musicplayer;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailsFragment extends Fragment {

    private View viewPlaylistColor, rootLayout;
    private TextView tvPlaylistTitle, tvPlaylistInfo;
    private RecyclerView rvSongs;
    private FloatingActionButton btnPlay;
    private Toolbar toolbar;
    
    private Playlist currentPlaylist;
    private int bgColor, circleColor;
    private AlbumSongAdapter adapter;
    private List<Song> playlistSongs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            currentPlaylist = (Playlist) getArguments().getSerializable("playlist");
            bgColor = getArguments().getInt("bgColor");
            circleColor = getArguments().getInt("circleColor");
            
            if (currentPlaylist != null) {
                // Recalculate duration here to ensure it's accurate with current song data
                currentPlaylist.calculateAndSetDuration(MyApplication.songs);
                setupUI();
                loadPlaylistSongs();
            }
        }

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        btnPlay.setOnClickListener(v -> {
            if (!playlistSongs.isEmpty()) {
                ((MainActivity) requireActivity()).showPlayerDialog(playlistSongs.get(0), false, playlistSongs, 0);
            } else {
                Toast.makeText(getContext(), "No songs in this playlist", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.rootLayout);
        viewPlaylistColor = view.findViewById(R.id.viewPlaylistColor);
        tvPlaylistTitle = view.findViewById(R.id.tvPlaylistTitle);
        tvPlaylistInfo = view.findViewById(R.id.tvPlaylistInfo);
        rvSongs = view.findViewById(R.id.rvSongs);
        btnPlay = view.findViewById(R.id.btnPlay);
        toolbar = view.findViewById(R.id.toolbar);
        
        setupImmersiveBackground(bgColor);
    }

    private void setupImmersiveBackground(int color) {
        // Create a smooth top-to-bottom gradient
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{color, 0xFF000000} // Fades from playlist color to black
        );
        rootLayout.setBackground(gd);
    }

    private void setupUI() {
        tvPlaylistTitle.setText(currentPlaylist.getTitle());
        String info = currentPlaylist.getTrackCount() + " tracks • " + 
                     (currentPlaylist.getDuration() != null ? currentPlaylist.getDuration() : "0 min");
        tvPlaylistInfo.setText(info);
        
        if (viewPlaylistColor.getBackground() != null) {
            viewPlaylistColor.getBackground().mutate().setTint(circleColor);
        }
    }

    private void loadPlaylistSongs() {
        playlistSongs.clear();
        if (currentPlaylist.getSongIds() != null) {
            for (String songId : currentPlaylist.getSongIds()) {
                for (Song song : MyApplication.songs) {
                    if (song.getId() != null && song.getId().equals(songId)) {
                        playlistSongs.add(song);
                        break;
                    }
                }
            }
        }

        adapter = new AlbumSongAdapter(requireContext(), playlistSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSongs.setAdapter(adapter);
    }
}
