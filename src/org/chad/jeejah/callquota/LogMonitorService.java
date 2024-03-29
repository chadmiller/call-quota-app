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
    private static final String TAG = "CallQuota.LogMonitorService";
    private Handler serviceHandler = null;
    private int counter;
    private NotificationManager notMan;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "creating service to listen for call-log changes.");
        serviceHandler = new Handler();
        ContentResolver contentResolver = getContentResolver();
        ContentObserver observer = new InterpretLogChanges(serviceHandler, this);
        contentResolver.registerContentObserver(Calls.CONTENT_URI, true, observer);
    }

    class InterpretLogChanges extends ContentObserver {
        private final String TAG = "InterpretLogChanges";
        Context context;

        InterpretLogChanges(Handler handler, Context context) {
            super(handler);

            this.context = context;
        }

        public void onChange(boolean thisChanged) {
            CallQuotaApplication app = (CallQuotaApplication) getApplication();
            Configuration configuration = app.conf();

            if (! configuration.getWantNotificationsP())
                return;

            UsageData usageData = app.usage(0, false);

            Intent seeStats = new Intent();
            seeStats.setClassName("org.chad.jeejah.callquota", "org.chad.jeejah.callquota.SeeStats");
            PendingIntent pendingSeeStats = PendingIntent.getActivity(context, 0, seeStats, 0);

            // FIXME refactor gBOTS gEOTS nS fBD
            int firstBillDay = configuration.getFirstBillDay();
            long nowMs = java.lang.System.currentTimeMillis();

            long graphBeginningOfTimeMs = configuration.getMeteringRules().getEndOfNthBillBackAsMs(1, firstBillDay);
            long graphEndOfTimeMs = configuration.getMeteringRules().getEndOfNthBillBackAsMs(0, firstBillDay);

            // FIXME move 0.2 into config
            if (((float)(nowMs - graphBeginningOfTimeMs) / (float)(graphEndOfTimeMs - graphBeginningOfTimeMs)) < 0.2)
                return;

            long allowedMin = configuration.getBillAllowedMeteredMinutes();

            Notification note = null;
            if (usageData.getUsedTotalMeteredMinutes() > allowedMin) {
                note = new Notification(R.drawable.cost_notification, context.getResources().getString(R.string.notification_overage_occurred_slug), java.lang.System.currentTimeMillis());
                note.setLatestEventInfo(
                        context, 
                        context.getResources().getString(R.string.notification_overage_occurred_title), 
                        String.format(
                            context.getResources().getString(R.string.notification_overage_occurred_description), 
                            usageData.getUsedTotalMeteredMinutes(),
                            usageData.getUsedTotalMinutes(),
                            allowedMin,
                            usageData.getPredictionAtBillMinutes()
                        ), 
                        pendingSeeStats);

            } else if (usageData.getPredictionAtBillMinutes() > ((configuration.getWarningPercentage() / 100.0) * allowedMin)) {
                note = new Notification(R.drawable.cost_notification, context.getResources().getString(R.string.notification_overage_prediction_slug), java.lang.System.currentTimeMillis());
                note.setLatestEventInfo(
                        context, 
                        context.getResources().getString(R.string.notification_overage_prediction_title), 
                        String.format(
                            context.getResources().getString(R.string.notification_overage_prediction_description), 
                            usageData.getUsedTotalMeteredMinutes(),
                            usageData.getUsedTotalMinutes(),
                            allowedMin,
                            usageData.getPredictionAtBillMinutes()
                        ), 
                        pendingSeeStats);

            }

            notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (note != null) {
                notMan.notify(13, note);
            } else {
                notMan.cancel(13);
            }

        }
    };
}
/* vim: set et ai sta : */
