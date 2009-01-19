package org.chad.jeejah.callquota;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.content.Intent;

public class Help extends Activity {
    private static final String TAG = "CallQuota.Help";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help);
    }
}
