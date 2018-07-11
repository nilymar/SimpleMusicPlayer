package com.example.android.SimpleMusicPlayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Constants {
    public static final String ACTION_MAIN = "MAIN_ACTION";
    public static final String ACTION_PREV = "PREV_ACTION";
    public static final String ACTION_NEXT = "NEXT_ACTION";
    public static final String ACTION_PLAY = "PLAY_ACTION";
    public static final String ACTION_PAUSE = "PAUSE_ACTION";
    //        public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_STOP = "STOP_ACTION";
    public static final String ACTION_START_FOREGROUND = "START_FOREGROUND_ACTION";
    public static final String ACTION_STOP_FOREGROUND = "STOP_FOREGROUND_ACTION";

    public static String STARTFOREGROUND_ACTION = "startforeground";
    public static String STOPFOREGROUND_ACTION = "stopforeground";

    public static final String ACTION_REWIND = "REWIND_ACTION";
    public static final String ACTION_FAST_FORWARD = "FAST_FORWARD_ACTION";

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }

    public static Bitmap getDefaultAlbumArt(Context context) {
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            bm = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.icon_background, options);
        } catch (Error ee) {
        } catch (Exception e) {
        }
        return bm;
    }

}


