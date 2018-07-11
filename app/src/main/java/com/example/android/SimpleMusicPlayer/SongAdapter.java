package com.example.android.SimpleMusicPlayer;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SongAdapter extends ArrayAdapter<Song> {
    private static final String LOG_TAG = SongAdapter.class.getSimpleName();
    public @BindView (R.id.song_name)
    TextView mSongName;
    public @BindView (R.id.album_name)
    TextView mAlbumName;
    public @BindView (R.id.artist_name)
    TextView mArtistName;

    // this is the constructor for the songAdapter object
    public SongAdapter(Activity context, ArrayList<Song> currentListOfSongs) {
        super(context, 0, currentListOfSongs);
    }

    // populating the list using song_list_item.xml layout
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.song_list_item, parent, false);
        }
        ButterKnife.bind(this,listItemView); // binding the views
        Song currentSong = getItem(position);
        String songName = currentSong.getSongName();
        if (!songName.isEmpty()) mSongName.setText(songName);
        else mSongName.setText("0");
        String albumName = currentSong.getAlbumName();
        if (!albumName.isEmpty()) mAlbumName.setText(albumName);
        else mAlbumName.setText("0");
        String artistName = currentSong.getArtistName();
        if (!artistName.isEmpty()) mArtistName.setText(artistName);
        else mArtistName.setText("0");
        listItemView.setTag(position);
        return listItemView;
    }
}
