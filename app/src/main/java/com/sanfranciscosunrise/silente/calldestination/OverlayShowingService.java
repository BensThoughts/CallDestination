package com.sanfranciscosunrise.silente.calldestination;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
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

public class OverlayShowingService extends Service implements View.OnTouchListener {
    private static final String TAG = "OverlayShowingService";

    private final IBinder mBinder = new LocalBinder();

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
    private boolean mClicked = false;
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

        mClicked = false;

        wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        modeSearchOrCall = QueryPreferences.isServiceSearch(this);
        mDestinationPhoneNumber = QueryPreferences.getPrefLastKnownPhoneNumber(this);

        overlayButton = new ImageButton(this);
        setButtonText(modeSearchOrCall);
        overlayButton.setOnTouchListener(this);
        overlayButton.setAlpha(0.7f);
        overlayButton.setBackgroundColor(0x55fe4444);
        overlayButton.setCropToPadding(true);
        overlayButton.setHovered(true);
        overlayButton.setMaxHeight(2);
        overlayButton.setMaxWidth(2);

        overlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (modeSearchOrCall) {
                    Intent searchIntent = new Intent(getApplicationContext(), PlacePickerActivity.class);
                    //searchIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    searchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(searchIntent);
                } else {
                    if (!mClicked) {
                        createCancelButton();
                        mClicked = true;
                    } else {
                        if (mDestinationPhoneNumber != null) {
                            Intent phoneCallIntent = new Intent(Intent.ACTION_DIAL,
                                    Uri.fromParts("tel", mDestinationPhoneNumber, null));
                            phoneCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(phoneCallIntent);
                        }
                        removeCancelButton();
                        mClicked = false;
                    }
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
        //WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
        //        WindowManager.LayoutParams.WRAP_CONTENT,
        //        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
        //        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        //        PixelFormat.TRANSLUCENT);
        //topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
        //topLeftParams.x = 0;
        //topLeftParams.y = 0;
        //topLeftParams.width = 0;
        //topLeftParams.height = 0;
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
        removeCancelButton();
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
        setButtonText(modeSearchOrCall);
        QueryPreferences.setServiceSearch(this, modeSearchOrCall);
    }

    private void setButtonText(boolean modeSearch) {
        if (modeSearch) {
            overlayButton.setImageResource(R.mipmap.ic_search);
            //overlayButton.setText(R.string.overlay_button_search_mode);
        } else {
            overlayButton.setImageResource(R.mipmap.ic_call);
            //overlayButton.setText(R.string.overlay_button_call_mode);
        }
    }

    public void createCancelButton() {
        cancelButton = new ImageButton(this);
        cancelButton.setOnTouchListener(this);
        cancelButton.setBackgroundColor(0x55fe4444);
        cancelButton.setAlpha(0.7f);
        //cancelButton.setCropToPadding(true);
        //cancelButton.setHovered(true);
        cancelButton.setImageResource(R.mipmap.ic_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mClicked) {
                    setModeSearch(true);
                    removeCancelButton();
                }
            }
        });

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
}
