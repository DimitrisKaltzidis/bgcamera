package com.admobilize.bgapi2.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;


/**
 * Created by Antonio Vanegas @hpsaturn on 3/24/17.
 */

public class ServiceScheduler extends BroadcastReceiver {

    public static final String TAG = ServiceScheduler.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        startScheduleService(context, 15*1000);
    }

    public static void startScheduleService(Context context, long repeatTime) {
        Log.d(TAG, "startScheduleService: builtincamera" );
        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ServiceReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        Calendar cal = Calendar.getInstance();
        // Start x seconds after boot completed
        cal.add(Calendar.SECOND, 5);
        // Fetch every 30 seconds
        // InexactRepeating allows Android to optimize the energy consumption
        service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeatTime, pending);
        // service.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
        // REPEAT_TIME, pending);
    }

    public static void stopSheduleService(Context context) {
        Log.d(TAG, "stopSheduleService:");
        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, ServiceReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        service.cancel(pending);
    }
}
