package com.sanfranciscosunrise.silente.calldestination;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;


/**
 * Created by Blu-J on 5/29/17.
 *
 * PlacePickerActivity is the primary activity responsible for launching the
 * Google Places API PlaceAutocomplete floating places search bar.  It binds to our OverShowingService
 * so that we can have a little back and forth between out BAAS (Buttons as a service!) which is just
 * our overlay button.
 */

public class PlacePickerActivity extends FragmentActivity implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "PlacePickerActivity";
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final int DIRECTIONS_REQUEST_CODE = 2;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            OverlayShowingService.LocalBinder binder = (OverlayShowingService.LocalBinder)service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    OverlayShowingService mService;
    boolean mBound = false;

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceBindingIntent = new Intent(this, OverlayShowingService.class);
        bindService(serviceBindingIntent, mConnection, Context.BIND_AUTO_CREATE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        try {
            Intent placePickingIntent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .build(this);
            startActivityForResult(placePickingIntent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, "ERROR: " + e);
            // Handle the error
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "ERROR: " + e);
            // Handle the error
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Place:" + place.getName());
                Log.i(TAG, "Phone:" + place.getPhoneNumber());

                if (mBound) {
                    // notify OverlayShowingService that we are about to start getting directions
                    // change overlay from search to phone call
                    mService.setDestinationPhoneNumber(place.getPhoneNumber().toString());
                    mService.switchMode();
                }
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // Handle the error
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
