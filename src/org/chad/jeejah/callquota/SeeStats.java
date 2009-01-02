package org.chad.jeejah.callquota;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.content.SharedPreferences;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.graphics.*;
import android.database.Cursor;
import android.provider.CallLog.Calls;

import java.util.Date;

import org.punit.runner.SoloRunner;
import org.punit.runner.AndroidRunner;

import org.chad.jeejah.callquota.carrier.AllMetered;

public class SeeStats extends Activity
{
    private static final String TAG = "SeeStats";
    private static final String PREFS_NAME = "CallQuota.root";

    private static String[] projection = { Calls.DATE, Calls.DURATION, Calls.TYPE, Calls.NUMBER };

    private Cursor managedCursor;
    private AllMetered counter;
    private Visualization viz;

    private int warningPercentage;
    private long allowedMeteredMinutes;

    private class CallData {
        public final long beginningFromEpochSec;
        public final long endFromEpochSec;
        public final long meteredMinutes;
        public final boolean firstAfterReset;

        CallData(long beginningFromEpochSec, long endFromEpochSec, long meteredMinutes, boolean firstAfterReset) {
            this.beginningFromEpochSec = beginningFromEpochSec;
            this.endFromEpochSec = endFromEpochSec;
            this.meteredMinutes = meteredMinutes;
            this.firstAfterReset = firstAfterReset;
        }
    };

    private CallData[] cachedCallData = null;

    private class UsageData {
        public String logEpoch;
        public Long minutesUsedIn30Days;
        public Long minutesUsedThisBillingPeriod;
    };

    public long getMeteredMinuteCount() { 

        managedCursor.requery();

        long meteredMinutesCount = 0;
        CallData[] newCallData = null;

        newCallData = new CallData[managedCursor.getCount()];
        if (managedCursor.moveToFirst()) {
            int dateColumn = managedCursor.getColumnIndex(Calls.DATE); 
            int durationColumn = managedCursor.getColumnIndex(Calls.DURATION);
            int typeColumn = managedCursor.getColumnIndex(Calls.TYPE);
            int numberColumn = managedCursor.getColumnIndex(Calls.NUMBER);

            String phoneNumber; 
            UsageData usage;
            //GregorianCalendar thisCall, lastCall = null;

            int i = 0;
            do {
                long dateInMs, durationSeconds;
                int type;
                String number;

                dateInMs = managedCursor.getLong(dateColumn);
                durationSeconds = managedCursor.getLong(durationColumn);
                type = managedCursor.getInt(typeColumn);
                number = managedCursor.getString(numberColumn);

                //thisCall.setTimeInMillis(dateInMs);

                long meteredMinutes = counter.extractMeteredSeconds(dateInMs, durationSeconds, number, type) / 60;

                assert(newCallData.length < i);
                newCallData[i] = new CallData(dateInMs / 1000, (dateInMs / 1000) + (durationSeconds / 60), meteredMinutes, false);
                meteredMinutesCount += meteredMinutes;

                //lastCall = thisCall;
                i++;
            } while (managedCursor.moveToNext());

        } else {
            Log.d(TAG, "The provider is empty.  That's okay.");
        }

        cachedCallData = newCallData;
        return meteredMinutesCount;
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        managedCursor = managedQuery(Calls.CONTENT_URI, projection, null, null, null);
        setContentView(R.layout.main);

        viz = new Visualization(this);

        ViewGroup root = (ViewGroup) findViewById(R.id.root);

        /*
        ScrollView sv = new ScrollView(this);
        sv.addView(viz);

        root.addView(sv, 1);
        */
        root.addView(viz, 1);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        // create meter
        String confMeterName = settings.getString("meteringRules", "HoursRestricted");
        Class meterClass = null;
        try {
            meterClass = Class.forName("org.chad.jeejah.callquota.carrier." + confMeterName);
            counter = (AllMetered) meterClass.newInstance();
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "ClassNotFoundException");
        } catch (IllegalAccessException e) {
            Log.d(TAG, "IllegalAccessException");
        } catch (InstantiationException e) {
            Log.d(TAG, "InstantiationException");
        }

        // should we test it?
        Boolean confRunUnitTests = settings.getBoolean("runUnitTests", false);
        if (confRunUnitTests) {
            AndroidRunner runner = new AndroidRunner(new SoloRunner());
            runner.run(meterClass);
        }

