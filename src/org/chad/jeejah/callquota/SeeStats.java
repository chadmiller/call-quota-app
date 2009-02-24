package org.chad.jeejah.callquota;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.util.Log;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AlphaAnimation;
import android.widget.ScrollView;
import android.content.Intent;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.chad.jeejah.callquota.Call;

public class SeeStats extends Activity //implements View.OnClickListener
{
    private static final String TAG = "CallQuota.SeeStats";

    private Visualization viz;

    private Configuration configuration;
    private UsageData usageData;
    private NotificationManager notMan;

    private View prev;
    private View next;
    private Animation mHideNextImageViewAnimation = new AlphaAnimation(1F, 0F);
    private Animation mHidePrevImageViewAnimation = new AlphaAnimation(1F, 0F);
    private Animation mShowNextImageViewAnimation = new AlphaAnimation(0F, 1F);
    private Animation mShowPrevImageViewAnimation = new AlphaAnimation(0F, 1F);
    private int nthMonthsBack;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        CallQuotaApplication app = (CallQuotaApplication) getApplication();
        this.configuration = app.conf();
        nthMonthsBack = 0;

        Intent i = new Intent();
        i.setClassName("org.chad.jeejah.callquota", "org.chad.jeejah.callquota.LogMonitorService");
        startService(i);

        if (! this.configuration.isConfigured()) {
            startActivityForResult(new Intent(this, Overview.class), 1);
        }

        setContentView(R.layout.main);

        prev = (View) findViewById(R.id.prev_image);
        next = (View) findViewById(R.id.next_image);

        this.usageData = app.usage(nthMonthsBack);

        viz = new Visualization(this, this.configuration, this.usageData, getWindowManager().getDefaultDisplay());

        ViewGroup root = (ViewGroup) findViewById(R.id.root);
        root.addView(viz, 1);
        updatePrevNext();
    }

    private void updatePrevNext() {
        /*
        boolean prevVisible = (prev.getVisibility() == View.VISIBLE);
        boolean nextVisible = (next.getVisibility() == View.VISIBLE);

        if (nthMonthsBack < 12) {
            if (! prevVisible) {
                Animation a = mShowPrevImageViewAnimation;
                a.setDuration(500);
                a.startNow();
                prev.setAnimation(a);
                prev.setVisibility(View.VISIBLE);
            }
        } else {
            if (prevVisible) {
                Animation a = mHidePrevImageViewAnimation;
                a.setDuration(500);
                a.startNow();
                prev.setAnimation(a);
                prev.setVisibility(View.INVISIBLE);
            }
        }

        if (nthMonthsBack > 0) {
            if (! nextVisible) {
                Animation a = mShowNextImageViewAnimation;
                a.setDuration(500);
                a.startNow();
                next.setAnimation(a);
                next.setVisibility(View.VISIBLE);
            }
        } else {
            if (nextVisible) {
                Animation a = mHideNextImageViewAnimation;
                a.setDuration(500);
                a.startNow();
                next.setAnimation(a);
                next.setVisibility(View.INVISIBLE);
            }
        }
        */
    }

    private void showArrows() {
        updatePrevNext();
        scheduleDismissOnScreenControls();
    }

    private void scheduleDismissOnScreenControls() {
        /*
        mHandler.removeCallbacks(mDismissOnScreenControlsRunnable);
        mHandler.postDelayed(mDismissOnScreenControlsRunnable, 1500);
        */
    }

    /** Called when the activity is first created. */
    @Override

    public void onResume()
    {
        super.onResume();

        NotificationManager notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notMan.cancelAll();
        int firstBillDay = this.configuration.getFirstBillDay();

        SimpleDateFormat sdf = new SimpleDateFormat(this.configuration.getDateFormatString());
        TextView description = (TextView) findViewById(R.id.description);
        String billStart = sdf.format(new Date(this.usageData.getBeginningOfPeriodAsMs()+1000));
        String billEnd = sdf.format(new Date(this.usageData.getEndOfPeriodAsMs()));
        boolean wrotePrediction = false;
        try {
            if (usageData.getIsSufficientDataToPredictP()) {
                description.setText(getResources().getString(
                        R.string.vis_summary, 
                        this.usageData.getUsedTotalMeteredMinutes(), // 1
                        this.usageData.getUsedTotalMinutes(), // 2
                        billStart, // 3
                        billEnd, // 4
                        this.usageData.getCallList().size(), // 5
                        this.configuration.getBillAllowedMeteredMinutes(), // 6
                        this.usageData.getPredictionAtBillMinutes() // 7
                    ));
                wrotePrediction = true;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        }

        if (! wrotePrediction)
            description.setText(getResources().getString(
                    R.string.vis_summary_no_prediction, 
                    this.usageData.getUsedTotalMeteredMinutes(), // 1
                    this.usageData.getUsedTotalMinutes(), // 2
                    billStart, // 3
                    billEnd, // 4
                    this.usageData.getCallList().size(), // 5
                    this.configuration.getBillAllowedMeteredMinutes() // 6
                ));



        TextView rules = (TextView) findViewById(R.id.rules);
        rules.setText("Assuming local time zone for bill; weekends are " + 
                (this.configuration.getMeteringOmitsWeekends() ? "free" : "metered") + "; nights are " +
                (! this.configuration.getWantNightsFree() ? "metered" : 
                    (String.format("free after %d o'clock and before %d o'clock", this.configuration.getDaytimeEndHour(), this.configuration.getDaytimeBeginningHour()))
                    ) + ".");

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        MenuItem mi;
        mi = menu.add(Menu.NONE, 1, Menu.NONE, "Configure");
        mi.setIcon(android.R.drawable.ic_menu_preferences);

        mi = menu.add(Menu.NONE, 2, Menu.NONE, "Help");
        mi.setIcon(android.R.drawable.ic_menu_help);

        mi = menu.add(Menu.NONE, 3, Menu.NONE, "Audit");
        mi.setIcon(android.R.drawable.ic_menu_agenda);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case 1:
            startActivityForResult(new Intent(this, Pref.class), 1);
            return true;
        case 2:
            startActivity(new Intent(this, Help.class));
            return true;
        case 3:
            startActivity(new Intent(this, Audit.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1) {
            configuration.invalidate();
            usageData.invalidate();
        }
    }

//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
//        }
//    }
        
}

/* vim: set et ai sta : */
