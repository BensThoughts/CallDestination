package com.sanfranciscosunrise.silente.calldestination;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;


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

    private static final int REQUEST_ERROR = 0;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int REQUEST_LOCATION_PERMISSIONS = 0;

    private Location mCurrentLocation;
    private LocationManager mLocationManager;

    private OverlayShowingService mService;
    private boolean mBound = false;

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



    //private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.fragment_settings_call_destination);

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        Intent serviceBindingIntent = new Intent(this, OverlayShowingService.class);
        bindService(serviceBindingIntent, mConnection, Context.BIND_AUTO_CREATE);

        //mGoogleApiClient = new GoogleApiClient.Builder(this)
        //        .addApi(Places.GEO_DATA_API)
        //        .addApi(Places.PLACE_DETECTION_API)
        //        .enableAutoManage(this, this)
        //        .build();
        if (hasLocationPermission()) {
            mCurrentLocation = findLocation();
            double locationLat = mCurrentLocation.getLatitude();
            double locationLng = mCurrentLocation.getLongitude();

            try {
                Intent placePickingIntent =
                        new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                                .setBoundsBias(new LatLngBounds(
                                        new LatLng(locationLat, locationLng),
                                        new LatLng(locationLat, locationLng)))
                                .build(this);
                //placePickingIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                //placePickingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(placePickingIntent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
            } catch (GooglePlayServicesRepairableException e) {
                Log.e(TAG, "ERROR: " + e);
                // Handle the error
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.e(TAG, "ERROR: " + e);
                // Handle the error
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int errorCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (errorCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = apiAvailability
                    .getErrorDialog(this, errorCode, REQUEST_ERROR,
                            new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    // Leave if services are unavailable
                                    finish();
                                }
                            });
            errorDialog.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);

                String phoneNumber = place.getPhoneNumber().toString();
                String address = place.getAddress().toString();

                if (mBound) {
                    // notify OverlayShowingService that we are about to start getting directions
                    // change overlay from search to phone call
                    mService.setDestinationPhoneNumber(phoneNumber);
                    mService.setModeSearch(false);
                }
                Uri navigateToDestinationUri = Uri.parse("google.navigation:q=" + address);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, navigateToDestinationUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //mapIntent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                startActivity(mapIntent);
                finish();

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
    public void onStop() {
        super.onStop();
        //mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        //mGoogleApiClient = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION_PERMISSIONS:
                if (hasLocationPermission()) {
                    mCurrentLocation = findLocation();
                }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(this, LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private Location findLocation() {
        String locationProvider = LocationManager.GPS_PROVIDER;
        return mLocationManager.getLastKnownLocation(locationProvider);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

}