        // should we test it?
        allowedMeteredMinutes = settings.getLong("allowedMeteredMinutes", 400);

    }


    /** Called when the activity is first created. */
    @Override
    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        TextView tv = (TextView) findViewById(R.id.description);
        if (tv != null) {
            getMeteredMinuteCount();
            //tv.setText("metered minutes logged: " + Long.toString(usage.minutesUsedThisBillingPeriod));
        } else {
            Log.e(TAG, "view not found!");
        }


    }

    @Override
    protected void onStop(){
       super.onStop();
    
      // Save user preferences. We need an Editor object to
      // make changes. All objects are from android.context.Context
      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
      SharedPreferences.Editor editor = settings.edit();
      //editor.putString("meteringRules", counter.name);

      // Don't forget to commit your edits!!!
      editor.commit();
    }


    public class Visualization extends View {
        private final String TAG = "Visualization";
        private static final int SIZE = 300;

        private Paint   mPaint = new Paint();
        private Path    mPath = new Path();


        public Visualization(Context context) {
            super(context);

            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Log.d(TAG, "onDraw()");
            long graphBeginningOfTimeSec = 0;
            long graphEndOfTimeSec = 0;

            Paint paint = mPaint;
            paint.setPathEffect(null);

            canvas.translate(10, 10);

            paint.setStrokeWidth(0);

            if (cachedCallData == null) {
                // fill it.
            }
            assert(cachedCallData != null);

            CallData[] snapshotCallData = cachedCallData;
            long nowSec = (new Date()).getTime() / 1000;

            if (snapshotCallData.length > 0) {
                //graphEndOfTimeSec = snapshotCallData[snapshotCallData.length-1].endFromEpochSec + 386000;  // FIXME When is end?
                graphEndOfTimeSec = nowSec + 386000;  // FIXME When is end?
                graphBeginningOfTimeSec = snapshotCallData[0].beginningFromEpochSec;

                Log.d(TAG, "graphEndOfTimeSec " + Long.toString(graphEndOfTimeSec));
                Log.d(TAG, "graphBeginningOfTimeSec " + Long.toString(graphBeginningOfTimeSec));

            }

            float x = 0, y = 0;
            double pixelsPerSecondH = (double) SIZE / (graphEndOfTimeSec - graphBeginningOfTimeSec);
            double pixelsPerMinuteV = (double) SIZE / (allowedMeteredMinutes * 1.2);
            Log.d(TAG, "pixelsPerSecondH " + Double.toString(pixelsPerSecondH));
            Log.d(TAG, "pixelsPerMinuteV " + Double.toString(pixelsPerMinuteV));

            {
                paint.setARGB(0x33, 0xCC, 0xCC, 0x00);
                Path p = new Path();
                p.moveTo((float)((nowSec - graphBeginningOfTimeSec) * pixelsPerSecondH), SIZE);
                p.lineTo((float)((nowSec - graphBeginningOfTimeSec) * pixelsPerSecondH), 0);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawTextOnPath("now", p, 0, -3, paint);
            }

            {
                paint.setARGB(0x33, 0xCC, 0xCC, 0x00);
                Path p = new Path();
                p.moveTo(SIZE, SIZE);
                p.lineTo(SIZE, 0);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawTextOnPath("bill", p, 0, -3, paint);
            }

            {
                paint.setARGB(0x99, 0xCC, 0x00, 0x00);
                Path p = new Path();
                p.moveTo(0,    SIZE-(float)(allowedMeteredMinutes * pixelsPerMinuteV));
                p.lineTo(SIZE, SIZE-(float)(allowedMeteredMinutes * pixelsPerMinuteV));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawTextOnPath(Long.toString(allowedMeteredMinutes) + " minutes in plan", p, 0, -3, paint);
            }


            long meteredMinutesCount = 0;
            for (CallData cd: snapshotCallData) {
                Path path = new Path();
                // if last is not null,
                //     if this month is not same as last month, 
                //         reset 

                x = (float)((cd.beginningFromEpochSec-graphBeginningOfTimeSec) * pixelsPerSecondH);
                y = (float)(SIZE-(meteredMinutesCount*pixelsPerMinuteV));
                path.moveTo(x, y);

                meteredMinutesCount += cd.meteredMinutes;

                x = (float)((cd.endFromEpochSec      -graphBeginningOfTimeSec) * pixelsPerSecondH) + 1;
                y = (float)(SIZE-(meteredMinutesCount*pixelsPerMinuteV));
                path.lineTo(x, y);

                if (meteredMinutesCount > allowedMeteredMinutes)
                    paint.setColor(Color.RED);
                else if (cd.meteredMinutes != 0)
                    paint.setColor(Color.BLUE);
                else
                    paint.setColor(Color.LTGRAY);

                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(path, paint);
                if (cd.meteredMinutes > (allowedMeteredMinutes / 10)) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawTextOnPath(Long.toString(cd.meteredMinutes), path, 0, -3, paint);
                }
            }

            if (snapshotCallData.length >= 2) {
                Path p = new Path();

                p.moveTo(x, y);

                long finalPointSec = snapshotCallData[snapshotCallData.length-1].endFromEpochSec;

                
                long periodLength = finalPointSec - graphBeginningOfTimeSec;
                long growthInPeriod = meteredMinutesCount;

                double growthRate = (double) growthInPeriod / periodLength;

                long predictionPoint = graphEndOfTimeSec;

                long predictionPeriod = graphEndOfTimeSec - graphBeginningOfTimeSec;

                double prediction = growthRate * predictionPeriod;

                x = SIZE;
                y = (float) (SIZE - (prediction * pixelsPerMinuteV));

                p.lineTo(x, y);
                if (prediction > allowedMeteredMinutes)
                    paint.setColor(Color.RED);
                else
                    paint.setColor(Color.LTGRAY);

                paint.setStrokeWidth(2);
                paint.setPathEffect(new DashPathEffect(new float[] { 2, 4 }, 3));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setPathEffect(null);
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawTextOnPath("predicted used: " + Long.toString((long) prediction), p, 0, -3, paint);


                paint.setTextAlign(Paint.Align.CENTER);
                if (prediction > allowedMeteredMinutes) {
                    canvas.drawText("Based on your usage so far, you will exceed your plan by " + Long.toString((long) prediction -allowedMeteredMinutes), SIZE/2, SIZE+60, paint);
                    canvas.drawText("minutes.", SIZE/2, SIZE+80, paint);
                } else {
                    canvas.drawText("Based on your usage so far, you will have " + Long.toString(allowedMeteredMinutes - (long) prediction), SIZE/2, SIZE+60, paint);
                    canvas.drawText("minutes left unused.", SIZE/2, SIZE+80, paint);
                }

            }

        }

    }

}

/* vim: set et ai sta : */
