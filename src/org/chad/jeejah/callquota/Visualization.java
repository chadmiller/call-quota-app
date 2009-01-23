package org.chad.jeejah.callquota;

import android.util.Log;
import android.util.TimingLogger;
import android.util.DisplayMetrics;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.Display;
import android.widget.Toast;

import android.graphics.*;

public class Visualization extends View {
    private final String TAG = "CallQuota.Visualization";

    private Paint   mPaint = new Paint();
    private Path    mPath = new Path();

    private Context context;
    private Configuration configuration;
    private UsageData usageData;
    private int graphWidth, graphHeight;
    private final static int padding = 2;

    public Visualization(Context context, Configuration configuration, UsageData usageData, Display display) {
        super(context);

        this.context = context;
        this.configuration = configuration;
        this.usageData = usageData;

        this.graphWidth = display.getWidth() - padding - padding;
        this.graphHeight = display.getHeight() - 160 - padding - padding;

        setMinimumHeight(this.graphHeight);
        setBackgroundResource(R.drawable.vis_background);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, this.graphHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        TimingLogger tl = new TimingLogger(TAG, "onDraw()");
        Log.d(TAG, "onDraw()");
        
        try {
            int firstBillDay = this.configuration.getFirstBillDay();

            long graphBeginningOfTimeSec = this.usageData.getBeginningOfPeriodAsMs() / 1000;
            long graphEndOfTimeSec = this.usageData.getEndOfPeriodAsMs()/ 1000;

            canvas.translate(padding, 0);

            Resources res = getResources();

            Paint paint = mPaint;
            paint.setPathEffect(null);

            paint.setStrokeWidth(0);

            long nowSec = java.lang.System.currentTimeMillis() / 1000;

            float x = 0, y = 0;
            double pixelsPerSecondH = (double) graphWidth / (graphEndOfTimeSec - graphBeginningOfTimeSec);

            long prediction = 0;
            double pixelsPerMinuteV;
            if (! this.usageData.getIsSufficientDataToPredictP()) {
                pixelsPerMinuteV = (double) graphHeight / configuration.getBillAllowedMeteredMinutes() * 1.1;
            } else {
                try {
                    prediction = usageData.getPredictionAtBillMinutes();
                    Log.d(TAG, "prediction = " + prediction);
                    pixelsPerMinuteV = (double) graphHeight / (Math.max(
                        configuration.getBillAllowedMeteredMinutes(),
                        prediction
                        ) * 1.1);
                } catch (ArrayIndexOutOfBoundsException e) {
                    pixelsPerMinuteV = (double) graphHeight / configuration.getBillAllowedMeteredMinutes() * 1.1;
                }
            }

            tl.addSplit("finished setup");

            {
                paint.setColor(res.getColor(R.drawable.vis_bill_graph_baseline));
                Path p = new Path();
                p.moveTo(0,    graphHeight);
                p.lineTo(graphWidth, graphHeight);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
            }

            {
                paint.setColor(res.getColor(R.drawable.vis_now_date));
                Path p = new Path();
                p.moveTo((float)((nowSec - graphBeginningOfTimeSec) * pixelsPerSecondH), graphHeight);
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
                p.moveTo(graphWidth, graphHeight);
                p.lineTo(graphWidth, 0);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setTextAlign(Paint.Align.RIGHT);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawTextOnPath(getResources().getString(R.string.label_bill), p, 0, -3, paint);
            }

            {
                paint.setColor(res.getColor(R.drawable.vis_limit_time));
                Path p = new Path();
                p.moveTo(0,    graphHeight-(float)(configuration.getBillAllowedMeteredMinutes() * pixelsPerMinuteV));
                p.lineTo(graphWidth, graphHeight-(float)(configuration.getBillAllowedMeteredMinutes() * pixelsPerMinuteV));
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(p, paint);
                paint.setTextAlign(Paint.Align.LEFT);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawTextOnPath(String.format(res.getString(R.string.vis_limit_description), configuration.getBillAllowedMeteredMinutes()), p, 0, -3, paint);
            }

            tl.addSplit("painted lines");

            long meteredMinutesCount = 0;
            y = (float)graphHeight;
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

            tl.addSplit("painted calls");


            if (this.usageData.getIsSufficientDataToPredictP()) {
                try {
                    Path p = new Path();

                    p.moveTo(x, y);

                    prediction = usageData.getPredictionAtBillMinutes();

                    x = graphWidth;
                    y = (float) (graphHeight - (prediction * pixelsPerMinuteV));

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
            } else {
                Toast t = Toast.makeText(this.context, R.string.data_too_short_to_trend, Toast.LENGTH_LONG);
                t.show();
            }
        } finally {
            tl.dumpToLog();
        }
    }

}
/* vim: set et ai sta : */
