package org.chad.jeejah.callquota;

import android.util.Log;
import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.provider.CallLog.Calls;

public class LogMonitorService extends Service {
    private static final String LOG_TAG = "LogMonitorService";
    private Handler serviceHandler = null;
    private int counter;

    private Notification note;
    private NotificationManager notMan;

    public void onStart(int startId, Bundle arguments) {
        //super.onStart(startId, arguments);
        Log.d(LOG_TAG, "onStart");

    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG,"onCreate");

        Intent seeStats = new Intent();
        seeStats.setClassName("org.chad.jeejah.callquota", "org.chad.jeejah.callquota.SeeStats");
        PendingIntent pendingSeeStats = PendingIntent.getActivity(this, 0, seeStats, 0);

        serviceHandler = new Handler();
        ContentResolver contentResolver = getContentResolver();
        ContentObserver observer = new InterpretLogChanges(serviceHandler);
        contentResolver.registerContentObserver(Calls.CONTENT_URI, true, observer);

        note = new Notification(R.drawable.cost_notification, "Call time may exceed allotment.", java.lang.System.currentTimeMillis() + 20000);
        note.setLatestEventInfo(this, "Too many prime-time calls", "42m for 19d remain. (+97min?)", pendingSeeStats);


        notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notMan.notify(13, note);


    }

    /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG,"onDestroy");
    }
    */

    /**
     * The IAdderService is defined through IDL
     */
    private final ILogInfoService.Stub binder = new ILogInfoService.Stub() {
        public int getLogInfoCount() { return counter; }
    };

    class InterpretLogChanges extends ContentObserver {
        private final String TAG = "InterpretLogChanges";

        InterpretLogChanges(Handler handler) {
            super(handler);
        }

        public void onChange(boolean thisChanged) {
            Log.d(TAG, "onChange!  Money!");
        }
    };
}
/* vim: set et ai sta : */
