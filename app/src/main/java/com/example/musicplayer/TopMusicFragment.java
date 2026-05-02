package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopMusicFragment extends Fragment {

    private RecyclerView rvTopSongs;
    private SearchAdapter adapter;
    private List<Song> topSongsList = new ArrayList<>();
    private CardView cvProfile, cvTopSong;
    private ImageView ivTopSongBg;
    private TextView tvTopSongTitle, tvTopSongArtist;
    private Map<String, Integer> playCountsMap = new HashMap<>();

    public TopMusicFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_top_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        cvProfile = view.findViewById(R.id.cvProfile);
        rvTopSongs = view.findViewById(R.id.rvTopSongs);
        cvTopSong = view.findViewById(R.id.cvTopSong);
        ivTopSongBg = view.findViewById(R.id.ivTopSongBg);
        tvTopSongTitle = view.findViewById(R.id.tvTopSongTitle);
        tvTopSongArtist = view.findViewById(R.id.tvTopSongArtist);
        
        adapter = new SearchAdapter(requireContext(), topSongsList);
        rvTopSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTopSongs.setAdapter(adapter);

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });

        loadData();
    }

    private void loadData() {
        if (MyApplication.songsHandler == null) return;

        // 1. Load Global Play Counts from Firebase
        MyApplication.songsHandler.loadPlayCounts(counts -> {
            this.playCountsMap = counts;
            refreshTopSongs();
        });

        // 2. Subscribe to songs if not already loaded
        MyApplication.subscribe(songs -> {
            if (isAdded()) {
                refreshTopSongs();
            }
        });
    }

    private void refreshTopSongs() {
        if (MyApplication.songs.isEmpty()) return;

        List<Song> allSongs = new ArrayList<>(MyApplication.songs);
        //use quick sort to sort the songs
        quickSort(allSongs, 0, allSongs.size() - 1);

        topSongsList.clear();
        // take top 50 or all if less
        int limit = Math.min(allSongs.size(), 50);
        for (int i = 0; i < limit; i++) {
            topSongsList.add(allSongs.get(i));
        }

        // Update the Top Card with the #1 song
        if (!topSongsList.isEmpty()) {
            Song topSong = topSongsList.get(0);
            cvTopSong.setVisibility(View.VISIBLE);
            tvTopSongTitle.setText(topSong.getTitle());
            tvTopSongArtist.setText(topSong.getArtist());

            Glide.with(this)
                    .load(topSong.getImageUrl())
                    .placeholder(android.R.color.darker_gray)
                    .into(ivTopSongBg);

            cvTopSong.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showPlayerDialog(topSong, false, topSongsList, 0);
                }
            });
        } else {
            cvTopSong.setVisibility(View.GONE);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // Manual Quick Sort Implementation as requested
    private void quickSort(List<Song> list, int low, int high) {
        if (low < high) {
            int pi = partition(list, low, high);
            quickSort(list, low, pi - 1);
            quickSort(list, pi + 1, high);
        }
    }

    private int partition(List<Song> list, int low, int high) {
        // We sort by play count in descending order
        int pivotCount = getPlayCount(list.get(high).getId());
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            // Descending: current count > pivot count
            if (getPlayCount(list.get(j).getId()) > pivotCount) {
                i++;
                Song temp = list.get(i);
                list.set(i, list.get(j));
                list.set(j, temp);
            }
        }
        Song temp = list.get(i + 1);
        list.set(i + 1, list.get(high));
        list.set(high, temp);
        return i + 1;
    }

    private int getPlayCount(String songId) {
        Integer count = playCountsMap.get(songId);
        return (count != null) ? count : 0;
    }
}
