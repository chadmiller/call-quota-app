package org.chad.jeejah.callquota;

import android.util.Log;
import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TableRow;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.telephony.PhoneNumberUtils;
import android.content.res.Resources;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class Audit extends Activity {
    private static final String TAG = "CallQuota.Audit";
    Configuration configuration;

    UsageData usageDataNow;
    UsageData usageDataPrev;
    private TableLayout tableNow;
    private TableLayout tablePrev;
    private TextView descriptionNow;
    private TextView descriptionPrev;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setTitle(R.string.title_audit);

        CallQuotaApplication app = (CallQuotaApplication) getApplication();
        this.configuration = app.conf();
        this.usageDataNow = app.usage(0);
        this.usageDataPrev = app.usage(1);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setVerticalFadingEdgeEnabled(true);

        this.descriptionPrev = new TextView(this);
        container.addView(this.descriptionPrev);

        this.tablePrev = new TableLayout(this);
        this.tablePrev.setStretchAllColumns(true);
        container.addView(this.tablePrev);

        this.descriptionNow = new TextView(this);
        container.addView(this.descriptionNow);

        this.tableNow = new TableLayout(this);
        this.tableNow.setStretchAllColumns(true);
        container.addView(this.tableNow);

        ScrollView scrollPane = new ScrollView(this);
        scrollPane.addView(container);
        setContentView(scrollPane);
    }


    @Override
    public void onResume() {
        super.onResume();

        fillTable(this.usageDataPrev, this.descriptionPrev, this.tablePrev);
        fillTable(this.usageDataNow, this.descriptionNow, this.tableNow);
    }

    private void fillTable(UsageData usageData, TextView description, TableLayout table) {

        boolean wroteLine = false;

        SimpleDateFormat dateFormat = new SimpleDateFormat(this.configuration.getDateFormatString());
        SimpleDateFormat datetimeFormat = new SimpleDateFormat(this.configuration.getDateTimeFormatString());

        table.removeAllViews();  // empty the table.

        Resources res = getResources();

        Map <String, Long>sumPerReason = new TreeMap<String, Long>();

        long sumMeteredCalls = 0;
        long sumAllCalls = 0;
        for (Call c: usageData.getCallList()) {
            wroteLine = true;

            TableRow tr = new TableRow(this);
            tr.setPadding(0, 2, 0, 2);

            long callLengthMin = new Double(Math.ceil((c.endFromEpochMs - c.beginningFromEpochMs) / 60000.0)).longValue();

            sumMeteredCalls += c.meteredMinutes;
            sumAllCalls += callLengthMin;

            TextView date = new TextView(this);
            date.setText(datetimeFormat.format(new Date(c.beginningFromEpochMs)));
            date.setTextSize(10);
            tr.addView(date);

            TextView number = new TextView(this);
            number.setText(PhoneNumberUtils.formatNumber(c.caller));
            number.setTextSize(10);
            tr.addView(number);

            if (c.meteredMinutes != 0) {
                TextView meteredMinutes = new TextView(this);
                meteredMinutes.setTextColor(res.getColor(R.drawable.vis_bill_graph_call_more));
                meteredMinutes.setText(String.format("%+d", c.meteredMinutes));
                meteredMinutes.setTextSize(10);
                tr.addView(meteredMinutes);
            } else {

                if (! sumPerReason.containsKey(c.reasonForRate)) {
                    sumPerReason.put(c.reasonForRate, 0L);
                }
                sumPerReason.put(c.reasonForRate, sumPerReason.get(c.reasonForRate) + callLengthMin);

                TextView reason = new TextView(this);
                reason.setText("0  (" + c.reasonForRate + ")");
                reason.setTextColor(res.getColor(R.drawable.vis_bill_graph_call_nochange));
                reason.setTextSize(10);
                tr.addView(reason);
            }

            TextView sumText = new TextView(this);
            sumText.setText(Long.toString(sumMeteredCalls));
            sumText.setTextSize(10);
            if (sumMeteredCalls > configuration.getBillAllowedMeteredMinutes())
                sumText.setTextColor(res.getColor(R.drawable.vis_bill_graph_prediction_over));
            tr.addView(sumText);

            table.addView(tr);
        }

        sumPerReason.put("~ all", sumAllCalls);
        if (sumAllCalls != sumMeteredCalls)
            sumPerReason.put("~ metered", sumMeteredCalls);

        for (String k: sumPerReason.keySet()) {
            long v = sumPerReason.get(k);
            if (v == 0)
                continue;

            TableRow tr = new TableRow(this);
            tr.setPadding(4, 5, 4, 5);

            TextView keyText = new TextView(this);
            keyText.setGravity(Gravity.RIGHT);
            keyText.setText(k + ": ");
            tr.addView(keyText);

            TextView valueText = new TextView(this);
            valueText.setText(Long.toString(v));
            tr.addView(valueText);

            table.addView(tr);
        }

        if (wroteLine) {
            description.setTextSize(17);
            description.setText("Bill " + 
                    dateFormat.format(new Date(usageData.getBeginningOfPeriodAsMs()+1000)) +
                    " to " +
                    dateFormat.format(new Date(usageData.getEndOfPeriodAsMs())) + ".");

            description.setPadding(5, 14, 5, 7);
        }
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
