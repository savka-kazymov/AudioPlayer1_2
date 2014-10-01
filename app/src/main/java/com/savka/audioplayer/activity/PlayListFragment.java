package com.savka.audioplayer.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.exception.EmptySongsListException;
import com.savka.audioplayer.utils.MediaProvider;
import com.savka.audioplayer.utils.Observer;
import com.savka.audioplayer.utils.PlayListMode;
import com.savka.audioplayer.utils.SongStoreUtil;
import com.savka.audioplayer.utils.impl.OnlineRadioMediaProvider;
import com.savka.audioplayer.utils.impl.SdCardMediaProvider;
import com.savka.audioplayer.utils.impl.VkMediaProvider;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.VKUIHelper;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;

import java.io.Serializable;
import java.util.ArrayList;

public class PlayListFragment extends ListFragment implements Observer, Serializable {

    private static final String LOG_TAG = "PlayListFragment";
    private static final String BROADCASTING_STATIONS_URL = "http://audio.rambler.ru/json/stations.js";
    public static final String INIT_MODE = "flag";
    private static final long serialVersionUID = 7561774209316378393L;
    private OnPlayListSelectedListener mCallback;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private ArrayAdapter<Song> adapter;
    private SdCardMediaProvider sdCardMediaProvider;
    private VkMediaProvider vkMediaProvider;
    private  OnlineRadioMediaProvider onlineRadioMediaProvider;
    private MediaProvider mediaProvider;
    private ArrayList<Song> songsList;

    private PlayListMode playListMode = PlayListMode.SD; //default value

    // Container Activity must implement this interface
    public interface OnPlayListSelectedListener {
        public void onSongSelected(int position, ArrayList<Song> songsList);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mCallback = (OnPlayListSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallback.onSongSelected(position, songsList);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VKUIHelper.onCreate(getActivity());
        Log.v(LOG_TAG, "onCreate");


        Log.d(LOG_TAG, "onCreate without  State");
        if (getArguments() != null) {
            Bundle arguments = getArguments();
            playListMode = (PlayListMode)arguments.getSerializable(INIT_MODE);
            init(playListMode);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreateView");
        Log.v(LOG_TAG, "Playlist fragment instanse" + this.toString());
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        editor = sharedPreferences.edit();
        return super.onCreateView(inflater, container, savedInstanceState);

    }


    private VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
            //     VKSdk.authorize(MainActivity.scope);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
            new AlertDialog.Builder(getActivity().getApplicationContext())
                    .setMessage(authorizationError.errorMessage)
                    .show();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            Log.d("onReceiveNewToken", newToken.accessToken);
            //     newToken.saveTokenToSharedPreferences(getApplicationContext(), MainActivity.tokenKey);

        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        VKUIHelper.onDestroy(getActivity());
        Log.d(LOG_TAG, "onDestroy()");
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    @Override
    public void onResume() {
        super.onResume();
        VKUIHelper.onResume(getActivity());
        Log.d(LOG_TAG, "onResume()");

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void update() {
        // Create adapter
        Log.v(LOG_TAG, "call update");
        try {
            if (getActivity() != null) {
                songsList = mediaProvider.getPlayList(getActivity().getApplicationContext());
                SongStoreUtil.saveSongs(songsList, playListMode, getActivity().getApplicationContext());
                createListAdapter(songsList);
            }
        } catch (EmptySongsListException e) {
            Log.e(LOG_TAG, "EmptySongsLis", e);
        }

    }

    public void update(PlayListMode playListMode) {
        Log.v(LOG_TAG, "call update from Main Activity");
        Toast.makeText(getActivity().getApplicationContext(), "Updating Playlist...", Toast.LENGTH_SHORT).show();
        init(playListMode);
    }

    // private members
    private void init(PlayListMode playListMode) {
        Log.v(LOG_TAG, playListMode.name() + " picked");
        this.playListMode = playListMode;
        ArrayList<Song> songsList = SongStoreUtil.restoreSongs(playListMode, getActivity().getApplicationContext());

        if (songsList != null) {
            createListAdapter(songsList);
            this.songsList = songsList;
            return;
        }

        switch (playListMode) {
            case VK:
                VKSdk.initialize(sdkListener, MainFragment.APP_ID);
                VKUIHelper.setApplicationContext(getActivity().getApplicationContext());
                vkMediaProvider = new VkMediaProvider();
                vkMediaProvider.registerObserver(this);
                if (VKSdk.wakeUpSession()) {
                    vkMediaProvider.doInBackground(getActivity().getApplicationContext());
                    mediaProvider = vkMediaProvider;
                } else {
                    VKSdk.authorize(MainFragment.scope);
                    vkMediaProvider.doInBackground(getActivity().getApplicationContext());
                    mediaProvider = vkMediaProvider;
                }
                break;

            case SD:
                sdCardMediaProvider = new SdCardMediaProvider();
                sdCardMediaProvider.registerObserver(this);
                sdCardMediaProvider.execute(getActivity().getApplicationContext());
                mediaProvider = sdCardMediaProvider;
                break;
            case ONLINE_STREAM:
                onlineRadioMediaProvider = new OnlineRadioMediaProvider();
                onlineRadioMediaProvider.registerObserver(this);
                onlineRadioMediaProvider.execute(BROADCASTING_STATIONS_URL);
                mediaProvider = onlineRadioMediaProvider;
                break;
        }
    }

    private void createListAdapter(ArrayList<Song> songs) {
        adapter = new ArrayAdapter<Song>(getActivity(),
                android.R.layout.simple_list_item_1, songs);
        setListAdapter(adapter);
    }
}
