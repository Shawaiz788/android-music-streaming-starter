package com.example.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Album implements Serializable {
    private String id;
    private String title;
    private String artist;
    private String imageUrl;
    private String year;
    private List<String> songIds;

    public Album() {
        this.songIds = new ArrayList<>();
    }

    public Album(String id, String title, String artist, String imageUrl, String year) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
        this.year = year;
        this.songIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }
}
