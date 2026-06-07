package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.Shimmer;
import com.facebook.shimmer.ShimmerDrawable;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    List<Song> songs;
    Context context;

    SearchAdapter(Context context, List<Song> songs) {
        this.songs = songs;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        
        String title = song.getTitle();
        if (song.getId() != null && song.getId().startsWith("youtube_")) {
            title =title;
        }
        holder.tvTitle.setText(title);
        holder.tvArtist.setText(song.getArtist());
        ShimmerDrawable shimmerDrawable = new ShimmerDrawable();
        shimmerDrawable.setShimmer(new Shimmer.ColorHighlightBuilder()
                .setBaseColor(0xFF555555)
                .setHighlightColor(0xFF888888)
                .setBaseAlpha(0.9f)
                .setHighlightAlpha(1f)
                .setDuration(1000)
                .build());

        Object imageSource = song.getImageUrl();
        DBManager dbManager = new DBManager(context);
        dbManager.Open();
        if (dbManager.isDownloaded(song.getId())) {
            String[] paths = dbManager.getSongPaths(song.getId());
            if (paths[1] != null && new java.io.File(paths[1]).exists()) {
                imageSource = new java.io.File(paths[1]);
            }
        }
        dbManager.Close();

        if (imageSource != null && (!(imageSource instanceof String) || !((String) imageSource).isEmpty())) {
            Glide.with(context)
                    .load(imageSource)
                    .placeholder(shimmerDrawable)
                    .error(R.drawable.error_song_cover)
                    .into(holder.ivSong);
        } else {
            holder.ivSong.setImageResource(android.R.color.darker_gray);
        }

        holder.itemView.setOnClickListener(v -> {
            if (MyApplication.recentSearchHandler != null) {
                MyApplication.recentSearchHandler.addRecentSearch(song);
            }

            if (context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(song, false, songs, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSong;
        TextView tvTitle, tvArtist;

        ViewHolder(View view) {
            super(view);
            ivSong = view.findViewById(R.id.ivSong);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvArtist = view.findViewById(R.id.tvArtist);
        }
    }
}
