package com.sanfranciscosunrise.silente.calldestination;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

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

public class OverlayShowingService extends Service implements View.OnTouchListener, View.OnClickListener {
    private static final String TAG = "OverlayShowingService";

    private final IBinder mBinder = new LocalBinder();

    private View topLeftView;

    private ImageButton overlayButton;
    //private ImageButton cancelButton;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private WindowManager wm;

    private boolean modeSearchOrCall = true; // true == search mode, false == call mode
    private String mDestinationPhoneNumber;

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

        wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        modeSearchOrCall = QueryPreferences.isServiceSearch(this);
        mDestinationPhoneNumber = QueryPreferences.getPrefLastKnownPhoneNumber(this);

        overlayButton = new ImageButton(this);
        setButtonText(modeSearchOrCall);
        overlayButton.setOnTouchListener(this);
        overlayButton.setAlpha(0.7f);
        overlayButton.setBackgroundColor(0x55fe4444);
        overlayButton.setOnClickListener(this);
        overlayButton.setCropToPadding(true);
        overlayButton.setHovered(true);
        overlayButton.setMaxHeight(2);
        overlayButton.setMaxWidth(2);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.x = 0;
        params.y = 0;
        wm.addView(overlayButton, params);

        /***************************************************************
         *
         * Not sure if we are going to keep this cancel button,
         * looking for less resource intensive ways of doing this first
         * seems two buttons are not greater then one, as the app gets shutdown
         * by the system with two for I'm guessing resource reasons
         *
         * still need to profile this
         * and look at alternatives.
         *
        cancelButton = new ImageButton(this);
        cancelButton.setOnTouchListener(this);
        cancelButton.setBackgroundColor(0x55fe4444);
        cancelButton.setOnClickListener(this);
        cancelButton.setAlpha(0.7f);
        cancelButton.setCropToPadding(true);
        cancelButton.setHovered(true);
        //cancelButton.setMaxHeight(2);
        //cancelButton.setMaxWidth(2);
        cancelButton.setImageResource(R.mipmap.ic_cancel);

        WindowManager.LayoutParams cancelParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        cancelParams.gravity = Gravity.LEFT | Gravity.TOP;
        cancelParams.width = 40;
        cancelParams.height = 40;
        cancelParams.x = params.x + 70;
        cancelParams.y = 0;
        Log.i(TAG, "cancelButton.getHeight() == " + cancelButton.getHeight());
        wm.addView(cancelButton, cancelParams);
        Log.i(TAG, "cancelButton.getHeight() == " + cancelButton.getHeight());
         ***************************************************************/


        topLeftView = new View(this);
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;
     //   wm.addView(topLeftView, topLeftParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayButton != null) {
            wm.removeView(overlayButton);
            //wm.removeView(topLeftView);
            overlayButton = null;
            topLeftView = null;
        }
        //if (cancelButton !=null) {
        //    wm.removeView(cancelButton);
        //    cancelButton = null;
        //}
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

            WindowManager.LayoutParams params = (WindowManager.LayoutParams)overlayButton.getLayoutParams();
            //WindowManager.LayoutParams cancelParams = (WindowManager.LayoutParams)cancelButton.getLayoutParams();

            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);

            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }

            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);
            //cancelParams.x = params.x + 70;
            //cancelParams.y = params.y;

            wm.updateViewLayout(overlayButton, params);
            //wm.updateViewLayout(cancelButton, cancelParams);
            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (moving) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (modeSearchOrCall) {
            Intent searchIntent = new Intent(this, PlacePickerActivity.class);
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(searchIntent);
        } else if (mDestinationPhoneNumber != null) {
            Intent phoneCallIntent = new Intent(Intent.ACTION_DIAL,
                    Uri.fromParts("tel", mDestinationPhoneNumber, null));
            phoneCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(phoneCallIntent);
        }
    }

    public void setDestinationPhoneNumber(String destinationPhoneNumber) {
        mDestinationPhoneNumber = destinationPhoneNumber;
        QueryPreferences.setPrefLastKnownPhoneNumber(this, mDestinationPhoneNumber);
    }

    public void setModeSearch(boolean modeSearch) {
        modeSearchOrCall = modeSearch;
        setButtonText(modeSearchOrCall);
        QueryPreferences.setServiceSearch(this, modeSearchOrCall);
    }

    public void setButtonText(boolean modeSearch) {
        if (modeSearch) {
            overlayButton.setImageResource(R.mipmap.ic_search);
            //overlayButton.setText(R.string.overlay_button_search_mode);
        } else {
            overlayButton.setImageResource(R.mipmap.ic_call);
            //overlayButton.setText(R.string.overlay_button_call_mode);
        }
    }

}
