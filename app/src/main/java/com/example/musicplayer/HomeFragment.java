package com.example.musicplayer;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    ImageView ivPfp;
    TextView tvViewAll;
    CardView cvProfile;
    RecyclerView rvReleases;
    RvReleasesAdapter adapter;
    ShimmerFrameLayout shimmerContainer;
    MyApplication.OnSongsLoadedListener songsLoadedListener;

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
        ivPfp = view.findViewById(R.id.ivPfp);
        
        // Use Glide for reliable image loading (handles both URLs and local resources/placeholders)
        if (MyApplication.currentUserInfo != null && MyApplication.currentUserInfo.getProfileImageUrl() != null && !MyApplication.currentUserInfo.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                .load(MyApplication.currentUserInfo.getProfileImageUrl())
                .placeholder(R.drawable.icon_pfp)
                .error(R.drawable.icon_pfp)
                .into(ivPfp);
        }

        tvViewAll = view.findViewById(R.id.tvViewAll);
        cvProfile = view.findViewById(R.id.cvProfile);
        rvReleases = view.findViewById(R.id.rvReleases);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        rvReleases.setHasFixedSize(true);
        
        adapter = new RvReleasesAdapter(requireContext(), MyApplication.songs);
        rvReleases.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvReleases.setAdapter(adapter);

        songsLoadedListener = songs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (adapter != null && songs != null && !songs.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        if (shimmerContainer != null) {
                            shimmerContainer.stopShimmer();
                            shimmerContainer.setVisibility(View.GONE);
                        }
                        rvReleases.setVisibility(View.VISIBLE);
                    }
                });
            }
        };

        MyApplication.subscribe(songsLoadedListener);

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
        tvViewAll.setOnClickListener((v)->{
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.ReleasesFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (songsLoadedListener != null) {
            MyApplication.unsubscribe(songsLoadedListener);
        }
    }
}
