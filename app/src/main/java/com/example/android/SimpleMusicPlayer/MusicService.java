package com.example.android.SimpleMusicPlayer;

import java.util.ArrayList;

import android.media.session.PlaybackState;
import android.support.annotation.RequiresApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.util.Collections;

import static com.example.android.SimpleMusicPlayer.SongsActivity.sessionToken;

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {
    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs1;
    private ArrayList<Integer> shuffleIndexes; // array list for indexes order of play
    //current position
    private int songPosn = 0;
    private int songsNumber;
    private final IBinder musicBind = new MusicBinder();
    AudioManager mAudioManager;
    private String songTitle = "";
    private boolean shuffle = false;
    long state = PlaybackStateCompat.STATE_PAUSED;
    static final int AUTO = 1;
    static final int PLAY_TRACK = 2;
    int action;
    private static final String CHANNEL_ID = "media_playback_channel";

    // This listener gets triggered when the audio focus has changed
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new
            AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    // The AUDIOFOCUS_LOSS_TRANSIENT case means that we've lost audio focus for a short amount of time.
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        if (player.isPlaying()) {
                            player.pause();
                            state = PlaybackStateCompat.STATE_PAUSED;
                        }
                        // The AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK case means that our app is allowed to continue playing
                        // sound but at a lower volume.
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                        // Lost focus for a short time, but it's ok to keep playing
                        // at an attenuated level
                        if (player.isPlaying()) player.setVolume(0.1f, 0.1f);
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                        // The AUDIOFOCUS_GAIN case means we have regained focus resume playback
                        if (player == null) initMusicPlayer();
                        else if (!player.isPlaying()) {
                            player.start();
                            state = PlaybackStateCompat.STATE_PLAYING;
                        }
                        // The AUDIOFOCUS_LOSS case means we've lost audio focus
                        // Lost focus for an unbounded amount of time: stop playback and release media player
                    } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        if (player.isPlaying()) player.stop();
                        player.release();
                        player = null;
                    }
                }
            };

    @Override
    public IBinder onBind(Intent intent) {
        Intent onBindIntent = new Intent("STATE_BIND");
        LocalBroadcastManager.getInstance(this).sendBroadcast(onBindIntent);
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i("MusicService", "onUnbind");
        return false;
    }

    public void onCreate() {
        //create the service
        Log.i("MusicService", "service OnCreate was called");
        super.onCreate();
        //create player
        player = new MediaPlayer();
        if (requestAudioFocus()) {
            Log.i("MusicService", "service as audioFocus");
            initMusicPlayer();
        }
    }

    // what happens when the user click shuffle-all button
    public void setShuffle() {
        shuffle = true;
        // set AUTO so when the song ends - it will start the next song automatically
        action = PLAY_TRACK;
        if (shuffleIndexes != null) shuffleIndexes.clear();
        // create a new list of shuffled indexes
        shuffleIndexes = new ArrayList<>();
        for (int i = 0; i < songsNumber; i++) shuffleIndexes.add(i);
        Collections.shuffle(shuffleIndexes);
        songPosn = 0;
        playSong();
    }

    // what happens when the user click play-all button
    public void setPlayAll() {
        shuffle = false;
        // set AUTO so when the song ends - it will start the next song automatically
        action = PLAY_TRACK;
        if (shuffleIndexes != null) shuffleIndexes.clear();
        songPosn = 0;
        playSong();
    }

    // what happens when the user click on a song in the list
    public void setPlayTrack(int position) {
        shuffle = false;
        action = PLAY_TRACK;
        if (shuffleIndexes != null) shuffleIndexes.clear();
        songPosn = position;
        playSong();
    }

    // set player properties
    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        player.reset();
    }

    // set the songs list to be played
    public void setList(ArrayList<Song> theSongs) {
        songs1 = theSongs;
        songsNumber = songs1.size();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        Log.i("MusicService", "completion song position is " + songPosn +
                " and action is " + action);
        // if the player clicked on prev, next or a song in the list and somehow got here - play current song
        if (action ==PLAY_TRACK) {
            playSong();
//            // if the player got here on next clicked - play current song
//        } else if (action == NEXT) {
//            playSong();
//            // if the player got here on song clicked - play current song
//        } else if (action == PLAY_TRACK) {
//            playSong();
        }
        // just reached the end of the song naturally - go to next song
        else if (action == AUTO) {
            playNext();
        }
    }

    // if an error happened - reset the player
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    // boolean method - return true when audio focus was granted
    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Focus gained
            return true;
        }
        //Could not gain focus
        return false;
    }

    // what to do when the player is ready
    public void preparePlayer() {
        player.start();
        state = PlaybackStateCompat.STATE_PLAYING;
        Intent onPreparedIntent = new Intent("MEDIA_PLAYER_PREPARED");
        LocalBroadcastManager.getInstance(this).sendBroadcast(onPreparedIntent);
        // set AUTO so when the song ends - it will start the next song automatically
        action = AUTO;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i("MusicService", "service onPrepared was called");
        //start playback
        preparePlayer();
    }

    //play the right song
    public void playSong() {
        player.reset();
        Song playedSong;
        //get the right song
        if (shuffle) {
            int newSong = shuffleIndexes.get(songPosn);
            playedSong = songs1.get(newSong);
        } else {
            playedSong = songs1.get(songPosn);
        }
        songTitle = playedSong.getSongName();
        Uri uri = Uri.parse(playedSong.getSongLocation());
        try {
            player.setDataSource(getApplicationContext(), uri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        state = PlaybackStateCompat.STATE_PLAYING;
        player.prepareAsync();
        showNotification();
    }

    public String getSongTitle() {
        return songTitle;
    }

    public int getPosn() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
        state = PlaybackStateCompat.STATE_PAUSED;
        showNotification();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
        state = PlaybackStateCompat.STATE_PLAYING;
        showNotification();
    }

    // skip to prev
    public void playPrev() {
        action = PLAY_TRACK;
        songPosn--;
        if (songPosn < 0) songPosn = songsNumber - 1;
        playSong();
    }

    //skip to next
    public void playNext() {
        action = PLAY_TRACK;
        songPosn++;
        if (songPosn >= songsNumber) songPosn = 0;
        playSong();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // from api 26 you need a channel to set notifications
    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager
                mNotificationManager =
                (NotificationManager) this
                        .getSystemService(NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    // Shows Media Style notification, with actions that depend on the current MediaSession
    // PlaybackState
    public void showNotification() {
        // You only need to create the channel on API 26+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
        android.support.v4.app.NotificationCompat.Builder builder =
                new android.support.v4.app.NotificationCompat.Builder(this, CHANNEL_ID);
        builder.addAction(R.drawable.ic_action_playback_prev, Constants.ACTION_PREV,
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(
                    R.drawable.ic_pause, Constants.ACTION_PAUSE,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PAUSE));
        } else {
            builder.addAction(
                    R.drawable.ic_play_arrow, Constants.ACTION_PLAY,
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PLAY));
        }
        builder.addAction(R.drawable.ic_action_playback_next, Constants.ACTION_NEXT,
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        builder.addAction(R.drawable.ic_action_cancel, Constants.ACTION_STOP,
                MediaButtonReceiver.buildMediaButtonPendingIntent
                        (this, PlaybackStateCompat.ACTION_STOP));

        Intent intent = new Intent(getApplicationContext(), SongsActivity.class);
        intent.addCategory(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.setClass(this, SongsActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentTitle(getString(R.string.app_name))
                .setContentText(getSongTitle())
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(Constants.getDefaultAlbumArt(this))
                .setVisibility(android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(sessionToken)
                        .setShowActionsInCompactView(0, 1));
        Notification notification = builder.build();
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                notification);
    }

    // stopping the notification and service
    public void stopNotifications() {
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    // Handle case when user swipes the app away from the recents apps list by
    // stopping the service (and any ongoing playback).
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

}
