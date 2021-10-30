package humer.UvcCamera.LibUsb;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class LibUsbManagerService extends Service {

    private static String LOG_TAG = "BoundService";
    private IBinder mBinder = new MyBinder();

    public boolean libusb_wrapped = false;
    public boolean native_values_set = false;
    // Interfaces:
    public boolean libusb_InterfacesClaimed = false;
    public boolean altsettingControl = false;
    public boolean altsettingStream = false;
    //CTL
    public boolean ctl_to_camera_sent = false;
    public boolean ctl_for_camera_sucessful = false;
    //Stream
    public boolean streamPerformed = false;
    public boolean streamOnPause = false;
    public boolean streamClosed = false;
    //Stream CallbacksOverJNA
    public boolean jnaCallbackSet = true;
    // Exit Values
    public boolean libusb_closed = false;
    public boolean libusb_exited = false;
    ////////////////////////////////////////////////////////  Values have Changed but not initialized
    public boolean valuesChangedButNotInit = false;
    ///////////////////////////////// Stream can be performed !!!!!!!1
    public boolean streamCanBePerformed = false;
    public boolean streamCanBeResumed = false;
    public boolean jniMethodsAndConstantsSet = false;
    public boolean streamHelperServiceStarted = false;

    public void altSettingControl () {
        altsettingControl = true;
        altsettingStream = false;
    }
    public void altSettingStream () {
        altsettingControl = false;
        altsettingStream = true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");
    }
    @Override
    public IBinder onBind(Intent intent) {
        sendMessage(getApplicationContext());
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }
    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }
    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        return true;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
    }
    public String getTimestamp() {
        return new String("Stamp");
    }
    public class MyBinder extends Binder {
        public LibUsbManagerService getService() {
            return LibUsbManagerService.this;
        }
    }

    private void sendMessage(Context context) {
        Intent intent = new Intent("REQUEST_PROCESSED");
        intent.putExtra("message", "Hello World!");
        // Send Broadcast to Broadcast receiver with message
        context.sendBroadcast(intent);
    }
}
