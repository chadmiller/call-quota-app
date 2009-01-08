package org.chad.jeejah.callquota;

import android.util.Log;
import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.content.Context;
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

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG,"onBind()   UNIMPLEMENTED!");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG,"onCreate");

        Configuration configuration = new Configuration();
        configuration.load(this);

        serviceHandler = new Handler();
        ContentResolver contentResolver = getContentResolver();
        ContentObserver observer = new InterpretLogChanges(serviceHandler, this, configuration);
        contentResolver.registerContentObserver(Calls.CONTENT_URI, true, observer);
    }

    class InterpretLogChanges extends ContentObserver {
        private final String TAG = "InterpretLogChanges";
        Context context;
        Configuration configuration;

        InterpretLogChanges(Handler handler, Context context, Configuration configuration) {
            super(handler);
            this.context = context;
            this.configuration = configuration;
        }

        public void onChange(boolean thisChanged) {
            Log.d(TAG, "onChange!  Money!");

            UsageData usageData = new UsageData(context, configuration);
            usageData.scanLog(false);

            Intent seeStats = new Intent();
            seeStats.setClassName("org.chad.jeejah.callquota", "org.chad.jeejah.callquota.SeeStats");
            PendingIntent pendingSeeStats = PendingIntent.getActivity(context, 0, seeStats, 0);

            if (usageData.usedTotalMeteredMinutes > configuration.billAllowedMeteredMinutes) {
                note = new Notification(R.drawable.cost_notification, context.getResources().getString(R.string.notification_overage_occurred_slug), java.lang.System.currentTimeMillis());
                note.setLatestEventInfo(
                        context, 
                        context.getResources().getString(R.string.notification_overage_occurred_title), 
                        String.format(
                            context.getResources().getString(R.string.notification_overage_occurred_description), 
                            usageData.usedTotalMeteredMinutes,
                            usageData.usedTotalMinutes,
                            configuration.billAllowedMeteredMinutes
                        ), 
                        pendingSeeStats);

            } else if (usageData.predictionAtBillMinutes > ((configuration.warningPercentage / 100.0) * configuration.billAllowedMeteredMinutes)) {
                note = new Notification(R.drawable.cost_notification, context.getResources().getString(R.string.notification_overage_prediction_slug), java.lang.System.currentTimeMillis());
                note.setLatestEventInfo(
                        context, 
                        context.getResources().getString(R.string.notification_overage_prediction_title), 
                        String.format(
                            context.getResources().getString(R.string.notification_overage_prediction_description), 
                            usageData.usedTotalMeteredMinutes,
                            usageData.usedTotalMinutes,
                            configuration.billAllowedMeteredMinutes
                        ), 
                        pendingSeeStats);

            }

            if (note != null) {
                notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notMan.notify(13, note);
            }

        }
    };
}
/* vim: set et ai sta : */
