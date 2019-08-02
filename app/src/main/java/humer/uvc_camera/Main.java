/*
 * Copyright 2019 peter.
 *
 */

package humer.uvc_camera;



import android.Manifest;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static humer.uvc_camera.UVC_Descriptor.log;


public class Main extends Activity {

    public static int camStreamingAltSetting;
    public static int camFormatIndex;
    public static int camFrameIndex;
    public static int camFrameInterval;
    public static int packetsPerRequest;
    public static int maxPacketSize;
    public static int imageWidth;
    public static int imageHeight;
    public static int activeUrbs;
    public static String videoformat;
    public static String deviceName;

    public boolean bildaufnahme = false;

    public Handler handler;
    public Button startStream;
    public Button settingsButton;
    public Button menu;
    private TextView tv;

    private final int REQUEST_PERMISSION_STORAGE=1;
    private int ActivitySetUpTheUsbDeviceRequestCode = 1;

    final static float STEP = 200;
    float mRatio = 1.0f;
    int mBaseDist;
    float mBaseRatio;

    private boolean sixtyfourbit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        tv = (TextView) findViewById(R.id.textDarstellung);
        tv.setText("Your current Values are:\n\n( - this is a sroll and zoom field - )\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaxPacketSize = " + maxPacketSize + "\nVideoformat = " + videoformat + "\ncamFormatIndex = " + camFormatIndex + "\n" +
                "camFrameIndex = " + camFrameIndex + "\nimageWidth = "+ imageWidth + "\nimageHeight = " + imageHeight + "\ncamFrameInterval = " + camFrameInterval + "" +
                "\n\nYou can edit these Settings by clicking on (Set Up The Camera Device).\nYou can then save the values and later restore them.");

        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getPointerCount() == 2) {
                    int action = event.getAction();
                    int pureaction = action & MotionEvent.ACTION_MASK;
                    if (pureaction == MotionEvent.ACTION_POINTER_DOWN) {
                        mBaseDist = getDistance(event);
                        mBaseRatio = mRatio;
                    } else {
                        float delta = (getDistance(event) - mBaseDist) / STEP;
                        float multi = (float) Math.pow(2, delta);
                        mRatio = Math.min(1024.0f, Math.max(0.1f, mBaseRatio * multi));
                        tv.setTextSize(mRatio + 13);
                    }
                }
                return true;
            }


        });

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(Main.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Main.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }


    int getDistance(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(dx * dx + dy * dy));
    }







    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivitySetUpTheUsbDeviceRequestCode && resultCode == RESULT_OK && data != null) {
            // TODO Extract the data returned from the child Activity.

            camStreamingAltSetting=data.getIntExtra("camStreamingAltSetting",0);
            videoformat=data.getStringExtra("videoformat");
            camFormatIndex=data.getIntExtra("camFormatIndex",0);
            imageWidth=data.getIntExtra("imageWidth",0);
            imageHeight=data.getIntExtra("imageHeight",0);
            camFrameIndex=data.getIntExtra("camFrameIndex",0);
            camFrameInterval=data.getIntExtra("camFrameInterval",0);
            packetsPerRequest=data.getIntExtra("packetsPerRequest",0);
            maxPacketSize=data.getIntExtra("maxPacketSize",0);
            activeUrbs=data.getIntExtra("activeUrbs",0);
            deviceName=data.getStringExtra("deviceName");

            tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                    "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                    "Camera FrameIndex = " + camFrameIndex + "\nImage Width = "+ imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval = " + camFrameInterval)          ;
            tv.setTextColor(Color.BLACK);

        }
    }


    public void viewPrivatePolicy(View view) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getApplicationContext(),
                PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    public void viewReadme (View view) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getApplicationContext(),
                ReadMeActivity.class);
        startActivity(intent);
    }


    public void setUpTheUsbDevice(View view){
        if (showStoragePermissionRead() && showStoragePermissionWrite()) {


            String bits;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bits = TextUtils.join(", ", Build.SUPPORTED_ABIS).contains("64") ? "64-Bit" : "32-Bit";
                sixtyfourbit = TextUtils.join(", ", Build.SUPPORTED_ABIS).contains("64");
            } else {
                bits = "32-Bit";
                sixtyfourbit = false;
            }
            log("bits = " + bits);
            log("sixtyfourbit = " + sixtyfourbit);


            Intent intent = new Intent(this, SetUpTheUsbDevice.class);
            Bundle bundle=new Bundle();
            bundle.putBoolean("sixtyfourbit", sixtyfourbit);
            bundle.putBoolean("edit", true);
            bundle.putInt("camStreamingAltSetting",camStreamingAltSetting);
            bundle.putString("videoformat",videoformat);
            bundle.putInt("camFormatIndex",camFormatIndex);
            bundle.putInt("imageWidth",imageWidth);
            bundle.putInt("imageHeight",imageHeight);
            bundle.putInt("camFrameIndex",camFrameIndex);
            bundle.putInt("camFrameInterval",camFrameInterval);
            bundle.putInt("packetsPerRequest",packetsPerRequest);
            bundle.putInt("maxPacketSize",maxPacketSize);
            bundle.putInt("activeUrbs",activeUrbs);
            bundle.putString("deviceName",deviceName);
            intent.putExtra("bun",bundle);
            super.onResume();
            startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
        }

    }


    public void restoreCameraSettings (View view) {
        if (showStoragePermissionRead() && showStoragePermissionWrite()) {
            SaveToFile  stf;
            stf = new SaveToFile(this, this);
            stf.restoreValuesFromFile();
            stf = null;
        }


    }




    public void isoStream(View view){
        if (showStoragePermissionRead() && showStoragePermissionWrite()) {
            if (camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||packetsPerRequest == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0 ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (TextView) findViewById(R.id.textDarstellung);
                        tv.setText("Values for the camera not correctly setted !!\nPlease set up the values for the Camera first.\nTo Set Up the Values press the Settings Button and click on 'Set up with Uvc Values' or 'Edit / Save / Restor' and 'Edit Save'");  }
                });
            } else {

                // TODO Auto-generated method stub
                Intent intent = new Intent(getApplicationContext(), Start_Iso_StreamActivity.class);
                Bundle bundle=new Bundle();
                bundle.putBoolean("sixtyfourbit", sixtyfourbit);
                bundle.putInt("camStreamingAltSetting",camStreamingAltSetting);
                bundle.putString("videoformat",videoformat);
                bundle.putInt("camFormatIndex",camFormatIndex);
                bundle.putInt("imageWidth",imageWidth);
                bundle.putInt("imageHeight",imageHeight);
                bundle.putInt("camFrameIndex",camFrameIndex);
                bundle.putInt("camFrameInterval",camFrameInterval);
                bundle.putInt("packetsPerRequest",packetsPerRequest);
                bundle.putInt("maxPacketSize",maxPacketSize);
                bundle.putInt("activeUrbs",activeUrbs);
                intent.putExtra("bun",bundle);
                startActivity(intent);
            }
        }

    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Main.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void setTextTextView () {
        tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                "Camera FrameIndex = " + camFrameIndex + "\nImage Width = "+ imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval = " + camFrameInterval)          ;
        tv.setTextColor(Color.GREEN);

    }

    private boolean showStoragePermissionRead() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed:", "Storage", Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                return false;
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                return false;
            }
        } else {
            //Toast.makeText(Main.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            return true;
        }
    }


    private boolean showStoragePermissionWrite() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed:", "Storage", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                return false;
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
                return false;
            }
        } else {
            //Toast.makeText(Main.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }



}
