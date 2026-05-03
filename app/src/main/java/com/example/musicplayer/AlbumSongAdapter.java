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
    private final boolean isAlbum;
    private String queueTitle;
    private OnOptionsClickListener onOptionsClickListener;

    public interface OnOptionsClickListener {
        void onOptionsClick(Song song, View view);
    }

    public AlbumSongAdapter(Context context, List<Song> songs, boolean isAlbum) {
        this.context = context;
        this.songs = songs;
        this.isAlbum = isAlbum;
    }

    public void setQueueTitle(String queueTitle) {
        this.queueTitle = queueTitle;
    }

    public void setOnOptionsClickListener(OnOptionsClickListener listener) {
        this.onOptionsClickListener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = isAlbum ? R.layout.item_song_album_no_dots : R.layout.item_song_album;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvSongArtist.setText(song.getArtist());

        if (song.getImageUrl() != null && !song.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(song.getImageUrl())
                    .placeholder(R.drawable.hungama)
                    .error(R.drawable.error_song_cover)
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

        holder.itemLayout.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(song, false, songs, position, queueTitle);
            }
        });

        if (holder.ivOptions != null) {
            holder.ivOptions.setOnClickListener(v -> {
                if (onOptionsClickListener != null) {
                    onOptionsClickListener.onOptionsClick(song, v);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvIndex, tvSongTitle, tvSongArtist, tvStatus;
        ImageView ivSongCover, ivOptions;
        View itemLayout;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView;
            tvIndex = itemView.findViewById(R.id.tvIndex);
            ivSongCover = itemView.findViewById(R.id.ivSongCover);
            tvSongTitle = itemView.findViewById(R.id.tvSongTitle);
            tvSongArtist = itemView.findViewById(R.id.tvSongArtist);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
