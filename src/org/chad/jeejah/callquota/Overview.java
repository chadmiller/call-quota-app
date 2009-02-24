package org.chad.jeejah.callquota;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.widget.ImageButton;
import android.view.View;


public class Overview extends Activity {
    private static final String TAG = "CallQuota.Overview";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);


        ImageButton b = (ImageButton) findViewById(R.id.button_okay);
        b.setOnClickListener(
            new View.OnClickListener() {
                public void onClick(View arg0) {
                    Intent i = new Intent(Overview.this, Pref.class);
                    startActivityForResult(i, 0);
                }
            }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                 Intent data) {
        finish();
    }

}
/* vim: set et ai sta : */
