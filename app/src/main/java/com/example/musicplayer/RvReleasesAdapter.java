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

import com.bumptech.glide.Glide;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerDrawable;

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

        return new ReleaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseViewHolder holder, int position) {
        ShimmerDrawable shimmerDrawable = new ShimmerDrawable();
        shimmerDrawable.setShimmer(new Shimmer.ColorHighlightBuilder()
                .setBaseColor(0xFF555555)
                .setHighlightColor(0xFF888888)
                .setBaseAlpha(0.9f)
                .setHighlightAlpha(1f)
                .setDuration(1000)
                .build());
        Song song = songs.get(position);
        
        // Use Glide for reliable image loading (Firebase URLs + Local Resources)
        if (song.getImageUrl() != null) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(shimmerDrawable)
                    .error(R.drawable.error_song_cover)
                    .into(holder.ivRelease);
        }

        holder.ivRelease.setOnClickListener((v->{
            if (MyApplication.songs != null && !MyApplication.songs.isEmpty()) {
                ((MainActivity) context).showPlayerDialog(song, false);
            }
        }));
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
