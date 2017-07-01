package com.sanfranciscosunrise.calldestination.app;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.crashlytics.android.Crashlytics;
//import com.google.firebase.FirebaseApp;

import io.fabric.sdk.android.Fabric;


/**
 * Created by Blu-J on 5/28/17.
 */

public class CallDestinationActivity extends SingleFragmentActivity {
        private static final String TAG = "CallDestinationActivity";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
           // FirebaseApp.initializeApp(this);
            Fabric.with(this, new Crashlytics());
          //  throw new RuntimeException("This is a forced crash");
        }

        @Override
        protected Fragment createFragment() {
            return CallDestinationFragment.newInstance();
        }

}
