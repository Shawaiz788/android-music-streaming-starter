package com.example.musicplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;


public class ReleasesFragment extends Fragment {

   RecyclerView rvReleases;
   RvReleasesAdapter adapter;
   ImageButton btn_back;
   MyApplication.OnSongsLoadedListener songsLoadedListener;

    public ReleasesFragment() {
    }


    private boolean isLoading = false;
    private int currentIndex = 0;
    private static final int PAGE_SIZE = 50;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        ThemeHelper.applyTheme(view);

        btn_back=view.findViewById(R.id.btn_back);
        btn_back.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();

        });
        rvReleases=view.findViewById(R.id.rvReleases);
        adapter=new RvReleasesAdapter(requireContext(),MyApplication.newReleases);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false);
        rvReleases.setLayoutManager(layoutManager);
        rvReleases.setAdapter(adapter);

        rvReleases.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int pastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 10) {
                            loadMore();
                        }
                    }
                }
            }
        });

        songsLoadedListener = songs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        };
        MyApplication.subscribe(songsLoadedListener);
        
        if (MyApplication.newReleases.isEmpty()) {
            loadMore();
        } else {
            currentIndex = MyApplication.newReleases.size();
        }
    }

    private void loadMore() {
        isLoading = true;
        MyApplication.youtubeApiHandler.searchImmediate("Trending Music 2025", new YouTubeApiHandler.YouTubeCallback<java.util.List<Song>>() {
            @Override
            public void onSuccess(java.util.List<Song> result) {
                if (isAdded()) {
                    int start = MyApplication.newReleases.size();
                    for (Song s : result) {
                        if (!MyApplication.newReleases.contains(s)) {
                            MyApplication.newReleases.add(s);
                            if (!MyApplication.songs.contains(s)) {
                                MyApplication.songs.add(s);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    currentIndex += result.size();
                    isLoading = false;
                }
            }

            @Override
            public void onError(Exception e) {
                isLoading = false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (songsLoadedListener != null) {
            MyApplication.unsubscribe(songsLoadedListener);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_releases, container, false);
    }
}
