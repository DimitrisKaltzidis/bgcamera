package com.admobilize.bgtest.service;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;


/**
 * Created by Antonio Vanegas @hpsaturn on 2017.11.19.
 */

public class BuiltinCameraDevice  {


    private final BuiltinFrameListener mBuiltFrame;

    public BuiltinCameraDevice(Context ctx, int width, int height) {

        SurfaceView sv = new SurfaceView(ctx);
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        SurfaceHolder sh = sv.getHolder();
        mBuiltFrame = new BuiltinFrameListener(ctx,width,height);
        sh.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        sv.setZOrderOnTop(true);
        sh.setFormat(PixelFormat.TRANSPARENT);
        sh.addCallback(mBuiltFrame);
        wm.addView(sv, params);

    }

    public void setCameraOrientation(int orientation){
      mBuiltFrame.setOrientation(orientation);

    }

    public void allowCameraOrientation(boolean isAllowed){
        mBuiltFrame.allowCameraOrientation(isAllowed);
    }

    public boolean isCameraOrientationChangeAllowed( ){
      return   mBuiltFrame.isCameraOrientationChangeAllowed();
    }

    public void stop (){
        mBuiltFrame.onStop();
    }


}
