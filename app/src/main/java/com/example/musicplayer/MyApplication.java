package com.example.musicplayer;

import android.app.Application;
import java.util.ArrayList;

public class MyApplication extends Application {
    public static  ArrayList<Song> songs;

    @Override
    public void onCreate() {
        super.onCreate();
        songs = new ArrayList<>();
        songs.add(new Song(
                "1",
                "RUDE",
                "AnimeVibe",
                "Chill",
                "Pop",
                "No lyrics available",
                206000,
                getResourceUri(R.raw.rude),
                getResourceUri(R.drawable.eternal_youth)
        ));
    }
    public String getResourceUri(int resId) {
        return "android.resource://" + getPackageName() + "/" + resId;
    }
}
