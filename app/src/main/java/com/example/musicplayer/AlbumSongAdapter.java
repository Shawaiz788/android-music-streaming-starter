package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlbumSongAdapter extends RecyclerView.Adapter<AlbumSongAdapter.SongViewHolder> {

    private final Context context;
    private final List<Song> songs;

    public AlbumSongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_album, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvSongArtist.setText(song.getArtist());

        // Using the layout hook to set the click listener in bind
        holder.itemLayout.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(song, false);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvSongTitle, tvSongArtist, tvStatus;
        View itemLayout; // Layout hook

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView; // Root layout of item_song_album.xml
            tvIndex = itemView.findViewById(R.id.tvIndex);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
