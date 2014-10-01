package com.savka.audioplayer.activity;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.savka.audioplayer.R;
import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.service.BackgroundAudioService;
import com.savka.audioplayer.utils.SeekbarHelper;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKUIHelper;

import java.util.ArrayList;

/**
 * Created by vlad-pc on 19.09.2014.
 */
public class MainFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {
    private static final String LOG_TAG = "MainFragment";
    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnNext;
    private ImageButton btnPrevious;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private SeekBar songProgressBar;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    //Service
    Intent playbackServiceIntent;
    BackgroundAudioService backgroundAudioService;
    ServiceConnection sConn;
    //Broadcast Receiver
    public final static String BROADCAST_ACTION = "com.savka.audioplayer";
    BroadcastReceiver br;
    public final static String PARAM_TASK = "task";
    public final static String PARAM_TITLE = "title";
    public final static String PARAM_TOTAL_DURATION = "duration";
    public final static String PARAM_CORENT_DURATION = "time";

    //tasks
    public final static int TASK1_CODE = 1; // update song title task
    public final static int TASK2_CODE = 2;// update progress bar task
    //Binder
    private boolean bound = false;
    private IntentFilter intFilt;
    // Handler to update UI timer, progress bar etc,.

    private String songTitle;
    private SeekbarHelper seekbarHelper;
    private int currentSongIndex;
    private long totalDuration;
    private boolean isPlaying = true;
    private boolean isShuffle;
    private boolean isRepeat;
    private static boolean isRadioMode;
    private SharedPreferences preferences;

    //VK dependence variables
    public static String[] scope = new String[]{VKScope.AUDIO, VKScope.OFFLINE, VKScope.FRIENDS, VKScope.STATUS};
    public static final String APP_ID = "4536437";
    public static final String METHOD_AUDIO_GET = "audio.get";
    private ArrayList<Song> songs;

    private int position;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        VKUIHelper.onCreate(getActivity());
        Log.v(LOG_TAG, "onCreateView");

        View viewRoot = inflater.inflate(R.layout.player, container, false);

