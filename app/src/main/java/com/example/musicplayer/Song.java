package com.example.musicplayer;

import java.io.Serializable;

public class Song implements Serializable {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private String lyrics;
    private int duration;
    private String songUrl;
    private String imageUrl;
    
    // Relationship IDs for efficient filtering
    private String albumId;
    private String artistId;

    public Song() {}

    public Song(String id, String title, String artist, String album, String genre, String lyrics, int duration, String songUrl, String imageUrl, String albumId, String artistId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.lyrics = lyrics;
        this.duration = duration;
        this.songUrl = songUrl;
        this.imageUrl = imageUrl;
        this.albumId = albumId;
        this.artistId = artistId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getSongUrl() { return songUrl; }
    public void setSongUrl(String songUrl) { this.songUrl = songUrl; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getAlbumId() { return albumId; }
    public void setAlbumId(String albumId) { this.albumId = albumId; }
    public String getArtistId() { return artistId; }
    public void setArtistId(String artistId) { this.artistId = artistId; }
}
