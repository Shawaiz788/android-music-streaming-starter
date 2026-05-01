package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
    
    private final int[] darkColors = {
            Color.parseColor("#1B5E20"),
            Color.parseColor("#B71C1C"),
            Color.parseColor("#0D47A1"),
            Color.parseColor("#E65100"),
            Color.parseColor("#4A148C"),
            Color.parseColor("#006064"),
            Color.parseColor("#880E4F"),
            Color.parseColor("#311B92"),
            Color.parseColor("#BF360C"),
            Color.parseColor("#1A237E")
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
        holder.tvInfo.setText(playlist.getSongIds().size() + " tracks • " + playlist.getDuration());

        Random random = new Random(playlist.getId().hashCode() ^ sessionSeed);
        
        int bgIndex = random.nextInt(colors.length);
        
        int circleIndex;
        if (colors.length > 1) {
            circleIndex = random.nextInt(colors.length);
            if (circleIndex == bgIndex) {
                circleIndex = (circleIndex + 1) % colors.length;
            }
        } else {
            circleIndex = 0;
        }
        
        holder.layoutBackground.setBackgroundColor(colors[bgIndex]);
        
        if (holder.viewCircle.getBackground() != null) {
            holder.viewCircle.getBackground().mutate().setTint(darkColors[circleIndex]);
        }

        holder.itemLayout.setOnClickListener(v -> {
            // Handle playlist click
        });
    }

    @Override
    public int getItemCount() {
        return playlists.size();
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvInfo;
        View layoutBackground, viewCircle, itemLayout;
        ImageView btnSave;

        public PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView;
            tvTitle = itemView.findViewById(R.id.tvPlaylistTitle);
            tvInfo = itemView.findViewById(R.id.tvPlaylistInfo);
            layoutBackground = itemView.findViewById(R.id.layoutBackground);
            viewCircle = itemView.findViewById(R.id.viewCircle);
            btnSave = itemView.findViewById(R.id.btnSave);
        }
    }
}
