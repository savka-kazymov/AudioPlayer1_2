package com.savka.audioplayer.utils.impl;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.exception.EmptySongsListException;
import com.savka.audioplayer.utils.MediaProvider;
import com.savka.audioplayer.utils.Observable;
import com.savka.audioplayer.utils.Observer;
import com.savka.audioplayer.utils.Parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by naz on 9/16/2014.
 */
public class OnlineRadioMediaProvider extends AsyncTask<String, Void, ArrayList<Song>> implements MediaProvider, Observable {

    private static final String LOG_TAG = "OnlineRadioMediaProvider";
    private List<Observer> observers;

    public OnlineRadioMediaProvider() {
        observers = new ArrayList<Observer>();
    }

    @Override
    protected ArrayList<Song> doInBackground(String[] urls) {
        URLConnection connection = null;
        InputStream response = null;
        ArrayList<Song> songs = null;
        try {
            connection = new URL(urls[0]).openConnection();
            response = connection.getInputStream();
            Reader reader = filterInputStream(response);

            songs = new Parser().readJsonStream(reader);
            response.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Fail to read from " + urls[0], e);
        }

        return songs;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected void onPostExecute(ArrayList<Song> list) {
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

    // private members
    private Reader filterInputStream(InputStream response) throws IOException {
        response.skip(29);
        ByteArrayOutputStream read = read(response);
        return new InputStreamReader(new ByteArrayInputStream(read.toByteArray(), 0, read.size() - 2));
    }

    private ByteArrayOutputStream read(InputStream in) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }


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
}
