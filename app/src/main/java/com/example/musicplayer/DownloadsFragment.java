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

import com.example.musicplayer.Song;

import java.util.ArrayList;

public class DownloadsFragment extends Fragment {

    private RecyclerView rvDownloads;
    private TextView tvNoDownloads;
    private ImageView ivBack;
    private SearchAdapter adapter;
    private MyApplication.OnDownloadsLoadedListener downloadsLoadedListener;
    private DownloadManager.DownloadProgressListener downloadProgressListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ThemeHelper.applyTheme(view);

        rvDownloads = view.findViewById(R.id.rvDownloads);
        tvNoDownloads = view.findViewById(R.id.tvNoDownloads);
        ivBack = view.findViewById(R.id.ivBack);

        ivBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        downloadsLoadedListener = songs -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(this::updateUI);
            }
        };

        downloadProgressListener = new DownloadManager.DownloadProgressListener() {
            @Override public void onDownloadProgress(String songId, int progress) { if (isAdded()) updateUI(); }
            @Override public void onDownloadCompleted(String songId) { if (isAdded()) updateUI(); }
            @Override public void onDownloadFailed(String songId, Exception e) { if (isAdded()) updateUI(); }
        };

        MyApplication.subscribeDownloads(downloadsLoadedListener);
        DownloadManager.getInstance().addListener(downloadProgressListener);
        updateUI();
    }

    private void updateUI() {
        ArrayList<Song> allSongs = new ArrayList<>(DownloadManager.getInstance().getDownloadingSongs());
        allSongs.addAll(MyApplication.downloadedSongs);

        if (allSongs.isEmpty()) {
            tvNoDownloads.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        } else {
            tvNoDownloads.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);

            if (adapter == null) {
                adapter = new SearchAdapter(requireContext(), allSongs);
                rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvDownloads.setAdapter(adapter);
            } else {
                adapter.songs = allSongs;
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadsLoadedListener != null) {
            MyApplication.unsubscribeDownloads(downloadsLoadedListener);
        }
        if (downloadProgressListener != null) {
            DownloadManager.getInstance().removeListener(downloadProgressListener);
        }
    }
}
