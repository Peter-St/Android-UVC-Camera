package humer.UvcCamera;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import humer.UvcCamera.LibUsb.LibUsbManagerService;

public class UVC_Camera_Controller extends Application {

    private boolean serviceStarted = false;
    private static UVC_Camera_Controller mInstance;

    public static synchronized UVC_Camera_Controller getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        startService();
    }

    public void startService(){
        Intent intent = new Intent(this, LibUsbManagerService.class);
        startService(intent);
        log("Manager Service started");
        //start your service
        serviceStarted = true;
    }

    public void stopService(){
        //stop service
        serviceStarted = false;
    }

    public void log(String msg) {
        Log.i("Libusb_Manager_Service", msg);
    }

}
