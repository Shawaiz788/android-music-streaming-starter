package com.example.musicplayer;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    RecyclerView rvSearch;
    View defaultContent;
    View searchResultContent;
    TextView tvSearchHeader;
    SearchAdapter adapter;
    List<Song> displayList = new ArrayList<>();
    CardView cvProfile;

    public SearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cvProfile=view.findViewById(R.id.cvProfile);
        EditText etSearch = view.findViewById(R.id.etSearch);
        rvSearch = view.findViewById(R.id.rvSearch);
        defaultContent = view.findViewById(R.id.defaultContent);
        searchResultContent = view.findViewById(R.id.searchResultContent);
        tvSearchHeader = view.findViewById(R.id.tvSearchHeader);

        adapter = new SearchAdapter(requireContext(), displayList);
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setAdapter(adapter);

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
                    if (query.trim().isEmpty()) {
                        tvSearchHeader.setText("Recent searches");
                    } else {
                        tvSearchHeader.setText("Search results");
                    }
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
        cvProfile.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.profileFragment);
        });
    }

    private void showSearchResults() {
        defaultContent.setVisibility(GONE);
        searchResultContent.setVisibility(VISIBLE);
        // Initially show all songs or recent searches
        if (displayList.isEmpty()) {
            performSearch("");
        }
    }

    private void performSearch(String query) {
        List<Song> results = MyApplication.searchSongs(query);
        displayList.clear();
        displayList.addAll(results);
        adapter.notifyDataSetChanged();
    }
}
