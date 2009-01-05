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

	private Context context;
    private Configuration configuration;
    private UsageData usageData;

    public Visualization(Context context, Configuration configuration, UsageData usageData) {
        super(context);

		this.context = context;
        this.configuration = configuration;
        this.usageData = usageData;

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw()");
        long graphBeginningOfTimeSec = configuration.meteringRules.getEndOfNthBillBackAsMs(1) / 1000;
        long graphEndOfTimeSec = configuration.meteringRules.getEndOfNthBillBackAsMs(0) / 1000;

        Paint paint = mPaint;
        paint.setPathEffect(null);

        canvas.translate(10, 10);

        paint.setStrokeWidth(0);

        Call[] snapshotCallData = this.usageData.callList;
        long nowSec = java.lang.System.currentTimeMillis() / 1000;

        float x = 0, y = 0;
        double pixelsPerSecondH = (double) SIZE / (graphEndOfTimeSec - graphBeginningOfTimeSec);
        double pixelsPerMinuteV = (double) SIZE / (configuration.billAllowedMeteredMinutes * 1.2);

        {
            paint.setARGB(0x66, 0xCC, 0xCC, 0x00);
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
            paint.setARGB(0x66, 0xCC, 0xCC, 0x00);
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
            paint.setARGB(0xFF, 0xFF, 0x33, 0x33);
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
                paint.setColor(Color.LTGRAY);
            else
                paint.setARGB(0xCC, 0x33, 0xCC, 0x33);

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);
            if (cd.meteredMinutes > (configuration.billAllowedMeteredMinutes / 20)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawTextOnPath(Long.toString(cd.meteredMinutes), path, 0, -3, paint);
            }
        }

        if (snapshotCallData.length >= 2) {
            Path p = new Path();

            p.moveTo(x, y);

            x = SIZE;
            y = (float) (SIZE - (usageData.predictionAtBillMinutes * pixelsPerMinuteV));

            p.lineTo(x, y);
            if (usageData.predictionAtBillMinutes > configuration.billAllowedMeteredMinutes)
                paint.setColor(Color.RED);
            else
                paint.setColor(Color.YELLOW);

            paint.setStrokeWidth(2);
            paint.setPathEffect(new DashPathEffect(new float[] { 2, 4 }, 3));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setPathEffect(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);


            canvas.drawText("predicted used: " + Long.toString((long) usageData.predictionAtBillMinutes), x, y-3, paint);

            /*

            paint.setColor(Color.LTGRAY);
            paint.setTextAlign(Paint.Align.CENTER);
            if (usageData.predictionAtBillMinutes > configuration.billAllowedMeteredMinutes) {
                canvas.drawText("Based on your usage so far, you will exceed your plan by " + Long.toString((long) usageData.predictionAtBillMinutes - configuration.billAllowedMeteredMinutes), SIZE/2, SIZE+60, paint);
                canvas.drawText("minutes.", SIZE/2, SIZE+80, paint);
            } else {
                canvas.drawText("Based on your usage so far, you will have " + Long.toString(configuration.billAllowedMeteredMinutes - (long) usageData.predictionAtBillMinutes), SIZE/2, SIZE+60, paint);
                canvas.drawText("minutes left unused.", SIZE/2, SIZE+80, paint);
            }
            */

        }

    }

}
/* vim: set et ai sta : */
