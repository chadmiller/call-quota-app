package org.chad.jeejah.callquota;

import android.app.Application;
import android.database.ContentObserver;


public class CallQuotaApplication extends Application {
    private static final String TAG = "CallQuotaApplication";

    private UsageData usageData;
    public UsageData usage() {
        if (this.usageData == null) {
            this.usageData = new UsageData(this, conf(), TAG, 0);
        }
        return this.usageData;
    }

    private Configuration configuration;
    public Configuration conf() {
        if (this.configuration == null) {
            this.configuration = new Configuration(this, TAG);
        }
        return this.configuration;
    }
}
/* vim: set et ai sta : */
