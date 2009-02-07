package org.chad.jeejah.callquota;

import android.app.Application;
import android.database.ContentObserver;


public class CallQuotaApplication extends Application {
    private static final String TAG = "CallQuota.CallQuotaApplication";

    private UsageData[] usageData;
    public UsageData usage(int nthMonthBack) {
        if (usageData == null)
            this.usageData = new UsageData[12];

        if (this.usageData[nthMonthBack] == null)
            this.usageData[nthMonthBack] = new UsageData(this, conf(), TAG, nthMonthBack);

        return this.usageData[nthMonthBack];
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
