package com.example.musicplayer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    ImageView ivPfp;
    TextView tvViewAll;
    CardView cvProfile;
    MaterialButton btnIdentifyMusic;
    RecyclerView rvReleases, rvAlbums;
    RvReleasesAdapter releasesAdapter;
    AlbumAdapter albumAdapter;
    ShimmerFrameLayout shimmerContainer, shimmerAlbums;
    MyApplication.OnSongsLoadedListener songsLoadedListener;
    MyApplication.OnUserLoadedListener userLoadedListener;
    MyApplication.OnAlbumsLoadedListener albumsLoadedListener;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    private boolean isLoadingAlbums = false;
    private int albumIndex = 0;
    private static final int PAGE_SIZE = 50;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ThemeHelper.applyTheme(view);
        ivPfp = view.findViewById(R.id.ivPfp);
        
        userLoadedListener = user -> {
            if (isAdded() && user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.icon_pfp)
                    .error(R.drawable.icon_pfp)
                    .into(ivPfp);
            }
        };
        MyApplication.subscribeUser(userLoadedListener);

        tvViewAll = view.findViewById(R.id.tvViewAll);
        cvProfile = view.findViewById(R.id.cvProfile);
        btnIdentifyMusic = view.findViewById(R.id.btnIdentifyMusic);
        rvReleases = view.findViewById(R.id.rvReleases);
        rvAlbums = view.findViewById(R.id.rvAlbums);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        shimmerAlbums = view.findViewById(R.id.shimmer_albums);

        if (!MyApplication.newReleases.isEmpty()) {
            rvReleases.setVisibility(View.VISIBLE);
            if (shimmerContainer != null) {
                shimmerContainer.setVisibility(View.GONE);
            }
        } else {
            rvReleases.setVisibility(View.GONE);
            if (shimmerContainer != null) {
                shimmerContainer.startShimmer();
                shimmerContainer.setVisibility(View.VISIBLE);
            }
        }

        if (!MyApplication.allAlbums.isEmpty()) {
            rvAlbums.setVisibility(View.VISIBLE);
            if (shimmerAlbums != null) {
                shimmerAlbums.setVisibility(View.GONE);
            }
        } else {
            rvAlbums.setVisibility(View.GONE);
            if (shimmerAlbums != null) {
                shimmerAlbums.startShimmer();
                shimmerAlbums.setVisibility(View.VISIBLE);
            }
        }

        releasesAdapter = new RvReleasesAdapter(requireContext(), MyApplication.newReleases);
        rvReleases.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvReleases.setAdapter(releasesAdapter);

        albumAdapter = new AlbumAdapter(requireContext(), MyApplication.allAlbums);
        GridLayoutManager albumLayoutManager = new GridLayoutManager(requireContext(), 2);
        rvAlbums.setLayoutManager(albumLayoutManager);
        rvAlbums.setAdapter(albumAdapter);
        rvAlbums.setNestedScrollingEnabled(false);

        if (view instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) view).setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY) { // Scrolling down
                    View child = v.getChildAt(0);
                    if (child != null) {
                        // Check if we are near the bottom of the scroll view
                        if (scrollY >= (child.getMeasuredHeight() - v.getMeasuredHeight() - 500)) {
                            if (!isLoadingAlbums) {
                                loadMoreAlbums();
                            }
                        }
                    }
                }
            });
        }

        songsLoadedListener = songs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (songs != null && !songs.isEmpty()) {
                        if (releasesAdapter != null) {
                            releasesAdapter.notifyDataSetChanged();
                        }
                        if (shimmerContainer != null) {
                            shimmerContainer.stopShimmer();
                            shimmerContainer.setVisibility(View.GONE);
                        }
                        rvReleases.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        albumsLoadedListener = albums -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (albums != null && !albums.isEmpty()) {
                        if (albumAdapter != null) {
                            albumAdapter.notifyDataSetChanged();
                        }
                        if (shimmerAlbums != null) {
                            shimmerAlbums.stopShimmer();
                            shimmerAlbums.setVisibility(View.GONE);
                        }
                        rvAlbums.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        MyApplication.subscribe(songsLoadedListener);
        MyApplication.subscribeAlbums(albumsLoadedListener);

        if (MyApplication.allAlbums.isEmpty()) {
            loadMoreAlbums();
        } else {
            albumIndex = MyApplication.allAlbums.size();
        }

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
        
        tvViewAll.setOnClickListener((v)->{
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.ReleasesFragment);
        });

        if (btnIdentifyMusic != null) {
            btnIdentifyMusic.setOnClickListener(v -> {
                NavController navController = NavHostFragment.findNavController(this);
                navController.navigate(R.id.fragment_music_recognition);
            });
        }
    }

    private void loadMoreAlbums() {
        isLoadingAlbums = true;
        // YouTube doesn't have a direct "Top Albums" endpoint in the current handler.
        // We could search for popular music playlists or just skip for now.
        isLoadingAlbums = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (songsLoadedListener != null) {
            MyApplication.unsubscribe(songsLoadedListener);
        }
        if (userLoadedListener != null) {
            MyApplication.unsubscribeUser(userLoadedListener);
        }
        if (albumsLoadedListener != null) {
            MyApplication.unsubscribeAlbums(albumsLoadedListener);
        }
    }
}
