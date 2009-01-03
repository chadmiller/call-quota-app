package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.view.View;
import android.graphics.*;

public class Visualization extends View {
    private final String TAG = "Visualization";
    private static final int SIZE = 300;

    private Paint   mPaint = new Paint();
    private Path    mPath = new Path();

	Context context;
    Configuration configuration;

    public Visualization(Context context, Configuration configuration) {
        super(context);

		this.context = context;
        this.configuration = configuration;

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

        Call[] snapshotCallData = null; // UsageData.getCalls(configuration, context);    FIXME NOW CHAD
        long nowSec = java.lang.System.currentTimeMillis() / 1000;

        if (snapshotCallData.length > 0) {
            //graphEndOfTimeSec = snapshotCallData[snapshotCallData.length-1].endFromEpochSec + 386000;  // FIXME When is end?
            graphEndOfTimeSec = nowSec + 386000;  // FIXME When is end?
            graphBeginningOfTimeSec = snapshotCallData[0].beginningFromEpochSec;

            Log.d(TAG, "graphEndOfTimeSec " + Long.toString(graphEndOfTimeSec));
            Log.d(TAG, "graphBeginningOfTimeSec " + Long.toString(graphBeginningOfTimeSec));

        }

        float x = 0, y = 0;
        double pixelsPerSecondH = (double) SIZE / (graphEndOfTimeSec - graphBeginningOfTimeSec);
        double pixelsPerMinuteV = (double) SIZE / (configuration.billAllowedMeteredMinutes * 1.2);
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
            p.moveTo(0,    SIZE-(float)(configuration.billAllowedMeteredMinutes * pixelsPerMinuteV));
            p.lineTo(SIZE, SIZE-(float)(configuration.billAllowedMeteredMinutes * pixelsPerMinuteV));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawTextOnPath(Long.toString(configuration.billAllowedMeteredMinutes) + " minutes in plan", p, 0, -3, paint);
        }


        long meteredMinutesCount = 0;
        for (Call cd: snapshotCallData) {
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

            if (meteredMinutesCount > configuration.billAllowedMeteredMinutes)
                paint.setColor(Color.RED);
            else if (cd.meteredMinutes != 0)
                paint.setColor(Color.BLUE);
            else
                paint.setColor(Color.LTGRAY);

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);
            if (cd.meteredMinutes > (configuration.billAllowedMeteredMinutes / 10)) {
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
            if (prediction > configuration.billAllowedMeteredMinutes)
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
            if (prediction > configuration.billAllowedMeteredMinutes) {
                canvas.drawText("Based on your usage so far, you will exceed your plan by " + Long.toString((long) prediction - configuration.billAllowedMeteredMinutes), SIZE/2, SIZE+60, paint);
                canvas.drawText("minutes.", SIZE/2, SIZE+80, paint);
            } else {
                canvas.drawText("Based on your usage so far, you will have " + Long.toString(configuration.billAllowedMeteredMinutes - (long) prediction), SIZE/2, SIZE+60, paint);
                canvas.drawText("minutes left unused.", SIZE/2, SIZE+80, paint);
            }

        }

    }

}
/* vim: set et ai sta : */
