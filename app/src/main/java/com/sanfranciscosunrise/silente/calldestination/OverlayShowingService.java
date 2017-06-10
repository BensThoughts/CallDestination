package com.sanfranciscosunrise.silente.calldestination;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by Blu-J on 5/28/17.
 *
 * OverlayShowingService is how we implement our button that is capable of remaining always
 * on top and in front of all applications no matter what.  How else we gonna make those calls?
 * It's all the new rage, BAAS (Buttons as a service).  Out service also implements full private
 * binding so that we can detect when the PlacePicker activity has had a place chosen as well as when
 * someone is finished getting directions to the place chosen.
 *
 * A. I would have done everything within this service but services don't allow for startActivityForResult()
 *    so I created a very small activity to purely handle that and the code to connect and authenticate with
 *    the GoogleApis objects. is there a better way?
 *
 * B. At least I think it's a private binding, can apps from outside of this package bind to and manipulate
 *    the BAAS (Buttons as a service)? That might be bad, how do I fix this?
 */

public class OverlayShowingService extends Service implements View.OnTouchListener {
    private static final String TAG = "OverlayShowingService";

    private static final int FOREGROUND_SERVICE_ID = 3499;
    private static final String[] LOCATION_PERMISSIONS = new String[]{
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final UUID EMPTY_UUID = new UUID(0,0);
    private static final SimpleDateFormat
            mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private static UUID mUUID; // This is static to the whole application context. Used
                               // to identify individual phones/users in the Firebase database

    private String mDestinationPhoneNumber;

    private View topLeftView;
    private ImageButton overlayButton;
    private ImageButton cancelButton;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private WindowManager wm;

    private boolean modeSearchOrCall = true; // true == search mode, false == call mode
    private boolean mClicked; // track is the button has been clicked once or twice

    private static LocationManager mLocationManager;
    private LatLng mCurrentLatLng;

    private DatabaseReference mRef;

    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        OverlayShowingService getService() {
            return OverlayShowingService.this;
        }
    }

