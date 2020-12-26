package humer.UvcCamera;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class StartIsoStreamService extends IntentService {

    private int result = Activity.RESULT_CANCELED;
    public static final String INIT = "urlpath";
    public static final String FILENAME = "filename";
    public static final String FILEPATH = "filepath";
    public static final String RESULT = "result";
    public static final String NOTIFICATION = "humer.UvcCamera.service.receiver";

    public native void JniGetAnotherFrame();
    public native void JniPrepairForStreamingfromService();
    public native void JniServiceOverSurface();



    public StartIsoStreamService() {
        super("IsoStream - Service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        log("onHandleIntent");

        String init = intent.getStringExtra(INIT);
        if (init.equals("INIT")) {
            JniPrepairForStreamingfromService();
        }

        String fileName = intent.getStringExtra(FILENAME);
        if (fileName.equals("Surface")) {
            JniServiceOverSurface();
            result = Activity.RESULT_OK;
            return;
        }

        if (fileName.equals("Resume")) {
            JniServiceOverSurface();
            result = Activity.RESULT_OK;
            return;
        }

        if (fileName.equals("Start") ) {
            log("prepairing");
            JniPrepairForStreamingfromService();
        } else log("not Start");
        JniGetAnotherFrame();
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
