package com.sanfranciscosunrise.silente.calldestination;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Blu-J on 5/31/17.
 * This is the main fragment that is used to start and stop the BAAS (Button as a service).
 * In the future I will be adding a few extra options into this fragment as it will also
 * be the primary place for setting various service and button options as well.
 *
 * TODO Options:
 *  * choose whether or not pressing search launches directions alongside binding the destination phone
 *    number to the button.
 */

public class CallDestinationFragment extends Fragment {
    private static final String TAG = "CallDestinationFragment";

    private Button mButton;
    private boolean mIsServiceOn = false;

    public static CallDestinationFragment newInstance() {
        return new CallDestinationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsServiceOn = QueryPreferences.isServiceOn(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings_call_destination, container, false);

        mButton = (Button)v.findViewById(R.id.toggle_service);
        setButtonText(mIsServiceOn);
        mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setServiceOn(!mIsServiceOn);
            }
        });

        return v;
    }

    public void setServiceOn(boolean isOn) {
        mIsServiceOn = isOn;
        //Intent svc = new Intent(getActivity(), OverlayShowingService.class);
        Intent svc = OverlayShowingService.newIntent(getActivity());
        if (mIsServiceOn) {
            getActivity().startService(svc);
        } else {
            getActivity().stopService(svc);
        }
        QueryPreferences.setServiceOn(getActivity(), mIsServiceOn);
        QueryPreferences.setServiceSearch(getActivity(), mIsServiceOn);
        setButtonText(mIsServiceOn);
    }

    public void setButtonText(boolean isOn) {
        if (isOn) {
            mButton.setText(R.string.button_toggle_service_stop);
        } else {
            mButton.setText(R.string.button_toggle_service_start);
        }
    }
}
