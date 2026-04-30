package com.example.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Artist implements Serializable {
    private String id;
    private String name;
    private String imageUrl;
    private List<String> albumIds;
    private List<String> songIds;

    public Artist() {
        this.albumIds = new ArrayList<>();
        this.songIds = new ArrayList<>();
    }

    public Artist(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.albumIds = new ArrayList<>();
        this.songIds = new ArrayList<>();
    }

    public Artist(String id, String name, String imageUrl, List<String> albumIds, List<String> songIds) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.albumIds = albumIds != null ? albumIds : new ArrayList<>();
        this.songIds = songIds != null ? songIds : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getAlbumIds() { return albumIds; }
    public void setAlbumIds(List<String> albumIds) { this.albumIds = albumIds; }
    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }
}