    public static Intent newIntent(Context context) {
        return new Intent(context, OverlayShowingService.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(FOREGROUND_SERVICE_ID, buildNotification());

        mUUID = QueryPreferences.getPrefUUID(getApplicationContext());
        if (mUUID.equals(EMPTY_UUID)) {
            mUUID = UUID.randomUUID();
            QueryPreferences.setPrefUUID(getApplicationContext(), mUUID);
        }

        mRef = FirebaseDatabase.getInstance().getReference();

        if (hasLocationPermission()) {
            mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        }

        mClicked = false;

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        modeSearchOrCall = QueryPreferences.isServiceSearch(this);
        mDestinationPhoneNumber = QueryPreferences.getPrefLastKnownPhoneNumber(this);

        overlayButton = new ImageButton(this);
        setButtonImage(modeSearchOrCall, overlayButton);
        overlayButton.setAlpha(0.7f);
        overlayButton.setBackgroundColor(Color.TRANSPARENT);
        overlayButton.setOnTouchListener(this);
        overlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mClicked) {
                    createCancelButton();
                    if (hasLocationPermission()) {
                        mCurrentLatLng = findLocation();
                        long currentTime = new Date().getTime();
                        mRef.getRoot()
                                .child("current_position/" + mUUID.toString() + "/" + currentTime)
                                .setValue(mCurrentLatLng);
                    }
                    mClicked = true;
                } else {
                    removeCancelButton();
                    mClicked = false;
                    startSearchOrCall(modeSearchOrCall);
                }
            }
        });

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        wm.addView(overlayButton, params);

        topLeftView = new View(this);

        /****************
         * I'm uncertain if we need to add topLeftView actually to the screen
         * or even really what it is for at all.  I suspect for screens in which
         * the top left is not 0,0?? can we just set to [0,0] in the onTouchListener,
         * which is the only place this is used.
         *
         * WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
         *        WindowManager.LayoutParams.WRAP_CONTENT,
         *        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
         *        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
         *        PixelFormat.TRANSLUCENT);
         * topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
         *
         * topLeftParams.x = 0;
         * topLeftParams.y = 0;
         * topLeftParams.width = 0;
         * topLeftParams.height = 0;
         * wm.addView(topLeftView, topLeftParams);*
         ****************/
    }

    private void startSearchOrCall(boolean isSearchOrCall) {
        if (isSearchOrCall) {
            final Intent searchIntent = new Intent(getApplicationContext(), PlacePickerActivity.class);
            searchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (hasLocationPermission()) {
                mCurrentLatLng = findLocation();
                LatLngBounds currentLatLngBounds = new LatLngBounds(mCurrentLatLng,
                        mCurrentLatLng);
                searchIntent.putExtra("LOCATION", currentLatLngBounds);
            }
            startActivity(searchIntent);
        } else if (mDestinationPhoneNumber != null){

            long currentTime = new Date().getTime();
            mRef.getRoot()
                    .child("phone_call_attempt/" + mUUID.toString() + "/" + currentTime)
                    .setValue(mDestinationPhoneNumber);

            final Intent phoneCallIntent=new Intent(Intent.ACTION_DIAL,
                    Uri.fromParts("tel",mDestinationPhoneNumber,null));
            phoneCallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(phoneCallIntent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (overlayButton != null) {
            wm.removeView(overlayButton);
            //wm.removeView(topLeftView);
            overlayButton = null;
            topLeftView = null;
        }
        removeCancelButton();
        mCurrentLatLng = null;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();

            moving = false;

            int[] location = new int[2];
            overlayButton.getLocationOnScreen(location);

            originalXPos = location[0];
            originalYPos = location[1];

            offsetX = originalXPos - x;
            offsetY = originalYPos - y;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            float x = event.getRawX();
            float y = event.getRawY();

            // Both buttons share the same LayoutParams other then the cancel button
            // is further to the right by exactly the width of one button
            WindowManager.LayoutParams params =
                    (WindowManager.LayoutParams)overlayButton.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);

            wm.updateViewLayout(overlayButton, params);

            // cancelButton uses overlayButton's LayoutParam and then
            // puts them back to where they were when done with them.
            if (cancelButton != null) {
                params.x = params.x + overlayButton.getWidth();
                wm.updateViewLayout(cancelButton, params);
                params.x = params.x - overlayButton.getWidth();
            }

            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (moving) {
                return true;
            }
        }
        return false;
    }

    public void setDestinationPhoneNumber(String destinationPhoneNumber) {
        mDestinationPhoneNumber = destinationPhoneNumber;
        QueryPreferences.setPrefLastKnownPhoneNumber(this, mDestinationPhoneNumber);
    }

    public void setModeSearch(boolean modeSearch) {
        modeSearchOrCall = modeSearch;
        setButtonImage(modeSearchOrCall, overlayButton);
        QueryPreferences.setServiceSearch(this, modeSearchOrCall);
    }

    private static void setButtonImage(boolean modeSearch, ImageButton button) {
        if (modeSearch) {
            button.setImageResource(R.mipmap.ic_search);
        } else {
            button.setImageResource(R.mipmap.ic_call);
        }
    }

    private void createCancelButton() {
        cancelButton = new ImageButton(this);
        cancelButton.setBackgroundColor(Color.TRANSPARENT);
        cancelButton.setAlpha(0.7f);
        setButtonImage(!modeSearchOrCall, cancelButton);
        cancelButton.setOnTouchListener(this);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    setModeSearch(!modeSearchOrCall);
                    setButtonImage(!modeSearchOrCall, cancelButton);
                    mClicked = false;
                    startSearchOrCall(modeSearchOrCall);
            }
        });

        // Again both buttons share the same LayoutParams, see above
        // so we put the params back when we are done with them...
        WindowManager.LayoutParams cancelParams = (WindowManager.LayoutParams)overlayButton.getLayoutParams();
        cancelParams.x =  cancelParams.x + overlayButton.getWidth();
        wm.addView(cancelButton, cancelParams);
        cancelParams.x = cancelParams.x - overlayButton.getWidth();

        new CountDownTimer(2500, 1000) {
            @Override
            public void onTick(long millisUntilFinished) { }

            @Override
            public void onFinish() {
                removeCancelButton();
            }
        }.start();
    }

    private void removeCancelButton() {
        if (cancelButton !=null) {
            mClicked = false;
            wm.removeView(cancelButton);
            cancelButton = null;
        }
    }

    private Notification buildNotification() {
        NotificationCompat.Builder b=new NotificationCompat.Builder(this);

        b.setOngoing(false)
                .setContentTitle(getString(R.string.notification_content_title))
                .setContentText(getString(R.string.notification_content_text))
                .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
                .setTicker(getString(R.string.notification_ticker))
                .setContentIntent(PendingIntent.getActivity(this, 5544,
                        new Intent(getApplicationContext(), CallDestinationActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT));
        return(b.build());
    }

    private boolean hasLocationPermission() {
        int result = ContextCompat.checkSelfPermission(this, LOCATION_PERMISSIONS[0]);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private LatLng findLocation() {
        String locationProvider = LocationManager.GPS_PROVIDER;
        Location lastKnown = mLocationManager.getLastKnownLocation(locationProvider);
        return new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude());
    }

}