        // All player buttons
        btnPlay = (ImageButton) viewRoot.findViewById(R.id.btnPlay);
        btnForward = (ImageButton) viewRoot.findViewById(R.id.btnForward);
        btnBackward = (ImageButton) viewRoot.findViewById(R.id.btnBackward);
        btnNext = (ImageButton) viewRoot.findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) viewRoot.findViewById(R.id.btnPrevious);
        btnRepeat = (ImageButton) viewRoot.findViewById(R.id.btnRepeat);
        btnShuffle = (ImageButton) viewRoot.findViewById(R.id.btnShuffle);
        songProgressBar = (SeekBar) viewRoot.findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) viewRoot.findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) viewRoot.findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) viewRoot.findViewById(R.id.songTotalDurationLabel);

        restoreState();

        playbackServiceIntent = new Intent(getActivity(), BackgroundAudioService.class);

        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "MainActivity onServiceConnected");
                backgroundAudioService = ((BackgroundAudioService.MyBinder) binder).getService();
                bound = true;
                if (songs != null) {
                    playSong(position, songs);
                }

            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "MainActivity onServiceDisconnected");
                bound = false;
            }
        };

        seekbarHelper = new SeekbarHelper();

        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important

        ////start new service if not running
        if (isMyServiceRunning(BackgroundAudioService.class)) {
            Log.v(LOG_TAG, "Service Is Already Running. Do Nothing!");
        } else {
            Log.v(LOG_TAG, "StartNewService!");
            getActivity().startService(playbackServiceIntent);

        }


        //     create BroadcastReceiver
        br = new BroadcastReceiver() {
            // actions when getting massages
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 0);
                totalDuration = intent.getLongExtra(PARAM_TOTAL_DURATION, 0);
                long currentDuration = intent.getLongExtra(PARAM_CORENT_DURATION, 0);
                // catch  massage's about starting tasks
                switch (task) {
                    case TASK1_CODE:
                        songTitle = intent.getStringExtra(PARAM_TITLE);
                        songTitleLabel.setText(songTitle);
                        break;
                    case TASK2_CODE:
                        // Displaying Total Duration time
                        songTotalDurationLabel.setText("" + seekbarHelper.milliSecondsToTimer(totalDuration));
                        // Displaying time completed playing
                        songCurrentDurationLabel.setText("" + seekbarHelper.milliSecondsToTimer(currentDuration));
                        //Updating progress bar
                        int progress = seekbarHelper.getProgressPercentage(currentDuration, totalDuration);
                        songProgressBar.setProgress(progress);
                        break;
                }
            }
        };

        // create filter for BroadcastReceiver
        intFilt = new IntentFilter(BROADCAST_ACTION);
        // register BroadcastReceiver

        /**
         * Play button click event
         * plays a song and changes button to pause image
         * pauses a song and changes button to play image
         * */
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                // check for already playing
                Log.v(LOG_TAG, "btnPlayPressed");
                isPlaying = backgroundAudioService.btnPlay();

                if (isPlaying) {
                    // Changing button image to pause button
                    btnPlay.setImageResource(R.drawable.btn_pause);
                } else {
                    // Changing button image to play button
                    btnPlay.setImageResource(R.drawable.btn_play);
                }
            }
        });

        /**
         * Forward button click event
         * Forwards song specified seconds
         * */
        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnForward");
                if (isRadioMode) {
                    Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
                } else {
                    backgroundAudioService.btnForward();
                }

            }
        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnBackward");
                if (isRadioMode) {
                    Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
                } else {
                    backgroundAudioService.btnBackWard();
                }
            }
        });

        /**
         * Next button click event
         * Plays next song by taking currentSongIndex + 1
         * */
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnNext");
                backgroundAudioService.btnNext();
            }
        });

        /**
         * Back button click event
         * Plays previous song by currentSongIndex - 1
         * */
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "Previous");
                backgroundAudioService.btnPrevious();
            }
        });

        /**
         * Button Click event for Repeat button
         * Enables repeat flag to true
         * */
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnRepeat");
                if (isRadioMode) {
                    Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
                } else {
                    isRepeat = backgroundAudioService.btnRepeat();
                    if (!isRepeat) {
                        Toast.makeText(getActivity().getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                        btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
                        btnShuffle.setImageResource(R.drawable.btn_shuffle);
                    }
                }
            }
        });

        /**
         * Button Click event for Shuffle button
         * Enables shuffle flag to true
         * */
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnShuffle");
                if (isRadioMode) {
                    Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
                } else {
                    isShuffle = backgroundAudioService.btnShuffle();
                    if (!isShuffle) {
                        Toast.makeText(getActivity().getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                        btnShuffle.setImageResource(R.drawable.btn_shuffle);
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                        // make shuffle to false
                        btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
                        btnRepeat.setImageResource(R.drawable.btn_repeat);
                    }
                }

            }
        });

        return viewRoot;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setSongs(ArrayList<Song> songs) {
        this.songs = songs;
    }

    public static void setRadioMode(boolean RadioMode) {
        MainFragment.isRadioMode = RadioMode;
    }

    public void playSong(int position, ArrayList<Song> songs) {
        Log.v(LOG_TAG, "bound = " + bound);
        if (!bound) return;
        backgroundAudioService.setSongsList(songs);
        backgroundAudioService.playSong(position);
    }


    @Override
    public void onStart() {
        super.onStart();
        getActivity().bindService(playbackServiceIntent, sConn, 0);
        Log.v(LOG_TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!bound) return;
        getActivity().unbindService(sConn);
        bound = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "onPause");
        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt("currentSongIndex", currentSongIndex);
        editor.putString("songTitle", songTitle);
        editor.putBoolean("isShuffle", isShuffle);
        editor.putBoolean("isRepeat", isRepeat);
        // Commit to storage
        editor.apply();
        getActivity().unregisterReceiver(br);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume");
        VKUIHelper.onResume(getActivity());
        getActivity().registerReceiver(br, intFilt);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        VKUIHelper.onCreate(getActivity());
    }


    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        backgroundAudioService.updateProgressBar();
    }


    /**
     *
     * */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        if (isRadioMode) {
            Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
        } else {
            // remove message Handler from updating progress bar
            backgroundAudioService.mHandlerRemoveCallbacks();
        }

    }

    /**
     * When user stops moving the progress hanlder
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (isRadioMode) {
            Toast.makeText(getActivity().getApplicationContext(), "Unusable in Radio mode", Toast.LENGTH_SHORT).show();
        } else {
            backgroundAudioService.mHandlerRemoveCallbacks();
            int currentPosition = (seekbarHelper.progressToTimer(seekBar.getProgress(), (int) totalDuration));

            Log.v(LOG_TAG, "currentPosition = " + currentPosition / 1000);
            // forward or backward to certain seconds
            backgroundAudioService.seekTo(currentPosition);

            // update timer progress again
            updateProgressBar();
        }

    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy()");
        super.onDestroy();
        VKUIHelper.onDestroy(getActivity());


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(requestCode, resultCode, data);
    }
   // private members
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void restoreState() {
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        // restore saved state


        currentSongIndex = preferences.getInt("currentSongIndex", 0);
        songTitle = preferences.getString("songTitle", "Song Title");
        isShuffle = preferences.getBoolean("isShuffle", false);

        songTitleLabel.setText(songTitle);
        isRepeat = preferences.getBoolean("isRepeat", false);
        //restore  shuffle/repeat image state
        if (!isShuffle) {
            btnShuffle.setImageResource(R.drawable.btn_shuffle);
        } else {
            btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
            btnRepeat.setImageResource(R.drawable.btn_repeat);
        }

        if (!isRepeat) {
            btnRepeat.setImageResource(R.drawable.btn_repeat);
        } else {
            btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
            btnShuffle.setImageResource(R.drawable.btn_shuffle);
        }
    }


}
