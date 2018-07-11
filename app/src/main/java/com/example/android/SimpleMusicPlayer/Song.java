package com.example.android.SimpleMusicPlayer;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Comparator;

// customized object of a song
public class Song implements Parcelable{
    // name of the song item
    private String mSongName;
    // song's artist
    private String mArtistName;
   // song's album
    private String mAlbumName;
    // song's album ID
    private String mAlbumID;
    //song's location
    private String mSongLocation;
    //song's id
    private long mId;
    //song's duration?
    private long mDuration;

    /**
     * Create a new Song object
     * @param songName
     * @param artistName
     * @param albumName
     * @param albumID
     * @param songLocation
     * @param id
     * @param duration
     */
    public Song(String songName, String artistName, String albumName, String albumID, String songLocation,
                long id, long duration) {
        mSongName = songName;
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumID = albumID;
        mSongLocation = songLocation;
        mId = id;
        mDuration = duration;
    }

    // comparator for songs names (ascending)
    public static Comparator<Song> SongNameComparator = new Comparator<Song>() {
        public int compare(Song song1, Song song2) {
            String songName1 = song1.getSongName().toUpperCase();
            String songName2 = song2.getSongName().toUpperCase();
            //ascending order
            return songName1.compareTo(songName2);
        }};

    // comparator for sorting by album name
    public static Comparator<Song> AlbumNameComparator = new Comparator<Song>() {
        public int compare(Song song1, Song song2) {
            String albumName1 = song1.getAlbumName();
            String albumName2 = song2.getAlbumName();
            //ascending order
            return albumName1.compareTo(albumName2);
        }};

    // comparator for sorting by album ID
    public static Comparator<Song> AlbumIDComparator = new Comparator<Song>() {
        public int compare(Song song1, Song song2) {
            String albumID1 = song1.getAlbumID();
            String albumID2 = song2.getAlbumID();
            //ascending order
            return albumID1.compareTo(albumID2);
        }};

    // comparator for sorting by artist name
    public static Comparator<Song> ArtistNameComparator = new Comparator<Song>() {
        public int compare(Song song1, Song song2) {
            String artistName1 = song1.getArtistName();
            String artistName2 = song2.getArtistName();
            //ascending order
            return artistName1.compareTo(artistName2);
        }};

    // get the name of the song
    public String getSongName() { return mSongName;    }

    // get the artist of the song
    public String getArtistName() {
        return mArtistName;
    }

    // get the album ID of the song
    public String getAlbumID() {
        return mAlbumID;
    }

    // get the album of the song
    public String getAlbumName() {
        return mAlbumName;
    }

    // get the location of the song
    public String getSongLocation() { return mSongLocation; }

    // get the id for the song
    public long getID(){ return mId;}

    // get the duration of the song
    public long getDuration() { return mDuration; }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mSongName);
        dest.writeString(this.mArtistName);
        dest.writeString(this.mAlbumName);
        dest.writeString(this.mAlbumID);
        dest.writeString(this.mSongLocation);
        dest.writeLong(this.mId);
        dest.writeLong(this.mDuration);
    }

    protected Song(Parcel in) {
        this.mSongName = in.readString();
        this.mArtistName = in.readString();
        this.mAlbumName = in.readString();
        this.mAlbumID = in.readString();
        this.mSongLocation = in.readString();
        this.mId = in.readLong();
        this.mDuration = in.readLong();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}

