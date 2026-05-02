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

public class AlbumSongAdapter extends RecyclerView.Adapter<AlbumSongAdapter.SongViewHolder> {

    private final Context context;
    private final List<Song> songs;
    private OnOptionsClickListener onOptionsClickListener;

    public interface OnOptionsClickListener {
        void onOptionsClick(Song song, View view);
    }

    public AlbumSongAdapter(Context context, List<Song> songs) {
        this.context = context;
        this.songs = songs;
    }

    public void setOnOptionsClickListener(OnOptionsClickListener listener) {
        this.onOptionsClickListener = listener;
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

        // Load song cover image
        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.hungama)
                    .error(R.drawable.error_song_cover)
                    .into(holder.ivSongCover);
        } else {
            holder.ivSongCover.setImageResource(R.drawable.hungama);
        }

        // Show "NOW" if the song is currently playing
        if (PlayerManager.getInstance().getCurrentSong() != null &&
                PlayerManager.getInstance().getCurrentSong().getId().equals(song.getId())) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("NOW");
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // Using the layout hook to set the click listener in bind
        holder.itemLayout.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(song, false, songs, position);
            }
        });

        holder.ivOptions.setOnClickListener(v -> {
            if (onOptionsClickListener != null) {
                onOptionsClickListener.onOptionsClick(song, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvSongTitle, tvSongArtist, tvStatus;
        ImageView ivSongCover, ivOptions;
        View itemLayout; // Layout hook

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView; // Root layout of item_song_album.xml
            tvIndex = itemView.findViewById(R.id.tvIndex);
            ivSongCover = itemView.findViewById(R.id.ivSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
