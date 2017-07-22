package com.sanfranciscosunrise.silente.calldestination;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

/**
 * Created by root on 6/6/17.
 */

public class MyPlace {
    private String mName;
    private String mAddress;
    private String mTel;
    private LatLng mLatLng;

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public String getTel() {
        return mTel;
    }

    public void setTel(String tel) {
        mTel = tel;
    }

    public LatLng getLatLng() {
        return mLatLng;
    }

    public void setLatLng(LatLng latLng) {
        mLatLng = latLng;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }
}
