package com.example.musicplayer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.musicplayer.Album;
import com.example.musicplayer.Song;
import com.example.musicplayer.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SearchFragment extends Fragment {

    RecyclerView rvSearch, rvExplore;
    View defaultContent;
    View searchResultContent;
    TextView tvSearchHeader, tvClearAll;
    SearchAdapter adapter;
    ExploreAdapter exploreAdapter;
    List<Song> displayList = new ArrayList<>();
    List<Song> exploreList = new ArrayList<>();
    CardView cvProfile;
    ImageView ivPfp;
    EditText etSearch;
    
    CardView cvAlbums, cvPlaylists, cvMadeForYou, cvNewReleases;
    ImageView ivAlbumBg, ivMadeForYouBg, ivNewReleasesBg;
    View layoutPlaylistBg, viewPlaylistCircle;
    
    private String currentQuery = "";

    private MyApplication.OnAlbumsLoadedListener albumsLoadedListener;
    private MyApplication.OnSongsLoadedListener songsLoadedListener;
    private MyApplication.OnFavouriteSongsLoadedListener favouriteSongsLoadedListener;
    private MyApplication.OnUserLoadedListener userLoadedListener;
    
    private final int[] playlistColors = {
            Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FF9800"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#673AB7"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#3F51B5")
    };

    private static SearchFragment instance;

    public SearchFragment() {
    }

    public static void refreshRecentSearches() {
        if (instance != null && instance.currentQuery.isEmpty()) {
            instance.displayList.clear();
            instance.displayList.addAll(MyApplication.recentSearches);
            if (instance.adapter != null) {
                instance.adapter.notifyDataSetChanged();
            }
            instance.updateClearAllVisibility();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        instance = this;
        
        cvProfile = view.findViewById(R.id.cvProfile);
        ivPfp = view.findViewById(R.id.ivPfp);
        etSearch = view.findViewById(R.id.etSearch);
        rvSearch = view.findViewById(R.id.rvSearch);
        rvExplore = view.findViewById(R.id.rvExplore);
        defaultContent = view.findViewById(R.id.defaultContent);
        searchResultContent = view.findViewById(R.id.searchResultContent);
        tvSearchHeader = view.findViewById(R.id.tvSearchHeader);
        tvClearAll = view.findViewById(R.id.tvClearAll);

        cvAlbums = view.findViewById(R.id.cvAlbums);
        cvPlaylists = view.findViewById(R.id.cvPlaylists);
        cvMadeForYou = view.findViewById(R.id.cvMadeForYou);
        cvNewReleases = view.findViewById(R.id.cvNewReleases);

        ivAlbumBg = view.findViewById(R.id.ivAlbumBg);
        ivMadeForYouBg = view.findViewById(R.id.ivMadeForYouBg);
        ivNewReleasesBg = view.findViewById(R.id.ivNewReleasesBg);
        layoutPlaylistBg = view.findViewById(R.id.layoutPlaylistBg);
        viewPlaylistCircle = view.findViewById(R.id.viewPlaylistCircle);

        setupCategoryCards();
        setupExploreSection();

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

        adapter = new SearchAdapter(requireContext(), displayList);
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setAdapter(adapter);

        String initialQuery = null;
        if (getArguments() != null) {
            initialQuery = getArguments().getString("initialQuery");
        }

        if (initialQuery != null && !initialQuery.isEmpty()) {
            etSearch.setText(initialQuery);
            showSearchResults();
            performSearch(initialQuery);
        } else if (currentQuery.isEmpty()) {
            displayList.clear();
            displayList.addAll(MyApplication.recentSearches);
            adapter.notifyDataSetChanged();
            updateClearAllVisibility();
        }

        if (etSearch != null) {
            etSearch.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    showSearchResults();
                }
            });

            etSearch.setOnClickListener(v -> showSearchResults());

            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString();
                    currentQuery = query;
                    if (query.trim().isEmpty()) {
                        tvSearchHeader.setText(getString(R.string.recent_searches));
                    } else {
                        tvSearchHeader.setText(getString(R.string.search_results));
                    }
                    updateClearAllVisibility();
                    performSearch(query);
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            etSearch.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(etSearch.getText().toString());
                    return true;
                }
                return false;
            });
        }
        
        tvClearAll.setOnClickListener(v -> {
            if (MyApplication.recentSearchHandler != null) {
                MyApplication.recentSearchHandler.clearRecentSearches();
            }
        });

        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });

        cvAlbums.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.FavouriteAlbumsFragment);
        });

        cvPlaylists.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.favouritePlaylistFragment);
        });

        cvMadeForYou.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.favouriteTracksFragment);
        });

        cvNewReleases.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.ReleasesFragment);
        });
    }

    private void updateClearAllVisibility() {
        if (tvClearAll == null) return;
        if (currentQuery.trim().isEmpty() && !MyApplication.recentSearches.isEmpty()) {
            tvClearAll.setVisibility(VISIBLE);
        } else {
            tvClearAll.setVisibility(GONE);
        }
    }

    private void setupCategoryCards() {
        Random random = new Random(MyApplication.sessionSeed + 123);
        int bgIndex = random.nextInt(playlistColors.length);
        int circleIndex = (bgIndex + (playlistColors.length / 2)) % playlistColors.length;
        layoutPlaylistBg.setBackgroundColor(playlistColors[bgIndex]);
        if (viewPlaylistCircle.getBackground() != null) {
            viewPlaylistCircle.getBackground().mutate().setTint(playlistColors[circleIndex]);
        }

        albumsLoadedListener = albums -> {
            if (isAdded() && albums != null && !albums.isEmpty() && ivAlbumBg != null) {
                Album randomAlbum = albums.get(new Random().nextInt(albums.size()));
                Glide.with(this).load(randomAlbum.getImageUrl()).into(ivAlbumBg);
            }
        };
        MyApplication.subscribeAlbums(albumsLoadedListener);

        songsLoadedListener = songs -> {
            if (isAdded() && songs != null && !songs.isEmpty() && ivNewReleasesBg != null) {
                Song randomNew = songs.get(new Random().nextInt(songs.size()));
                Glide.with(this).load(randomNew.getImageUrl()).into(ivNewReleasesBg);
            }
            setupExploreSection();
        };
        MyApplication.subscribe(songsLoadedListener);

        favouriteSongsLoadedListener = favs -> {
            if (isAdded() && favs != null && !favs.isEmpty() && ivMadeForYouBg != null) {
                Song randomFav = favs.get(new Random().nextInt(favs.size()));
                Glide.with(this).load(randomFav.getImageUrl()).into(ivMadeForYouBg);
            }
        };
        MyApplication.subscribeFavouriteSongs(favouriteSongsLoadedListener);
    }

    private void setupExploreSection() {
        if (!isAdded()) return;

        exploreList.clear();
        ArrayList<Song> allSongs = new ArrayList<>(MyApplication.songs);
        if (allSongs.size() > 0) {
            Collections.shuffle(allSongs);
            int count = Math.min(allSongs.size(), 20);
            for (int i = 0; i < count; i++) {
                exploreList.add(allSongs.get(i));
            }
        }

        if (exploreAdapter == null) {
            exploreAdapter = new ExploreAdapter(requireContext(), exploreList);
            rvExplore.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            rvExplore.setAdapter(exploreAdapter);
        } else {
            exploreAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (albumsLoadedListener != null) {
            MyApplication.unsubscribeAlbums(albumsLoadedListener);
        }
        if (songsLoadedListener != null) {
            MyApplication.unsubscribe(songsLoadedListener);
        }
        if (favouriteSongsLoadedListener != null) {
            MyApplication.unsubscribeFavouriteSongs(favouriteSongsLoadedListener);
        }
        if (userLoadedListener != null) {
            MyApplication.unsubscribeUser(userLoadedListener);
        }
        instance = null;
    }

    private void showSearchResults() {
        defaultContent.setVisibility(GONE);
        searchResultContent.setVisibility(VISIBLE);
        if (currentQuery.isEmpty()) {
            performSearch("");
        }
    }

    private void performSearch(String query) {
        this.currentQuery = query;
        List<Song> results = MyApplication.searchSongs(query);
        displayList.clear();
        displayList.addAll(results);
        adapter.notifyDataSetChanged();
        updateClearAllVisibility();
    }
}
