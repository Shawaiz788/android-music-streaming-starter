package com.example.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

public class DBManager {
    
    private static final String DB_NAME = "DownloadedMusic.db";
    private static final String TABLE_NAME = "downloaded_songs";
    private static final int DB_VERSION = 1; 

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_ARTIST = "artist";
    public static final String COLUMN_ALBUM = "album";
    public static final String COLUMN_GENRE = "genre";
    public static final String COLUMN_LYRICS = "lyrics";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_LOCAL_PATH = "local_path";
    public static final String COLUMN_COVER_PATH = "cover_path";
    public static final String COLUMN_TIMESTAMP = "download_timestamp";

    private DBHelper helper;
    private Context context;

    public DBManager(Context context){
        this.context = context;
    }

    public void Open(){
        helper = new DBHelper(context);
    }

    public void Close(){
        if (helper != null) {
            helper.close();
        }
    }

    public interface OnDownloadListener {
        void onDownloadComplete();
        void onDownloadFailed(Exception e);
    }

    public void AddSongs(Song song, OnDownloadListener listener) {
       //download the audio file
        DownloadUtils.getLocalPath(context, song, new DownloadUtils.DownloadCallback() {
            @Override
            public void onDownloadComplete(String localAudioPath) {
                //download cover image
                DownloadUtils.getCoverPath(context, song, new DownloadUtils.DownloadCallback() {
                    @Override
                    public void onDownloadComplete(String localCoverPath) {
                        //save to database
                        saveSongToDb(song, localAudioPath, localCoverPath);
                        if (listener != null) listener.onDownloadComplete();
                    }

                    @Override
                    public void onDownloadFailed(Exception e) {//cover save failed
                        saveSongToDb(song, localAudioPath, null);
                        if (listener != null) listener.onDownloadComplete();
                    }
                });
            }

            @Override
            public void onDownloadFailed(Exception e) {//audio save failed
                if (listener != null) listener.onDownloadFailed(e);
            }
        });
    }

    private void saveSongToDb(Song song, String audioPath, String coverPath) {
        SQLiteDatabase writeDB = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID, song.getId());
        cv.put(COLUMN_TITLE, song.getTitle());
        cv.put(COLUMN_ARTIST, song.getArtist());
        cv.put(COLUMN_ALBUM, song.getAlbum());
        cv.put(COLUMN_GENRE, song.getGenre());
        cv.put(COLUMN_LYRICS, song.getLyrics());
        cv.put(COLUMN_DURATION, song.getDuration());
        cv.put(COLUMN_LOCAL_PATH, audioPath);
        cv.put(COLUMN_COVER_PATH, coverPath);
        cv.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        // Use insertWithOnConflict to handle updates if the song is downloaded again
        writeDB.insertWithOnConflict(TABLE_NAME, null, cv, SQLiteDatabase.CONFLICT_REPLACE);

        // Update global list
        MyApplication.downloadedSongs.clear();
        MyApplication.downloadedSongs.addAll(getAllDownloadedSongs());
        MyApplication.notifyDownloadsLoaded();
    }

    public boolean isDownloaded(String songId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{songId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public String[] getSongPaths(String songId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String[] paths = new String[2]; // [audioPath, coverPath]
        String query = "SELECT " + COLUMN_LOCAL_PATH + ", " + COLUMN_COVER_PATH + " FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = ?";
        android.database.Cursor cursor = db.rawQuery(query, new String[]{songId});
        if (cursor.moveToFirst()) {
            paths[0] = cursor.getString(0);
            paths[1] = cursor.getString(1);
        }
        cursor.close();
        return paths;
    }

    public void deleteSong(String songId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{songId});

        // Update global list
        MyApplication.downloadedSongs.clear();
        MyApplication.downloadedSongs.addAll(getAllDownloadedSongs());
        MyApplication.notifyDownloadsLoaded();
    }

    public java.util.ArrayList<Song> getAllDownloadedSongs() {
        java.util.ArrayList<Song> songs = new java.util.ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        android.database.Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        if (cursor.moveToFirst()) {
            do {
                Song song = new Song();
                song.setId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                song.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)));
                song.setArtist(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ARTIST)));
                song.setAlbum(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ALBUM)));
                song.setGenre(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENRE)));
                song.setLyrics(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LYRICS)));
                song.setDuration(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)));
                song.setSongUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_PATH)));
                song.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVER_PATH)));
                songs.add(song);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return songs;
    }

    private class DBHelper extends SQLiteOpenHelper {
       public DBHelper(Context context){
           super(context, DB_NAME, null, DB_VERSION);
       }

       @Override
       public void onCreate(SQLiteDatabase db){
           String query = "CREATE TABLE " + TABLE_NAME + " ("
                   + COLUMN_ID + " TEXT PRIMARY KEY, "
                   + COLUMN_TITLE + " TEXT, "
                   + COLUMN_ARTIST + " TEXT, "
                   + COLUMN_ALBUM + " TEXT, "
                   + COLUMN_GENRE + " TEXT, "
                   + COLUMN_LYRICS + " TEXT, "
                   + COLUMN_DURATION + " INTEGER, "
                   + COLUMN_LOCAL_PATH + " TEXT, "
                   + COLUMN_COVER_PATH + " TEXT, "
                   + COLUMN_TIMESTAMP + " INTEGER);";
           db.execSQL(query);
       }

       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
           db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
           onCreate(db);
       }
    }
}
