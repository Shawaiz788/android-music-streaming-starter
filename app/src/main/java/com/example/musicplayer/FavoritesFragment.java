package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FavoritesFragment extends Fragment {

    LinearLayout LLTracks;

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
        
        LLTracks.setOnClickListener(v -> {
            MyApplication app = (MyApplication) requireActivity().getApplication();
            if (app.songs != null && !app.songs.isEmpty()) {
                // Safely get the song
                Song song = app.songs.get(app.songs.size() > 1 ? 1 : 0);
                // Call the central showPlayerDialog in MainActivity
                ((MainActivity) requireActivity()).showPlayerDialog(song, false);
            }
        });
    }
}
