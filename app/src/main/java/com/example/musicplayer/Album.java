package com.example.musicplayer;

import java.io.Serializable;

public class Album implements Serializable {
    private String id;
    private String title;
    private String artist;
    private String imageUrl;

    public Album() {}

    public Album(String id, String title, String artist, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
