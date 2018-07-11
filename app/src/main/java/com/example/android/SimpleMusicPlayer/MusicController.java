package com.example.android.SimpleMusicPlayer;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context c){
        super(c);
    }

    public void hide(){
        super.show();
    }

    // get out of the activity even if the controller is up
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode(); if(keyCode == KeyEvent.KEYCODE_BACK) {
            Context c = getContext();
            ((Activity) c).finish();
            return true;
        }
            return super.dispatchKeyEvent(event);
    }

}
