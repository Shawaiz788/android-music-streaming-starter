package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private final List<Playlist> playlists;
    private final long sessionSeed; // Unique seed for this app session
    
    private final int[] colors = {
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#00BCD4"), // Cyan
            Color.parseColor("#E91E63"), // Pink
            Color.parseColor("#673AB7"), // Deep Purple
            Color.parseColor("#FF5722"), // Deep Orange
            Color.parseColor("#3F51B5")  // Indigo
    };

    public PlaylistAdapter(Context context, List<Playlist> playlists) {
        this.context = context;
        this.playlists = playlists;
        this.sessionSeed = new Random().nextLong(); // Generate once per app open
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvTitle.setText(playlist.getTitle());
        holder.tvInfo.setText(playlist.getSongIds().size() + " tracks • " + (playlist.getDuration() != null ? playlist.getDuration() : "0 min"));

        Random random = new Random(playlist.getId().hashCode() ^ sessionSeed);
        int bgIndex = random.nextInt(colors.length);
        int circleIndex = (bgIndex + (colors.length / 2)) % colors.length;
        int finalBgColor = colors[bgIndex];
        int finalCircleColor = colors[circleIndex];

        holder.layoutBackground.setBackgroundColor(finalBgColor);
        
        if (holder.viewCircle.getBackground() != null) {
            holder.viewCircle.getBackground().mutate().setTint(finalCircleColor);
            holder.viewCircle.setAlpha(0.5f);
        }

        holder.itemLayout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("playlist", playlist);
            bundle.putInt("bgColor", finalBgColor);
            bundle.putInt("circleColor", finalCircleColor);
            Navigation.findNavController(v).navigate(R.id.playlistDetailsFragment, bundle);
        });

        holder.btnPlay.setOnClickListener(v -> {
            List<Song> playlistSongs = new ArrayList<>();
            for (String id : playlist.getSongIds()) {
                for (Song s : MyApplication.songs) {
                    if (s.getId().equals(id)) {
                        playlistSongs.add(s);
                        break;
                    }
                }
            }
            if (!playlistSongs.isEmpty() && context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(playlistSongs.get(0), false, playlistSongs, 0, playlist.getTitle());
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvInfo;
        View layoutBackground, viewCircle, itemLayout;
        View btnPlay;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView;
            tvTitle = itemView.findViewById(R.id.tvPlaylistTitle);
            tvInfo = itemView.findViewById(R.id.tvPlaylistInfo);
            layoutBackground = itemView.findViewById(R.id.layoutBackground);
            viewCircle = itemView.findViewById(R.id.viewCircle);
            btnPlay = itemView.findViewById(R.id.btnPlay);
        }
    }
}
