package com.savka.audioplayer.utils.impl;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.exception.EmptySongsListException;
import com.savka.audioplayer.utils.MediaProvider;
import com.savka.audioplayer.utils.Observable;
import com.savka.audioplayer.utils.Observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

;

/**
 * Created by Admin on 27.08.2014.
 */
public class SdCardMediaProvider extends AsyncTask<Context, Context, ArrayList<Song>> implements MediaProvider, Observable {
    private List<Observer> observers;

    public SdCardMediaProvider() {
        observers = new ArrayList<Observer>();
    }

    private static final String LOG_TAG = "SdCardMediaProvider";

    private ArrayList<Song> songsList = new ArrayList<Song>();


    /**
     * Function to read all mp3 files from sdcard
     * and store the details in ArrayList
     */
    @Override
    public ArrayList<Song> getPlayList(Context context) throws EmptySongsListException {
        ArrayList<Song> songs = null;
        try {
            songs = this.get();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "InterruptedException");
        } catch (ExecutionException e) {
            Log.e(LOG_TAG, "ExecutionException");
        }
        if (songs == null) {
            throw new EmptySongsListException();
        } else {
            return songs;
        }
    }


    @Override
    protected ArrayList<Song> doInBackground(Context... context) {
        ContentResolver contentResolver = context[0].getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            Log.e(LOG_TAG, "query failed, cursor = null"); // query failed,
        } else if (!cursor.moveToFirst()) {
            Log.e(LOG_TAG, "Songs not found on Sd Card");
        } else {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
            do {
                Uri contentUri = ContentUris.withAppendedId(
                        android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idColumn));

                Song song = new Song();
                song.setId(cursor.getLong(idColumn));
                song.setTitle(cursor.getString(titleColumn));
                song.setPath(contentUri.toString());
                song.setArtist(cursor.getString(artistColumn));
                song.setAlbum(cursor.getString(albumColumn));
                song.setYear(cursor.getString(yearColumn));
                //todo set  bitRate
                songsList.add(song);
            } while (cursor.moveToNext());
        }
        return songsList;
    }



    @Override
    protected void onPostExecute(ArrayList<Song> songs) {
        notifyObservers();
    }

    @Override
    public void registerObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }
}
