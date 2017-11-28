package com.admobilize.bgapi2.service;

import android.content.Context;


/**
 * Created by Antonio Vanegas @hpsaturn on 2017.11.19.
 */

public class CameraDevice {

    private final FrameListener mBuiltFrame;

    public CameraDevice(Context ctx, int width, int height) {
        mBuiltFrame = new FrameListener(ctx, width, height);
    }

    public void stop (){
        mBuiltFrame.onStop();
    }

}
