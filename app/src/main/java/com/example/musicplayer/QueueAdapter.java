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

import java.util.List;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final Context context;
    private final List<Song> songs;
    private final OnSongClickListener onSongClickListener;

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public QueueAdapter(Context context, List<Song> songs, OnSongClickListener listener) {
        this.context = context;
        this.songs = songs;
        this.onSongClickListener = listener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_album, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvSongArtist.setText(song.getArtist());

        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.hungama)
                    .into(holder.ivSongCover);
        } else {
            holder.ivSongCover.setImageResource(R.drawable.hungama);
        }

        if (PlayerManager.getInstance().getCurrentSong() != null &&
                PlayerManager.getInstance().getCurrentSong().getId().equals(song.getId())) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("NOW");
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (onSongClickListener != null) {
                onSongClickListener.onSongClick(song, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvSongTitle, tvSongArtist, tvStatus;
        ImageView ivSongCover, ivOptions;

        public QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tvIndex);
            ivSongCover = itemView.findViewById(R.id.ivSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivOptions = itemView.findViewById(R.id.ivOptions);
            ivOptions.setVisibility(View.GONE); // Hide options in queue list for now
        }
    }
}
