package com.example.musicplayer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private final Context context;
    private final List<Album> albums;

    public AlbumAdapter(Context context, List<Album> albums) {
        this.context = context;
        this.albums = albums;
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.tvAlbumTitle.setText(album.getTitle());
        holder.tvArtistName.setText(album.getArtist());
        String year;
        if(album.getYear()!=null){
            year=album.getYear();
        }else{
            year="Unknown Year";
        }
        holder.tvYear.setText(year);

        if (album.getImageUrl() != null && !album.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(album.getImageUrl())
                    .placeholder(R.drawable.hungama)
                    .into(holder.ivAlbumArt);
        } else {
            holder.ivAlbumArt.setImageResource(R.drawable.hungama);
        }

        holder.itemLayout.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("album", album);
            Navigation.findNavController(v).navigate(R.id.albumDetailsFragment, bundle);
        });
    }

    @Override
    public int getItemCount() {
        if(albums!=null){
            return albums.size();
        }else{
            return 0;
        }
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt, ivOptions;
        TextView tvAlbumTitle, tvArtistName, tvYear;
        View itemLayout; // Layout hook

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            itemLayout = itemView; // Root layout of item_album.xml
            ivAlbumArt = itemView.findViewById(R.id.ivAlbumArt);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvYear = itemView.findViewById(R.id.tvYear);
            ivOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
