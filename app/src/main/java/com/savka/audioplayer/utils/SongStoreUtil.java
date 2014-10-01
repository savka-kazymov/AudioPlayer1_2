package com.savka.audioplayer.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.savka.audioplayer.entity.Song;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Util class that provide saving and restoring of a playlist
 * <p/>
 * Created by naz on 30-Sep-14.
 */
public class SongStoreUtil {

    private static ArrayList<Song> songs;
    private static boolean isSaved;
    private static final String KEY_SONGS = "com.savka.audioplayer.songs";
    private static final String KEY_IS_SAVED = "com.savka.audioplayer.isSaved";

    static SharedPreferences sharedPreferences;
    static SharedPreferences.Editor editor;


    public static void saveSongs(ArrayList<Song> songs, PlayListMode playListMode, Context context) {
        sharedPreferences = context.getSharedPreferences(playListMode.name(), context.MODE_PRIVATE);
        isSaved = sharedPreferences.getBoolean(KEY_IS_SAVED, false);
        editor = sharedPreferences.edit();
        if (!isSaved) {
            Gson gson = new Gson();
            String json = gson.toJson(songs);

            editor.putString(KEY_SONGS, json);
            isSaved = true;
            editor.putBoolean(KEY_IS_SAVED, isSaved);
            editor.apply();

        }

    }

    public static ArrayList<Song> restoreSongs(PlayListMode playListMode, Context context) {

        sharedPreferences = context.getSharedPreferences(playListMode.name(), context.MODE_PRIVATE);

        isSaved = sharedPreferences.getBoolean(KEY_IS_SAVED, false);
        if (isSaved) {
            Gson gson = new Gson();
            String json = context.getSharedPreferences(playListMode.name(), context.MODE_PRIVATE)
                    .getString(KEY_SONGS, "");
            Type type = new TypeToken<ArrayList<Song>>() {
            }.getType();
            songs = gson.fromJson(json, type);
        }

        return songs;
    }

}
