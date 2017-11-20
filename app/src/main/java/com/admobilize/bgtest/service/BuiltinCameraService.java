package com.admobilize.bgtest.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Antonio Vanegas @hpsaturn on 4/3/17.
 */

public class BuiltinCameraService extends Service {

    private static final String TAG = BuiltinCameraService.class.getSimpleName();
    private BuiltinCameraDevice camera;
    private boolean isDetectionRunning;
    private final IBinder mBinder = new BuiltinCameraService.LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, TAG + " onStartCommand..");
        handleStartTrackingCommand();
        return START_STICKY;
    }

    private void handleStartTrackingCommand() {

        if (isDetectionRunning) {
            Log.d(TAG, "-->already configured.");
            return;
        }

        isDetectionRunning = true;
        camera = new BuiltinCameraDevice(this, 640, 480);
        camera.setCameraOrientation(0);
        camera.allowCameraOrientation(false);

        Log.d(TAG, "-->camera start");
    }

    private void handleStopTrackingCommand(String message) {
        try {
            Log.d(TAG, "-->handleStopTrackingCommand..");
            if (camera != null) {
                camera.stop();
            }
            isDetectionRunning = false;

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Detection is Finished.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        handleStopTrackingCommand("");
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public BuiltinCameraService getServiceInstance() {
            return BuiltinCameraService.this;
        }
    }

}
