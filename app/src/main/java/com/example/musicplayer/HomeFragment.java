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
    ShimmerFrameLayout shimmerContainer;
    MyApplication.OnSongsLoadedListener songsLoadedListener;
    MyApplication.OnUserLoadedListener userLoadedListener;
    MyApplication.OnAlbumsLoadedListener albumsLoadedListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

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

        rvReleases.setVisibility(View.GONE);
        if (shimmerContainer != null) {
            shimmerContainer.startShimmer();
        }
        
        // Setup New Releases Adapter
        releasesAdapter = new RvReleasesAdapter(requireContext(), MyApplication.newReleases);
        rvReleases.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvReleases.setAdapter(releasesAdapter);

        // Setup Albums Adapter (Grid Layout span 2)
        albumAdapter = new AlbumAdapter(requireContext(), MyApplication.allAlbums);
        rvAlbums.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvAlbums.setAdapter(albumAdapter);
        rvAlbums.setNestedScrollingEnabled(false);

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
                    if (albumAdapter != null) {
                        albumAdapter.notifyDataSetChanged();
                    }
                });
            }
        };

        MyApplication.subscribe(songsLoadedListener);
        MyApplication.subscribeAlbums(albumsLoadedListener);

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
