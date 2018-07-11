package com.example.android.SimpleMusicPlayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import butterknife.BindView;
import butterknife.ButterKnife;

import com.example.android.SimpleMusicPlayer.MusicService.MusicBinder;

public class SongsActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener,
        MediaPlayerControl {
    // binding the views in song_list_display.xml
    public @BindView(R.id.parent_relative_layout)
    RelativeLayout relativeLayout;
    public @BindView(R.id.shuffle_all_button)
    FloatingActionButton mShuffle;
    public @BindView(R.id.play_all_button)
    FloatingActionButton mPlay;
    public @BindView(R.id.track_playing)
    TextView mTrackPlaying;
    public @BindView(R.id.list)
    ListView listView;
    public @BindView(R.id.action_layout)
    LinearLayout actionLayout;
    public int numberOfSongs;
    public ArrayList<Song> songs; // arrayList for the Song objects
    //service
    public static MusicService musicSrv;
    private Intent playIntent;
    //binding
    private boolean musicBound=false;
    public static MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    long state;
    public MusicController controller;
    private boolean paused = false, playbackPaused = false;
    public static MediaSessionCompat.Token sessionToken; // needed to put static because it is used for the notifications

    // Broadcast receiver to determine when music player has been prepared
    private BroadcastReceiver onPrepareReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            Log.i("SongsActivity", "onPrepareReceiver");
            // When music player has been prepared, show controller and song info
            showSongInfo();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.songs_activity_display); // the layout from which the list will be inflated
        ButterKnife.bind(this); // binding the views
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out); // animation for layout transition
        actionLayout.setVisibility(View.GONE); // no need for it until a track is played
        createLists(); // right now only one list - of the audio files in the device
        SongAdapter adapter = new SongAdapter(this, songs);  // creating the adapter for the list of objects
        listView.setAdapter(adapter); // setting the adapter
        // setting click listener for the items in the list
        listView.setOnItemClickListener(this);
        mShuffle.setOnClickListener(this);
        // when the user press the play button - start to play the songs one after the other
        mPlay.setOnClickListener(this);
        bindMusicService();
        setController();
        initializeMediaSession();
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicService.MusicBinder) service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songs);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void bindMusicService() {
        Log.i("SongsActivity", "bindMusicService");
        if (playIntent == null) {
            playIntent = new Intent(this, MusicService.class);
            startService(playIntent); // starting the service and then binding it - so it will continue running after closing app
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void createLists() {
        createSongsList(); // create the list of Song objects
        if (!songs.isEmpty()) {
            numberOfSongs = songs.size(); // get the size of the list (number of objects)
        } else numberOfSongs = 0;
    }

    // creating the ArrayList of Song objects - using loadAudio method
    public void createSongsList() {
        songs = new ArrayList<Song>(); // create the arrayList to store Song objects
        loadAudio(); // this add items to the arrayList based on files in the user device
        if (!songs.isEmpty()) {
            //sorting the songs alphabetically
            Collections.sort(songs, Song.SongNameComparator);
        }
    }

    // method for generating an ArrayList of Song object, from the audio files on the device
    private void loadAudio() {
        String songName; // local String to store song (track) name
        String artistName; // local String to store artist name
        String albumName; // local String to store album name
        String songLocation; // local String to store track location (path)
        String albumID; // local String to store album id
        long songId; // local long to store song id
        long songDuration; // local long for song duration
        // going through the device memory to fetch audio files data and storing each track data in a Song object
        // to create Song object list
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);
        if (songCursor != null && songCursor.moveToFirst()) { //if there are song items on the device
            int idColumn = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int currentSongName = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int currentAlbumID = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int currentArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int currentAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int currentLocation = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int currentDuration = songCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            do {
                songName = songCursor.getString(currentSongName);
                artistName = songCursor.getString(currentArtist);
                albumName = songCursor.getString(currentAlbum);
                albumID = songCursor.getString(currentAlbumID);
                songLocation = songCursor.getString(currentLocation);
                songId = songCursor.getLong(idColumn);
                songDuration = songCursor.getLong(currentDuration);
                songs.add(new Song(songName, artistName, albumName, albumID, songLocation, songId, songDuration));
            } while (songCursor.moveToNext());
        }
        songCursor.close();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.list:
                if (songs.isEmpty())
                    Toast.makeText(this, R.string.no_tracks_toast,
                            Toast.LENGTH_SHORT).show();
                else {
                    songPicked(position);
                }
        }
    }

    // the onClick events for this activity
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_all_button:
                if (songs.isEmpty())
                    Toast.makeText(this, R.string.no_tracks_toast,
                            Toast.LENGTH_SHORT).show();
                else {
                    musicSrv.setPlayAll();
                    actionLayout.setVisibility(View.VISIBLE);
                    mTrackPlaying.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in));
                    mTrackPlaying.setText(musicSrv.getSongTitle());
                    if (playbackPaused) {
                        playbackPaused = false;
                    }
                }
                break;
            case R.id.shuffle_all_button:
                if (songs.isEmpty())
                    Toast.makeText(this, R.string.no_tracks_toast,
                            Toast.LENGTH_SHORT).show();
                else {
                    musicSrv.setShuffle();
                    actionLayout.setVisibility(View.VISIBLE);
                    mTrackPlaying.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in));
                    mTrackPlaying.setText(musicSrv.getSongTitle());
                    if (playbackPaused) {
                        playbackPaused = false;
                    }
                }
                break;
        }
    }

     public void songPicked(int position) {
        musicSrv.setPlayTrack(position);
        actionLayout.setVisibility(View.VISIBLE);
        mTrackPlaying.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in));
        mTrackPlaying.setText(musicSrv.getSongTitle());
        if (playbackPaused) {
            playbackPaused = false;
        }
    }

    // if no song was played - just leave, otherwise - unbound the service
    @Override
    public void onBackPressed() {
        Log.i("SongsActivity", "onBack");
        super.onBackPressed();
        if (musicSrv == null) {
            // if the mediaPlayer is empty - just clear the relevant arrayLists and finish
            songs.clear();
            finish();
        } else { // otherwise - release media resources and clear the relevant arrayLists
            moveTaskToBack(true);
            doUnbindService();
        }
    }

    @Override
    public void pause() {
        playbackPaused = true;
        paused = true;
        pausePlayer();
    }

    @Override
    public void seekTo(int pos) {
        seek(pos);
    }

    @Override
    public void start() {
        playbackPaused = false;
        paused = false;
        go();
    }

    @Override
    public int getDuration() {
        if (musicSrv != null)
            return getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicSrv != null )
            return getPosn();
        else return 0;
    }

    @Override
    public boolean isPlaying() {
        if (musicSrv != null) {
            state = PlaybackStateCompat.STATE_PLAYING;
            return isPng();
        }
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public static class MediaReceiver extends BroadcastReceiver {
        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mediaSession, intent);
        }
    }

    private void initializeMediaSession() {
        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, "SongActivity");
        sessionToken = mediaSession.getSessionToken();
        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        // Do not let MediaButtons restart the player when the app is not visible
        mediaSession.setMediaButtonReceiver(null);
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_STOP);

        mediaSession.setPlaybackState(mStateBuilder.build());
        // MySessionCallback has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());
        // Start the Media Session since the activity is active
        mediaSession.setActive(true);
    }

    /**
     * Media Session Callbacks, where all external clients control the player.
     */
    public class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            paused = false;
            playbackPaused = false;
            go();
            showSongInfo();
            state = PlaybackStateCompat.STATE_PLAYING;
        }

        @Override
        public void onPause() {
            paused = true;
            playbackPaused = true;
            pausePlayer();
            showSongInfo();
            state = PlaybackStateCompat.STATE_PAUSED;
        }

        @Override
        public void onSkipToPrevious() {
            playPrev();
            showSongInfo();
        }

        @Override
        public void onSkipToNext() {
            playNext();
            showSongInfo();
        }

        @Override
        public void onStop() {
            mediaSession.release();
            musicSrv.stopNotifications();
            musicSrv.stopSelf();
            System.exit(0);
        }
    }

    public String getSongTitle() {
        return musicSrv.getSongTitle();
    }

    public int getPosn() {
        return musicSrv.getPosn();
    }

    public int getDur() {
        return musicSrv.getDur();
    }

    public boolean isPng() {
        return musicSrv.isPng();
    }

    public void pausePlayer() {
        paused = true;
        playbackPaused = true;
        state = PlaybackStateCompat.STATE_PAUSED;
        musicSrv.pausePlayer();
        controller.show();
    }

    public void seek(int posn) {
        musicSrv.seek(posn);
    }

    public void go() {
        paused = false;
        playbackPaused = false;
        musicSrv.go();
        state = PlaybackStateCompat.STATE_PLAYING;
        controller.show();
    }

    public void playPrev() {
        Log.i("SongsActivity", "playPrev was clicked song position");
        paused = false;
        playbackPaused = false;
        musicSrv.playPrev();
    }

    //skip to next
    public void playNext() {
        Log.i("SongsActivity", "playNext was clicked song position");
        paused = false;
        playbackPaused = false;
        musicSrv.playNext();
    }

    // if songs were played - unbound the service
    @Override
    protected void onDestroy() {
        Log.i("SongsActivity", "onDestroy");
        if (musicSrv!=null ) {
            doUnbindService();
            moveTaskToBack(true);
        }
        super.onDestroy();
    }

    void doUnbindService() {
        if (musicBound) {
            // Release information about the service's state
            unbindService(musicConnection);
            musicBound = false;
        }
    }

    //set the controller up
    public void setController() {
        Log.i("SongsActivity", "setController was called");
        if (controller == null) controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(relativeLayout);
        controller.setEnabled(true);
    }

    // showing the controller on the bottom and the name of the song on the start
    public void showSongInfo() {
        controller.show();
        actionLayout.setVisibility(View.VISIBLE);
        mTrackPlaying.setAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in));
        mTrackPlaying.setText(getSongTitle());
    }

    @Override
    public void onResume() {
        Log.i("SongsActivity", "onResume");
        super.onResume();
        if (paused) {
            paused = false;
            playbackPaused = false;
        }
        // Set up receiver for media player onPrepared broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(onPrepareReceiver,
                new IntentFilter("MEDIA_PLAYER_PREPARED"));
    }

    // here you make sure that if the service already started (i.e. returned to the app via notification)
    // the mediaController will show on screen
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (musicSrv!=null) {
            try {
                showSongInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}




