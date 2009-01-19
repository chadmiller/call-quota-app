package org.chad.jeejah.callquota;

import android.util.Log;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableRow;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Audit extends Activity {
    private static final String TAG = "CallQuota.Audit";
    UsageData usageData;
    Configuration configuration;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        CallQuotaApplication app = (CallQuotaApplication) getApplication();
        this.usageData = app.usage();
        this.configuration = app.conf();
    }


    @Override
    public void onResume() {
        super.onResume();
        SimpleDateFormat sdf = new SimpleDateFormat(this.configuration.getDateTimeFormatString());

        TableLayout root = new TableLayout(this);
        root.setStretchAllColumns(true);

        for (Call c: usageData.getCallList()) {
            TableRow tr = new TableRow(this);
            tr.setPadding(0, 2, 0, 2);

            TextView date = new TextView(this); date.setText(sdf.format(new Date(c.beginningFromEpochSec*1000))); tr.addView(date);
            TextView number = new TextView(this); number.setText(c.caller); tr.addView(number);
            TextView meteredMinutes = new TextView(this); meteredMinutes.setText(String.format("%d", c.meteredMinutes)); tr.addView(meteredMinutes);
            TextView reason = new TextView(this); reason.setText(c.reasonForRate); tr.addView(reason);

            root.addView(tr);
        }

        setContentView(root);

        Log.d(TAG, "onCreate() ran activity");
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
        MenuItem mi;
        mi = menu.add(Menu.NONE, 1, Menu.NONE, "Query Carrier");
        mi.setIcon(android.R.drawable.ic_menu_help);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case 1:
            startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", "#646#", null)));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
/* vim: set et ai sta : */
