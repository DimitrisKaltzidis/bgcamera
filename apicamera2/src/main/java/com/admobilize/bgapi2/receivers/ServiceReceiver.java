package com.admobilize.bgapi2.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.admobilize.bgapi2.service.CameraService;


/**
 * Created by Antonio Vanegas @hpsaturn on 3/24/17.
 */

public class ServiceReceiver extends BroadcastReceiver {
    public static final String TAG = ServiceReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"StartServiceReceiver: onReceive");
        Intent service = new Intent(context, CameraService.class);
        context.startService(service);
    }

}
