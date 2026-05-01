package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Random;

public class PlaylistSmallAdapter extends RecyclerView.Adapter<PlaylistSmallAdapter.ViewHolder> {

    private final Context context;
    private final List<Playlist> playlists;
    private final OnPlaylistSelectedListener listener;

    private final int[] colors = {
            Color.parseColor("#4CAF50"), Color.parseColor("#F44336"),
            Color.parseColor("#2196F3"), Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4")
    };

    public interface OnPlaylistSelectedListener {
        void onSelected(Playlist playlist);
    }

    public PlaylistSmallAdapter(Context context, List<Playlist> playlists, OnPlaylistSelectedListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist_small, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);
        holder.tvTitle.setText(playlist.getTitle());
        holder.tvInfo.setText(playlist.getSongIds().size() + " tracks");

        Random random = new Random(playlist.getId().hashCode());
        holder.viewColor.getBackground().setTint(colors[random.nextInt(colors.length)]);

        holder.itemLayout.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(playlist);
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvInfo;
        View viewColor, itemLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView;
            tvTitle = itemView.findViewById(R.id.tv_playlist_title);
            tvInfo = itemView.findViewById(R.id.tv_playlist_info);
            viewColor = itemView.findViewById(R.id.view_playlist_color);
        }
    }
}
