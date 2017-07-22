package com.sanfranciscosunrise.calldestination.app;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.SearchEvent;
//import com.google.firebase.FirebaseApp;

import io.fabric.sdk.android.Fabric;

import static android.R.attr.permission;


/**
 * Created by Blu-J on 5/28/17.
 */

public class CallDestinationActivity extends SingleFragmentActivity {
        private static final String TAG = "CallDestinationActivity";

    public final static int REQUEST_CODE = 8974; //-1010101;

    /**
     *
     *
    public void checkDrawOverlayPermission() {
        // check if we already  have permission to draw over other apps
        if (!Settings.canDrawOverlays(this)) {
            // if not construct intent to request permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            // request permission via start activity for result
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        // check if received result code
         is equal our requested code for draw permission
        if (requestCode == REQUEST_CODE) {
       // if so check once again if we have permission
            if (Settings.canDrawOverlays(this)) {
                // continue here - permission was granted
            }
        }
    }
     **/

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
           // FirebaseApp.initializeApp(this);
            Fabric.with(this, new Crashlytics());
            //checkDrawOverlayPermission();
          //  throw new RuntimeException("This is a forced crash");
        }

        @Override
        protected Fragment createFragment() {
            return CallDestinationFragment.newInstance();
        }

}
