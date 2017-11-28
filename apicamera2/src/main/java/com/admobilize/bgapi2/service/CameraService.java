package com.admobilize.bgapi2.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * Created by Antonio Vanegas @hpsaturn on 2017.11.19.
 */

public class CameraService extends Service {

    private static final String TAG = CameraService.class.getSimpleName();

    //stop messages
    private static final String MESSAGE_SERVICE_STOP = "service_stop";

    private final IBinder mBinder = new CameraService.LocalBinder();
    private CameraDevice camera;
    private boolean isDetectionRunning;


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
        camera = new CameraDevice(this, 640,480 );
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
        handleStopTrackingCommand(MESSAGE_SERVICE_STOP);
        super.onDestroy();
    }

    //returns the instance of the service
    public class LocalBinder extends Binder {
        public CameraService getServiceInstance() {
            return CameraService.this;
        }
    }



}
