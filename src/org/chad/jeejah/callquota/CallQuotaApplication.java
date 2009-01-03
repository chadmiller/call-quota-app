package org.chad.jeejah.callquota;

import android.app.Application;
import android.database.ContentObserver;


public class CallQuotaApplication extends Application {
    private static final String TAG = "CallQuotaApplication";

    public Configuration configuration;

    CallQuotaApplication() {
        configuration.load(this);
    }

}
/* vim: set et ai sta : */
