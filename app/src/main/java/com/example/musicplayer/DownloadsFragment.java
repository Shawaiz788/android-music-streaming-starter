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

public class DownloadsFragment extends Fragment {

    private RecyclerView rvDownloads;
    private TextView tvNoDownloads;
    private ImageView ivBack;
    private SearchAdapter adapter;
    private DBManager dbManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDownloads = view.findViewById(R.id.rvDownloads);
        tvNoDownloads = view.findViewById(R.id.tvNoDownloads);
        ivBack = view.findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        dbManager = new DBManager(requireContext());
        dbManager.Open();

        ArrayList<Song> downloadedSongs = dbManager.getAllDownloadedSongs();
        dbManager.Close();

        if (downloadedSongs.isEmpty()) {
            tvNoDownloads.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        } else {
            tvNoDownloads.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
            
            adapter = new SearchAdapter(requireContext(), downloadedSongs);
            rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvDownloads.setAdapter(adapter);
        }
    }
}