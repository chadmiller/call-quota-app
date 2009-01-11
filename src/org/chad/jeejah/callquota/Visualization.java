package org.chad.jeejah.callquota;

import android.util.Log;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.Toast;

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
        int firstBillDay = this.configuration.getFirstBillDay();

        long graphBeginningOfTimeSec = this.usageData.getBeginningOfPeriodAsMs() / 1000;
        long graphEndOfTimeSec = this.usageData.getEndOfPeriodAsMs()/ 1000;

        Resources res = getResources();

        Paint paint = mPaint;
        paint.setPathEffect(null);

        canvas.translate(10, 10);

        paint.setStrokeWidth(0);

        long nowSec = java.lang.System.currentTimeMillis() / 1000;

        if (((float)(nowSec - graphBeginningOfTimeSec) / (float)(graphEndOfTimeSec - graphBeginningOfTimeSec)) < 0.2) {
            Toast t = Toast.makeText(this.context, R.string.data_too_short_to_trend, Toast.LENGTH_LONG);
            t.show();
        }

        float x = 0, y = 0;
        double pixelsPerSecondH = (double) SIZE / (graphEndOfTimeSec - graphBeginningOfTimeSec);

        long prediction;
        try {
            prediction = usageData.getPredictionAtBillMinutes();
        } catch (ArrayIndexOutOfBoundsException e) {
            prediction = 0;
        }
        double pixelsPerMinuteV = (double) SIZE / (Math.max(
                configuration.getBillAllowedMeteredMinutes(),
                prediction
                ) * 1.1);

        {
            paint.setColor(res.getColor(R.drawable.vis_bill_graph_baseline));
            Path p = new Path();
            p.moveTo(0,    SIZE);
            p.lineTo(SIZE, SIZE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
        }

        {
            paint.setColor(res.getColor(R.drawable.vis_now_date));
            Path p = new Path();
            p.moveTo((float)((nowSec - graphBeginningOfTimeSec) * pixelsPerSecondH), SIZE);
            p.lineTo((float)((nowSec - graphBeginningOfTimeSec) * pixelsPerSecondH), 0);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawTextOnPath(getResources().getString(R.string.label_now), p, 0, -3, paint);
        }

        {
            paint.setColor(res.getColor(R.drawable.vis_bill_date));
            Path p = new Path();
            p.moveTo(SIZE, SIZE);
            p.lineTo(SIZE, 0);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawTextOnPath(getResources().getString(R.string.label_bill), p, 0, -3, paint);
        }

        {
            paint.setColor(res.getColor(R.drawable.vis_limit_time));
            Path p = new Path();
            p.moveTo(0,    SIZE-(float)(configuration.getBillAllowedMeteredMinutes() * pixelsPerMinuteV));
            p.lineTo(SIZE, SIZE-(float)(configuration.getBillAllowedMeteredMinutes() * pixelsPerMinuteV));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawTextOnPath(String.format(res.getString(R.string.vis_limit_description), configuration.getBillAllowedMeteredMinutes()), p, 0, -3, paint);
        }

        long meteredMinutesCount = 0;
        y = (float)SIZE;
        for (Call cd: this.usageData.getCallList()) {
            Path path = new Path();

            x = (float)((cd.beginningFromEpochSec-graphBeginningOfTimeSec) * pixelsPerSecondH);
            path.moveTo(x, y);

            meteredMinutesCount += cd.meteredMinutes;
            double screenDistanceBilledTime = pixelsPerMinuteV * cd.meteredMinutes;

            x = (float)((cd.endFromEpochSec      -graphBeginningOfTimeSec) * pixelsPerSecondH) + 1;

            y -= screenDistanceBilledTime;
            path.lineTo(x, y);

            if (meteredMinutesCount > configuration.getBillAllowedMeteredMinutes())
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_call_over));
            else if (cd.meteredMinutes != 0)
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_call_more));
            else
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_call_nochange));

            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(path, paint);

            if (screenDistanceBilledTime >= paint.measureText(Long.toString(cd.meteredMinutes) + "M")) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawTextOnPath(Long.toString(cd.meteredMinutes), path, 0, -3, paint);
            }
        }

        try {
            Path p = new Path();

            p.moveTo(x, y);

            prediction = usageData.getPredictionAtBillMinutes();

            x = SIZE;
            y = (float) (SIZE - (prediction * pixelsPerMinuteV));

            p.lineTo(x, y);
            if (prediction > configuration.getBillAllowedMeteredMinutes())
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_prediction_over));
            else
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_prediction_under));

            paint.setStrokeWidth(2);
            paint.setPathEffect(new DashPathEffect(new float[] { 2, 4 }, 3));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(p, paint);
            paint.setPathEffect(null);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.RIGHT);

            canvas.drawText(String.format(res.getString(R.string.vis_prediction_description), prediction), x, Math.max(10, (int)y-3), paint);

        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "Can't make prediction with no data available.");
        }

    }

}
/* vim: set et ai sta : */
