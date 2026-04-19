package com.example.musicplayer;

import java.io.Serializable;

public class Song implements Serializable { //implemented serialisable so ican pass the class as it is in intents etc
    private String id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String lyrics;
    private int duration; // in milliseconds
    private String songUrl;
    private String imageUrl;


    public Song() {}

    public Song(String id, String title, String artist, String album, String genre, String lyrics, int duration, String songUrl, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.lyrics = lyrics;
        this.duration = duration;
        this.songUrl = songUrl;
        this.imageUrl = imageUrl;
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public String getLyrics() { return lyrics; }
    public int getDuration() { return duration; }
    public String getSongUrl() { return songUrl; }
    public String getImageUrl() { return imageUrl; }


    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setArtist(String artist) { this.artist = artist; }
    public void setAlbum(String album) { this.album = album; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setSongUrl(String songUrl) { this.songUrl = songUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
