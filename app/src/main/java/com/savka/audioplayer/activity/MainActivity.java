package com.savka.audioplayer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.savka.audioplayer.R;
import com.savka.audioplayer.entity.ObjectDrawerItem;
import com.savka.audioplayer.entity.Song;
import com.savka.audioplayer.utils.PlayListMode;
import com.savka.audioplayer.utils.adapter.DrawerCustomAdapter;
import com.vk.sdk.VKUIHelper;

import java.util.ArrayList;


public class MainActivity extends FragmentActivity implements PlayListFragment.OnPlayListSelectedListener {

    private static final String LOG_TAG = "MainActivity";
    private static final String PLAY_LIST_FRAGMENT_TAG = "PlayListFragment";
    private static final String MAIN_FRAGMENT_TAG = "MainFragment";

    //DrawerLayout
    private String[] playListTitles;
    private DrawerLayout drawerLayout;
    private ListView myDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private static final int DRAWER_NAV_ITEM_FIRST = 0;
    private static final int DRAWER_NAV_ITEM_SECOND = 1;
    private static final int DRAWER_NAV_ITEM_THIRD = 2;


    PlayListMode playListMode = PlayListMode.SD;//sd card mode

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        VKUIHelper.onCreate(this);
        setContentView(R.layout.main);

        if (findViewById(R.id.fragment_container) != null) {
            //we are on one-pane Layout
            MainFragment mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
            if (mainFragment == null) {
                Log.v(LOG_TAG, "CreateNewMainFragment");
                mainFragment = new MainFragment();
            }
            if (savedInstanceState != null) {
                Log.v(LOG_TAG, "onCreate. Save currentPlayListMode");
                playListMode = (PlayListMode) savedInstanceState.getSerializable(PlayListFragment.INIT_MODE);
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mainFragment, MAIN_FRAGMENT_TAG).commit();
        } else {
            //we are on one-two pane Layout
            if (savedInstanceState != null) {
                playListMode = (PlayListMode) savedInstanceState.getSerializable(PlayListFragment.INIT_MODE);
            }
            PlayListFragment playListFragment = (PlayListFragment) getSupportFragmentManager().findFragmentById(R.id.play_list_fragment);
            Log.v(LOG_TAG, "playListFragment.update(" + playListMode.name() + ")");
            playListFragment.update(playListMode);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.v(LOG_TAG, "onOptionsItemSelected");
        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onSongSelected(int position, ArrayList<Song> songsList) {
        Log.v(LOG_TAG, "onSongSelected");
        // this.songsList = songsList;
        if (findViewById(R.id.fragment_container) != null) {
            Log.v(LOG_TAG, "onSongSelected one-pane layout");
            // we're in the one-pane layout and must swap frags...
            // take main fragment and update with current songs and position/
            MainFragment mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG);
            mainFragment.setSongs(songsList);
            mainFragment.setPosition(position);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, mainFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        } else {
            Log.v(LOG_TAG, "onSongSelected two-pane layout");
            MainFragment mainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.player_fragment);
            mainFragment.playSong(position, songsList);
        }
        // set correct Radio Mode
        if (playListMode.equals(PlayListMode.ONLINE_STREAM)) {
            MainFragment.setRadioMode(true);
        } else {
            MainFragment.setRadioMode(false);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(PlayListFragment.INIT_MODE, playListMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        VKUIHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Initialization of the Drawer Navigation Menu
        initDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);
    }

    @Override
    protected void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");
        super.onDestroy();
        VKUIHelper.onDestroy(this);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }

        private void selectItem(int position) {

            switch (position) {
                case DRAWER_NAV_ITEM_FIRST:
                    playListMode = PlayListMode.SD;
                    break;
                case DRAWER_NAV_ITEM_SECOND:
                    playListMode = PlayListMode.VK;
                    break;
                case DRAWER_NAV_ITEM_THIRD:
                    playListMode = PlayListMode.ONLINE_STREAM;
                    break;
            }

            Log.v(LOG_TAG, "DrawerItemClickListener" + playListMode.name());
            if (findViewById(R.id.fragment_container) != null) {
                // fragment_container != null,so we are on one-pane layout
                PlayListFragment playListFragment = (PlayListFragment) getSupportFragmentManager().findFragmentByTag(PLAY_LIST_FRAGMENT_TAG);
                if (playListFragment == null) {
                    Log.v(LOG_TAG, "Create new PlayListFragment " + playListMode.name());
                    playListFragment = new PlayListFragment();
                    Bundle bundle = new Bundle();
                    bundle.putSerializable(PlayListFragment.INIT_MODE, playListMode);
                    playListFragment.setArguments(bundle);
                } else {
                    Log.v(LOG_TAG, "Update old PlayList Fragment " + playListMode.name());
                    playListFragment.update(playListMode);
                }
                FragmentTransaction transaction2 = getSupportFragmentManager().beginTransaction();
                transaction2.replace(R.id.fragment_container, playListFragment, PLAY_LIST_FRAGMENT_TAG);
                transaction2.addToBackStack(null);
                transaction2.commit();
            } else {
                // Otherwise, we are on two-pane layout
                Log.v(LOG_TAG, "Update playlist fragment  " + playListMode.name());
                PlayListFragment playListFragment = (PlayListFragment) getSupportFragmentManager().findFragmentById(R.id.play_list_fragment);
                playListFragment.update(playListMode);
            }
        }
    }

    private void initDrawer() {
        playListTitles = getResources().getStringArray(R.array.playlist);

        mTitle = getTitle();
        mDrawerTitle = getResources().getString(R.string.change_drawer_title);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        myDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.drawable.ic_navigation_drawer, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActionBar().setTitle(mDrawerTitle);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActionBar().setTitle(mTitle);

            }
        };

        drawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        ObjectDrawerItem[] drawerItems = new ObjectDrawerItem[3];
        drawerItems[0] = new ObjectDrawerItem(R.drawable.btn_playlist, playListTitles[0]);

        drawerItems[1] = new ObjectDrawerItem(R.drawable.btn_playlist_vk, playListTitles[1]);

        drawerItems[2] = new ObjectDrawerItem(R.drawable.btn_playlist_live_stream, playListTitles[2]);

        DrawerCustomAdapter adapter = new DrawerCustomAdapter(this, R.layout.drawerview_item_row, drawerItems);

        //set adapter for list view
        myDrawerList.setAdapter(adapter);

        myDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }
}
