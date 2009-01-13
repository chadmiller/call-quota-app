package org.chad.jeejah.callquota;

import android.app.Activity;
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

import org.punit.runner.SoloRunner;
import org.punit.runner.AndroidRunner;

import org.chad.jeejah.callquota.Call;

public class SeeStats extends Activity
{
    private static final String TAG = "SeeStats";

    private Visualization viz;

    private Configuration configuration;
    private UsageData usageData;


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

        viz = new Visualization(this, this.configuration, this.usageData);

        ViewGroup root = (ViewGroup) findViewById(R.id.root);
        root.addView(viz, 1);

    }


    /** Called when the activity is first created. */
    @Override

    public void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        int firstBillDay = this.configuration.getFirstBillDay();

        SimpleDateFormat sdf = new SimpleDateFormat(this.configuration.getDateFormatString());
        TextView description = (TextView) findViewById(R.id.description);
        description.setText(
                String.format(
                    getResources().getString(R.string.vis_summary), 
                    this.usageData.getUsedTotalMeteredMinutes(), // 1
                    this.usageData.getUsedTotalMinutes(), // 2
                    sdf.format(new Date(this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(1, firstBillDay))), // 3
                    sdf.format(new Date(this.configuration.getMeteringRules().getEndOfNthBillBackAsMs(0, firstBillDay))), // 4
                    this.usageData.getCallList().length // 5
                )
            );

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

        mi = menu.add(Menu.NONE, 3, Menu.NONE, "Costly Contacts");
        mi.setIcon(android.R.drawable.ic_menu_info_details);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case 1:
            startActivity(new Intent(this, Pref.class));
			return true;
		case 2:
            startActivity(new Intent(this, Help.class));

            //AndroidRunner runner = new AndroidRunner(new SoloRunner());
            //runner.run(Metering.class);
            //runner.run(Call.class);

			return true;
		case 3:
            startActivity(new Intent(this, ShowCostly.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

/* vim: set et ai sta : */
