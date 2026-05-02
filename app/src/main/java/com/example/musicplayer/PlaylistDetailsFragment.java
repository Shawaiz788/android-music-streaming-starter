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
    private ImageButton btnMore;
    
    private Playlist currentPlaylist;
    private int bgColor, circleColor;
    private AlbumSongAdapter adapter;
    private List<Song> playlistSongs = new ArrayList<>();
    private MyApplication.OnPlaylistsLoadedListener playlistListener;

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

        playlistListener = playlists -> {
            if (isAdded() && currentPlaylist != null) {
                for (Playlist p : playlists) {
                    if (p.getId() != null && p.getId().equals(currentPlaylist.getId())) {
                        currentPlaylist = p;
                        requireActivity().runOnUiThread(() -> {
                            setupUI();
                            loadPlaylistSongs();
                        });
                        break;
                    }
                }
            }
        };
        MyApplication.subscribePlaylists(playlistListener);

        toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(view).navigateUp());

        btnMore.setOnClickListener(v -> showOptionsMenu(v));

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
        btnMore = view.findViewById(R.id.btnMore);
        
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

        if (adapter == null) {
            adapter = new AlbumSongAdapter(requireContext(), playlistSongs);
            rvSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvSongs.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void showOptionsMenu(View v) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), v);
        popup.getMenu().add("Delete Playlist");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete Playlist")) {
                deletePlaylist();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deletePlaylist() {
        if (currentPlaylist != null && currentPlaylist.getId() != null) {
            MyApplication.playlistHandler.deletePlaylist(currentPlaylist.getId());
            Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playlistListener != null) {
            MyApplication.unsubscribePlaylists(playlistListener);
        }
    }
}
