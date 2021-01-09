package com.example.androidthings.videortc;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import humer.UvcCamera.LibUsb.JNA_I_LibUsb;

public class WebRtcService extends IntentService {

    private int result = Activity.RESULT_CANCELED;
    public static final String INIT = "ini";
    public static final String ENABLE_STREAM = "enable";
    public static final String RESULT = "result";
    public static final String NOTIFICATION = "humer.UvcCamera.service.receiver";

    //public native void JniGetAnotherFrame();
    public native void JniPrepairForStreamingfromService();
    //public native void JniServiceOverSurface();

    public WebRtcService() {
        super("WebRtc - Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        log("onHandleIntent");
        String init = intent.getStringExtra(INIT);
        if (init.equals("INIT")) {
            JNA_I_LibUsb.INSTANCE.prepairTheStream_WebRtc_Service();
        }

        String fileName = intent.getStringExtra(ENABLE_STREAM);
        if (fileName.equals("ENABLE_STREAM")) {
            JNA_I_LibUsb.INSTANCE.lunchTheStream_WebRtc_Service();
            result = Activity.RESULT_OK;
            return;
        }

        result = Activity.RESULT_OK;
    }

    public void returnToStreamActivity(){
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, Activity.RESULT_OK);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void publishSurface(){
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, Activity.RESULT_OK);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void publishResults(byte[] streamdata){
        log("publishResults called");
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(RESULT, Activity.RESULT_OK);
        intent.putExtra("byteArray", streamdata);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void log(String msg) {
        Log.i("ServiceClass", msg);
    }
}
