package com.sanfranciscosunrise.silente.calldestination;


import android.support.v4.app.Fragment;


/**
 * Created by Blu-J on 5/28/17.
 */

public class CallDestinationActivity extends SingleFragmentActivity {
        private static final String TAG = "CallDestinationActivity";

        @Override
        protected Fragment createFragment() {
            return CallDestinationFragment.newInstance();
        }

}
