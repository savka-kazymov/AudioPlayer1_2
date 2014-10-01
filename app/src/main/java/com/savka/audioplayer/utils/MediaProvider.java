package com.savka.audioplayer.utils;

import android.content.Context;

import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.exception.EmptySongsListException;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by vlad-pc on 03.09.2014.
 */
public interface MediaProvider extends Serializable {


    public ArrayList<Song> getPlayList(Context context) throws EmptySongsListException;


}
