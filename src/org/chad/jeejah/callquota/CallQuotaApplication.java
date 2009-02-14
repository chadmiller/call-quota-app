package org.chad.jeejah.callquota;

import android.app.Application;
import android.database.ContentObserver;


public class CallQuotaApplication extends Application {
    private static final String TAG = "CallQuota.CallQuotaApplication";

    private UsageData[] usageData;
    public UsageData usage(int nthMonthBack) {
        return usage(nthMonthBack, true);
    }

    public UsageData usage(int nthMonthBack, boolean cache_p) {
        if (this.usageData == null) {
            this.usageData = new UsageData[12];
        }

        UsageData u;

        if (this.usageData[nthMonthBack] == null) {
            u = new UsageData(this, conf(), TAG, nthMonthBack);

            if (cache_p) {
                this.usageData[nthMonthBack] = u;
            }
        } else {
            u = this.usageData[nthMonthBack];
        }

        return u;
    }

    private Configuration configuration;
    public Configuration conf() {
        if (this.configuration == null) {
            this.configuration = new Configuration(this, TAG);
        }
        return this.configuration;
    }

    public void invalidateAll() {
        for (int i = 0; i < 12; i++)
            if (this.usageData[i] != null)
                this.usageData[i].invalidate();

        if (this.configuration != null)
            this.configuration.invalidate();
    }

}
/* vim: set et ai sta : */
