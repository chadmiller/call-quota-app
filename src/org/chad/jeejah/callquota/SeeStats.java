package org.chad.jeejah.callquota;

import android.app.Activity;
import android.os.Bundle;

import android.util.Log;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.content.Intent;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.punit.runner.SoloRunner;
import org.punit.runner.AndroidRunner;

import org.chad.jeejah.callquota.Call;

public class SeeStats extends Activity
{
    private static final String TAG = "SeeStats";

    private Visualization viz;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        Intent i = new Intent();
        i.setClassName( "org.chad.jeejah.callquota", "org.chad.jeejah.callquota.LogMonitorService" );
        startService( i );


        Configuration configuration = new Configuration();
        configuration.load(this);

        if (configuration.runUnitTestsP) {
            AndroidRunner runner = new AndroidRunner(new SoloRunner());
            runner.run(configuration.meteringRulesClass);
        }

        setContentView(R.layout.main);

        UsageData usageData = new UsageData(this, configuration);
        usageData.scanLog(true);

        viz = new Visualization(this, configuration, usageData);

        ViewGroup root = (ViewGroup) findViewById(R.id.root);
        root.addView(viz, 1);

        SimpleDateFormat sdf = new SimpleDateFormat(configuration.dateFormatString);
        TextView description = (TextView) findViewById(R.id.description);
        description.setText(
                String.format(
                    getResources().getString(R.string.vis_summary), 
                    usageData.usedTotalMeteredMinutes, // 1
                    usageData.usedTotalMinutes, // 2
                    sdf.format(new Date(configuration.meteringRules.getEndOfNthBillBackAsMs(1))), // 3
                    sdf.format(new Date(configuration.meteringRules.getEndOfNthBillBackAsMs(0))), // 4
                    usageData.callList.length // 5
                )
            );
    }


    /** Called when the activity is first created. */
    @Override

    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        TextView tv = (TextView) findViewById(R.id.description);
        if (tv != null) {
            //getMeteredMinuteCount();
            //tv.setText("metered minutes logged: " + Long.toString(usage.minutesUsedThisBillingPeriod));
        } else {
            Log.e(TAG, "view not found!");
        }


    }

    /*
    @Override
    protected void onStop(){
       super.onStop();
    }
    */



}

/* vim: set et ai sta : */
