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


public class HomeFragment extends Fragment {


    CardView cvProfile;
    RecyclerView rvReleases;
    RvReleasesAdapter adapter;



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
        rvReleases.setHasFixedSize(true);
        //ArrayList<Song>list=new ArrayList<>();
        //the array here should be an array that gets only the latest songs based on the date added
        //or atleast an array thats sorted based on the date added
        adapter=new RvReleasesAdapter(requireContext(),MyApplication.songs);
        rvReleases.setLayoutManager(new LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false));
        rvReleases.setAdapter(adapter);
        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
    }
}