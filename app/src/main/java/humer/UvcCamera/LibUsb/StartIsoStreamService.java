package humer.UvcCamera.LibUsb;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class StartIsoStreamService extends IntentService {

    private int result = Activity.RESULT_CANCELED;
    public static final String INIT = "urlpath";
    public static final String ACCESS_LIBUSB = "filename";
    public static final String FRAMEFORMAT = "format";
    public static final String RESULT = "result";
    public static final String NOTIFICATION = "humer.UvcCamera.service.receiver";

    public native void JniPrepairForStreamingfromService();
    public native void JniServiceOverSurface();

    public StartIsoStreamService() {
        super("IsoStream - Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        log("onHandleIntent");

        String init = intent.getStringExtra(INIT);
        if (init != null && init.equals("INIT")) {
            if (intent.getStringExtra(FRAMEFORMAT).equals("MFPEG")) JniPrepairForStreamingfromService();
            else JniPrepairForStreamingfromService();

        }

        String fileName = intent.getStringExtra(ACCESS_LIBUSB);
        if (fileName != null && fileName.equals("Surface")) {
            if (intent.getStringExtra(FRAMEFORMAT).equals("MFPEG"))  JniServiceOverSurface();
            else JniServiceOverSurface();
            result = Activity.RESULT_OK;
            return;
        }

        if (fileName != null && fileName.equals("Resume")) {
            if (intent.getStringExtra(FRAMEFORMAT).equals("MFPEG"))  JniServiceOverSurface();
            else JniServiceOverSurface();
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
