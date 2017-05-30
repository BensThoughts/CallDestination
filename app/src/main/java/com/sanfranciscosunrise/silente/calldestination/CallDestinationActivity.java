package com.sanfranciscosunrise.silente.calldestination;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * Created by Blu-J on 5/28/17.
 */

public class CallDestinationActivity extends FragmentActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Intent svc = new Intent(this, OverlayShowingService.class);
            startService(svc);
            finish();
        }
}
