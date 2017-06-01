package com.sanfranciscosunrise.silente.calldestination;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by Blu-J on 5/31/17.
 */

public class CallDestinationFragment extends Fragment implements View.OnClickListener{
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
        View v = inflater.inflate(R.layout.activity_call_destination, container, false);

        mButton = (Button)v.findViewById(R.id.toggle_service);
        setButtonText(mIsServiceOn);
        mButton.setOnClickListener(this);

        return v;
    }

    public void setServiceOn(boolean isOn) {
        Intent svc = new Intent(getActivity(), OverlayShowingService.class);
        mIsServiceOn = isOn;
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

    public void onClick(View v) {
        if (v.getId() == R.id.toggle_service) {
            if (mIsServiceOn) {
                setServiceOn(false);
            } else {
                setServiceOn(true);
            }
        }
    }

}
