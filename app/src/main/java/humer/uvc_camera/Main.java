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
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import noman.zoomtextview.ZoomTextView;

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
    public static byte bUnitID;
    public static byte bTerminalID;
    public static byte[] bNumControlTerminal;
    public static byte[] bNumControlUnit;


    public boolean bildaufnahme = false;

    public Handler handler;
    public Button startStream;
    public Button settingsButton;
    public Button menu;
    private ZoomTextView tv;

    private final int REQUEST_PERMISSION_STORAGE=1;
    private int ActivitySetUpTheUsbDeviceRequestCode = 1;

    final static float STEP = 200;
    float mRatio = 1.0f;
    int mBaseDist;
    float mBaseRatio;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
        tv.setText("Your current Values are:\n\n( - this is a sroll and zoom field - )\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaxPacketSize = " + maxPacketSize + "\nVideoformat = " + videoformat + "\ncamFormatIndex = " + camFormatIndex + "\n" +
                "camFrameIndex = " + camFrameIndex + "\nimageWidth = "+ imageWidth + "\nimageHeight = " + imageHeight + "\ncamFrameInterval = " + camFrameInterval + "" +
                "\n\nYou can edit these Settings by clicking on (Set Up The Camera Device).\nYou can then save the values and later restore them.");
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
            bUnitID = data.getByteExtra("bUnitID",(byte) 0);
            bTerminalID = data.getByteExtra("bTerminalID",(byte)0);
            bNumControlTerminal = data.getByteArrayExtra("bNumControlTerminal");
            bNumControlUnit = data.getByteArrayExtra("bNumControlUnit");


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




            Intent intent = new Intent(this, SetUpTheUsbDevice.class);
            Bundle bundle=new Bundle();
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
            bundle.putByte("bUnitID",bUnitID);
            bundle.putByte("bTerminalID",bTerminalID);
            bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
            bundle.putByteArray("bNumControlUnit", bNumControlUnit);

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
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Values for the camera not correctly setted !!\nPlease set up the values for the Camera first.\nTo Set Up the Values press the Settings Button and click on 'Set up with Uvc Values' or 'Edit / Save / Restor' and 'Edit Save'");  }
                });
            } else {
                // TODO Auto-generated method stub
                Intent intent = new Intent(getApplicationContext(), Start_Iso_StreamActivity.class);
                Bundle bundle=new Bundle();
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
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);

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
                "Camera FrameIndex = " + camFrameIndex + "\nImage Width = "+ imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval = " +
                camFrameInterval + "\n\nUVC Values:\nbUnitID = " + bUnitID + "\nbTerminalID = " + bTerminalID);
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
