/*
 * Copyright 2019 peter.
 *
 */

package humer.uvc_camera;



import android.app.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


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

    private int ActivitySetUpTheUsbDeviceRequestCode = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        tv = (TextView) findViewById(R.id.textDarstellung);
        tv.setText("Your current Values are:\n\n( - this is a sroll field - )\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaxPacketSize = " + maxPacketSize + "\nVideoformat = " + videoformat + "\ncamFormatIndex = " + camFormatIndex + "\n" +
                "camFrameIndex = " + camFrameIndex + "\nimageWidth = "+ imageWidth + "\nimageHeight = " + imageHeight + "\ncamFrameInterval = " + camFrameInterval + "" +
                "\n\nYou can edit these Settings by clicking on (Set Up The Camera Device).\nYou can then save the values and later restore them.");

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
        intent.putExtra("bun",bundle);
        super.onResume();
        startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
    }


    public void restoreCameraSettings (View view) {
        SaveToFile  stf;
        stf = new SaveToFile(this, this);
        stf.restoreValuesFromFile();
        stf = null;

    }




    public void isoStream(View view){


        if (camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||packetsPerRequest == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0 ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (TextView) findViewById(R.id.textDarstellung);
                    tv.setText("Values for the camera not correctly setted !!\nPlease set up the values for the Camera first.\nTo Set Up the Values press the Settings Button and click on 'Set up with Uvc Values' or 'Edit / Save / Restor' and 'Edit Save'");  }
            });
        } else {

            // TODO Auto-generated method stub
            Intent intent = new Intent(getApplicationContext(), StartTheStreamActivity.class);
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
            intent.putExtra("bun",bundle);
            startActivity(intent);
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



}
