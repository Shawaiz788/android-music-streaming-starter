package com.example.musicplayer;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import kotlinx.serialization.descriptors.SerialKind;

public class RvReleasesAdapter extends RecyclerView.Adapter<RvReleasesAdapter.ReleaseViewHolder> {

Context context;
ArrayList<Song>songs;
    public RvReleasesAdapter(Context context, ArrayList<Song>list){
        this.songs=list;
        this.context=context;
    }

    @NonNull
    @Override
    public ReleaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.new_release_item,parent,false);
       view.setOnClickListener((v->{
           Toast.makeText(context,"Clicked",Toast.LENGTH_SHORT).show();
       }));
        return new ReleaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseViewHolder holder, int position) {
        Song song=songs.get(position);
       holder.ivRelease.setImageURI(Uri.parse(song.getImageUrl()));

    }

    @Override
    public int getItemCount() {
        return songs.size();
    }


    public class ReleaseViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRelease;

        public ReleaseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRelease = itemView.findViewById(R.id.ivRelease);

        }
    }


}
