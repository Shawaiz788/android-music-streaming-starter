package com.example.musicplayer;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import com.facebook.shimmer.ShimmerFrameLayout;


public class HomeFragment extends Fragment {


    CardView cvProfile;
    RecyclerView rvReleases;
    RvReleasesAdapter adapter;
    ShimmerFrameLayout shimmerContainer;
    private MyApplication.OnSongsLoadedListener songsLoadedListener;



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
        cvProfile = view.findViewById(R.id.cvProfile);
        rvReleases=view.findViewById(R.id.rvReleases);
        shimmerContainer = view.findViewById(R.id.shimmer_view_container);
        rvReleases.setHasFixedSize(true);
        
        adapter=new RvReleasesAdapter(requireContext(),MyApplication.songs);
        rvReleases.setLayoutManager(new LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false));
        rvReleases.setAdapter(adapter);

        songsLoadedListener = songs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (adapter != null && songs != null && !songs.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        rvReleases.setVisibility(View.VISIBLE);
                        shimmerContainer.stopShimmer();
                        shimmerContainer.setVisibility(View.GONE);
                    }
                });
            }
        };
        MyApplication.subscribe(songsLoadedListener);

        // Check if already loaded
        if (MyApplication.songs != null && !MyApplication.songs.isEmpty()) {
            rvReleases.setVisibility(View.VISIBLE);
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        } else {
            shimmerContainer.startShimmer();
        }

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
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