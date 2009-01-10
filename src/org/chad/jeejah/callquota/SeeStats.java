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

    private Configuration configuration = new Configuration();
    private UsageData usageData;


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        this.configuration = new Configuration();
        this.configuration.load(this);

        Intent i = new Intent();
        i.setClassName( "org.chad.jeejah.callquota", "org.chad.jeejah.callquota.LogMonitorService" );
        startService( i );

        if (this.configuration.runUnitTestsP) {
            AndroidRunner runner = new AndroidRunner(new SoloRunner());
            runner.run(this.configuration.meteringRulesClass);
        }

        setContentView(R.layout.main);

        this.usageData = new UsageData(this, this.configuration);
        this.usageData.scanLog(true);

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

        this.configuration.refresh();

        int firstBillDay = this.configuration.firstBillDay;

        SimpleDateFormat sdf = new SimpleDateFormat(this.configuration.dateFormatString);
        TextView description = (TextView) findViewById(R.id.description);
        description.setText(
                String.format(
                    getResources().getString(R.string.vis_summary), 
                    this.usageData.usedTotalMeteredMinutes, // 1
                    this.usageData.usedTotalMinutes, // 2
                    sdf.format(new Date(this.configuration.meteringRules.getEndOfNthBillBackAsMs(1, firstBillDay))), // 3
                    sdf.format(new Date(this.configuration.meteringRules.getEndOfNthBillBackAsMs(0, firstBillDay))), // 4
                    this.usageData.callList.length // 5
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

        /*
        mi = menu.add(Menu.NONE, 3, Menu.NONE, "About");
        mi.setIcon(android.R.drawable.ic_menu_info_details);
        */

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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

/* vim: set et ai sta : */
