package com.example.musicplayer;

import static android.app.PendingIntent.getActivity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.MainActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.Song;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
     List<Song> songs;
     Context context;

    SearchAdapter(Context context, List<Song> songs) {
        this.songs = songs;
        this.context=context;
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
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        if (song.getImageUrl() != null) {
            Glide.with(holder.itemView.getContext())
                    .load(song.getImageUrl())
                    .placeholder(android.R.color.darker_gray)
                    .into(holder.ivSong);
        }

        holder.itemView.setOnClickListener(v -> {
            if (context instanceof MainActivity) {
                ((MainActivity) context).showPlayerDialog(song, false);
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