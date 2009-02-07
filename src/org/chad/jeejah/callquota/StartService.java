package org.chad.jeejah.callquota;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

public class StartService extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, LogMonitorService.class);
        context.startService(i);
	}
}

