package com.example.musicplayer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FavoritesFragment extends Fragment {

    LinearLayout LLTracks, LLFYNF, LLAlbums, LLPlaylists, LLDownload;
    CardView cvProfile, cvTopPlaylist, cvTopPlaylistPlay;
    ImageView ivPfp;
    TextView tvTopPlaylistTitle, tvTopPlaylistTracks;
    MyApplication.OnPlaylistsLoadedListener playlistListener;
    MyApplication.OnUserLoadedListener userLoadedListener;

    private final int[] colors = {
            Color.parseColor("#4CAF50"), Color.parseColor("#F44336"),
            Color.parseColor("#2196F3"), Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"), Color.parseColor("#00BCD4"),
            Color.parseColor("#E91E63"), Color.parseColor("#673AB7"),
            Color.parseColor("#FF5722"), Color.parseColor("#3F51B5")
    };

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        LLTracks = view.findViewById(R.id.LLTracks);
        LLAlbums = view.findViewById(R.id.LLAlbums);
        LLPlaylists = view.findViewById(R.id.LLPlaylists);
        LLDownload = view.findViewById(R.id.LLDownload);
        cvProfile = view.findViewById(R.id.cvProfile);
        ivPfp = view.findViewById(R.id.ivPfp);
        LLFYNF=view.findViewById(R.id.LLFYNF);
        
        cvTopPlaylist = view.findViewById(R.id.cvTopPlaylist);
        cvTopPlaylistPlay = view.findViewById(R.id.cvTopPlaylistPlay);
        tvTopPlaylistTitle = view.findViewById(R.id.tvTopPlaylistTitle);
        tvTopPlaylistTracks = view.findViewById(R.id.tvTopPlaylistTracks);

        // Subscribe to playlist updates
        playlistListener = playlists -> {
            if (isAdded()) {
                getActivity().runOnUiThread(() -> updateTopPlaylistUI(playlists));
            }
        };
        MyApplication.subscribePlaylists(playlistListener);
        

        updateTopPlaylistUI(MyApplication.favouritePlaylists);

        // Click listeners
        LLTracks.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.favouriteTracksFragment);
        });

        LLAlbums.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.FavouriteAlbumsFragment);
        });

        LLPlaylists.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.favouritePlaylistFragment);
        });

        LLDownload.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.downloadsFragment);
        });
        LLFYNF.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.fragment_music_recognition);
        });

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });

        userLoadedListener = user -> {
            if (isAdded() && user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && ivPfp != null) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.icon_pfp)
                        .error(R.drawable.icon_pfp)
                        .into(ivPfp);
            }
        };
        MyApplication.subscribeUser(userLoadedListener);
    }

    private void updateTopPlaylistUI(ArrayList<Playlist> playlists) {
        if (playlists != null && !playlists.isEmpty()) {
            Playlist topPlaylist = playlists.get(0);
            tvTopPlaylistTitle.setText(topPlaylist.getTitle());
            tvTopPlaylistTracks.setText(topPlaylist.getSongIds().size() + " tracks");
            
            // Replicate the color logic for consistency
            Random random = new Random(topPlaylist.getId().hashCode() ^ MyApplication.sessionSeed);
            int bgIndex = random.nextInt(colors.length);
            int circleIndex = (bgIndex + (colors.length / 2)) % colors.length;
            
            int finalBgColor = colors[bgIndex];
            int finalCircleColor = colors[circleIndex];
            
            cvTopPlaylist.setCardBackgroundColor(finalBgColor);
            
            cvTopPlaylist.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putSerializable("playlist", topPlaylist);
                bundle.putInt("bgColor", finalBgColor);
                bundle.putInt("circleColor", finalCircleColor);
                NavHostFragment.findNavController(this).navigate(R.id.playlistDetailsFragment, bundle);
            });

            cvTopPlaylistPlay.setOnClickListener(v -> {
                List<Song> playlistSongs = new ArrayList<>();
                for (String id : topPlaylist.getSongIds()) {
                    for (Song s : MyApplication.songs) {
                        if (s.getId().equals(id)) {
                            playlistSongs.add(s);
                            break;
                        }
                    }
                }
                if (!playlistSongs.isEmpty() && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showPlayerDialog(playlistSongs.get(0), false, playlistSongs, 0);
                }
            });

            cvTopPlaylist.setVisibility(View.VISIBLE);
        } else {
            cvTopPlaylist.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (playlistListener != null) {
            MyApplication.unsubscribePlaylists(playlistListener);
        }
        if (userLoadedListener != null) {
            MyApplication.unsubscribeUser(userLoadedListener);
        }
    }
}
