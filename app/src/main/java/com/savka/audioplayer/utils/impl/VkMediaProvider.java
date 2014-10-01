package com.savka.audioplayer.utils.impl;

import android.content.Context;
import android.util.Log;

import com.savka.audioplayer.activity.MainFragment;
import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.exception.EmptySongsListException;
import com.savka.audioplayer.utils.MediaProvider;
import com.savka.audioplayer.utils.Observable;
import com.savka.audioplayer.utils.Observer;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by naz on 9/10/2014.
 */
public class VkMediaProvider implements MediaProvider, Observable {

    public static final String LOG_TAG = "VkMediaProvider";
    public ArrayList<Song> songs;
    public List<Observer> observers;

    public VkMediaProvider() {
        this.songs = new ArrayList<>();
        observers = new ArrayList<>();
    }

    @Override
    public ArrayList<Song> getPlayList(Context context) throws EmptySongsListException {

        if (this.songs == null) {
            throw new EmptySongsListException();
        } else {
            return this.songs;
        }

    }

    public void doInBackground(Context context) {
        VKRequest vkRequest = new VKRequest(MainFragment.METHOD_AUDIO_GET);
        vkRequest.parseModel = false;
        vkRequest.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);
                Log.d(LOG_TAG + "onComplete: ", response.json.toString());
                songs = new ArrayList<>();
                try {
                    JSONObject jsonObject = response.json.getJSONObject("response");

                    JSONArray jsonArray = jsonObject.getJSONArray("items");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        Song song = Song.parse(json);
                        songs.add(song);
                    }
                    notifyObservers();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Log.d("MainActivity, Songs: ", songs.toString());

            }

            @Override
            public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                super.attemptFailed(request, attemptNumber, totalAttempts);
            }

            @Override
            public void onError(VKError error) {
                super.onError(error);
            }

            @Override
            public void onProgress(VKRequest.VKProgressType progressType, long bytesLoaded, long bytesTotal) {
                super.onProgress(progressType, bytesLoaded, bytesTotal);
            }
        });

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
