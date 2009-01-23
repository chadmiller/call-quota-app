package org.chad.jeejah.callquota;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;

import android.util.Log;
import android.content.Context;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.content.Intent;

import java.util.Date;
import java.text.SimpleDateFormat;

import org.chad.jeejah.callquota.Call;

public class SeeStats extends Activity
{
    private static final String TAG = "CallQuota.SeeStats";

    private Visualization viz;

    private Configuration configuration;
    private UsageData usageData;
    private NotificationManager notMan;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        CallQuotaApplication app = (CallQuotaApplication) getApplication();
        this.configuration = app.conf();

        Intent i = new Intent();
        i.setClassName("org.chad.jeejah.callquota", "org.chad.jeejah.callquota.LogMonitorService");
        startService(i);

        setContentView(R.layout.main);

        this.usageData = app.usage();

        viz = new Visualization(this, this.configuration, this.usageData, getWindowManager().getDefaultDisplay());

        ViewGroup root = (ViewGroup) findViewById(R.id.root);
        root.addView(viz, 1);

    }


    /** Called when the activity is first created. */
    @Override

    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        NotificationManager notMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notMan.cancelAll();
        int firstBillDay = this.configuration.getFirstBillDay();

        SimpleDateFormat sdf = new SimpleDateFormat(this.configuration.getDateFormatString());
        TextView description = (TextView) findViewById(R.id.description);
        String billStart = sdf.format(new Date(this.usageData.getBeginningOfPeriodAsMs()));
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

    /*
    @Override
    protected void onStop(){
       super.onStop();
    }
    */

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
}

/* vim: set et ai sta : */
