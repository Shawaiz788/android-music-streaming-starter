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

    public ReleasesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btn_back=view.findViewById(R.id.btn_back);
        btn_back.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(ReleasesFragment.this);
            navController.navigate(R.id.homeFragment);
        });
        rvReleases=view.findViewById(R.id.rvReleases);
        adapter=new RvReleasesAdapter(requireContext(),MyApplication.songs);
        rvReleases.setLayoutManager(new GridLayoutManager(requireContext(),2,GridLayoutManager.VERTICAL,false));
        rvReleases.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_releases, container, false);
    }
}