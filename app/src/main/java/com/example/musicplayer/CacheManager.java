package com.example.musicplayer;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class CacheManager {
    private static final String CACHE_DIR = "data_cache";
    private static final String NEW_RELEASES_FILE = "new_releases.json";
    private static final String ALL_ALBUMS_FILE = "all_albums.json";
    private static final String TOP_SONGS_FILE = "top_songs.json";
    private static final String EXPLORE_SONGS_FILE = "explore_songs.json";
    private static final String PLAY_COUNTS_FILE = "play_counts.json";

    private final Context context;
    private final Gson gson;

    public CacheManager(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    private File getCacheFile(String fileName) {
        File dir = new File(context.getFilesDir(), CACHE_DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, fileName);
    }

    public <T> void saveToCache(String fileName, List<T> data) {
        new Thread(() -> {
            try (FileWriter writer = new FileWriter(getCacheFile(fileName))) {
                gson.toJson(data, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public <T> ArrayList<T> loadFromCache(String fileName, Type type) {
        File file = getCacheFile(fileName);
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveNewReleases(List<Song> songs) {
        saveToCache(NEW_RELEASES_FILE, songs);
    }

    public ArrayList<Song> loadNewReleases() {
        Type type = new TypeToken<ArrayList<Song>>() {}.getType();
        return loadFromCache(NEW_RELEASES_FILE, type);
    }

    public void saveAllAlbums(List<Album> albums) {
        saveToCache(ALL_ALBUMS_FILE, albums);
    }

    public ArrayList<Album> loadAllAlbums() {
        Type type = new TypeToken<ArrayList<Album>>() {}.getType();
        return loadFromCache(ALL_ALBUMS_FILE, type);
    }

    public void saveTopSongs(List<Song> songs) {
        saveToCache(TOP_SONGS_FILE, songs);
    }

    public ArrayList<Song> loadTopSongs() {
        Type type = new TypeToken<ArrayList<Song>>() {}.getType();
        return loadFromCache(TOP_SONGS_FILE, type);
    }

    public void saveExploreSongs(List<Song> songs) {
        saveToCache(EXPLORE_SONGS_FILE, songs);
    }

    public ArrayList<Song> loadExploreSongs() {
        Type type = new TypeToken<ArrayList<Song>>() {}.getType();
        return loadFromCache(EXPLORE_SONGS_FILE, type);
    }

    public void savePlayCounts(Map<String, Integer> playCounts) {
        new Thread(() -> {
            try (FileWriter writer = new FileWriter(getCacheFile(PLAY_COUNTS_FILE))) {
                gson.toJson(playCounts, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public Map<String, Integer> loadPlayCounts() {
        File file = getCacheFile(PLAY_COUNTS_FILE);
        if (!file.exists()) return new java.util.HashMap<>();

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
            return new java.util.HashMap<>();
        }
    }
}
