package com.example.musicplayer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Playlist implements Serializable {
    private String id;
    private String title;
    private int trackCount;
    private String duration;
    private List<String> songIds;

    public Playlist() {
        this.songIds = new ArrayList<>();
    }

    public Playlist(String id, String title, int trackCount, String duration) {
        this.id = id;
        this.title = title;
        this.trackCount = trackCount;
        this.duration = duration;
        this.songIds = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getTrackCount() { return trackCount; }
    public void setTrackCount(int trackCount) { this.trackCount = trackCount; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public List<String> getSongIds() { return songIds; }
    public void setSongIds(List<String> songIds) { this.songIds = songIds; }

    public void calculateAndSetDuration(List<Song> allSongs) {
        long totalMillis = 0;
        if (songIds != null && allSongs != null) {
            for (String songId : songIds) {
                for (Song song : allSongs) {
                    if (song.getId() != null && song.getId().equals(songId)) {
                        totalMillis += song.getDuration();
                        break;
                    }
                }
            }
        }
        this.duration = formatDuration(totalMillis);
        this.trackCount = (songIds != null) ? songIds.size() : 0;
    }

    private String formatDuration(long totalMillis) {
        long seconds = totalMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + " hr " + (minutes % 60) + " min";
        } else if (minutes > 0) {
            return minutes + " min";
        } else {
            return seconds + " sec";
        }
    }
}
