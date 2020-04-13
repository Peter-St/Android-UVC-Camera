/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.videortc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import humer.uvc_camera.R;
import humer.uvc_camera.Start_Iso_StreamActivity;

/**
 *
 */
public class WebRtc_MainActivity extends Activity {

    // Camera Values
    private static int camStreamingAltSetting;
    private static int camFormatIndex;
    private int camFrameIndex;
    private static int camFrameInterval;
    private static int packetsPerRequest;
    private static int maxPacketSize;
    private int imageWidth;
    private int imageHeight;
    private static int activeUrbs;
    private static String videoformat;
    private static boolean camIsOpen;
    public static byte bUnitID;
    public static byte bTerminalID;
    public static byte bStillCaptureMethod;
    public static byte[] bNumControlTerminal;
    public static byte[] bNumControlUnit;


    private static final String TAG = WebRtc_MainActivity.class.getSimpleName();

    private Button button;
    private static final int INTERNET_PERMISSION_CODE = 100;
    private static final int CHANGE_NETWORK_STATE_PERMISSION_CODE = 101;
    private static final int ACCESS_NETWORK_STATE_PERMISSION_CODE = 102;
    private static final int RECORD_AUDIO_STATE_PERMISSION_CODE = 103;
    private static final int MODIFY_AUDIO_STATE_PERMISSION_CODE = 104;
    private static final int CAMERA_PERMISSION_CODE = 105;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.main_activity);

        addListenerOnButton();

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.CHANGE_NETWORK_STATE,  Manifest.permission.ACCESS_NETWORK_STATE};

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        checkPermission (Manifest.permission.INTERNET,
                INTERNET_PERMISSION_CODE);
        checkPermission (Manifest.permission.CHANGE_NETWORK_STATE,
                CHANGE_NETWORK_STATE_PERMISSION_CODE);
        checkPermission (Manifest.permission.ACCESS_NETWORK_STATE,
                ACCESS_NETWORK_STATE_PERMISSION_CODE);
        checkPermission (Manifest.permission.RECORD_AUDIO,
                RECORD_AUDIO_STATE_PERMISSION_CODE);
        checkPermission (Manifest.permission.MODIFY_AUDIO_SETTINGS,
                MODIFY_AUDIO_STATE_PERMISSION_CODE);
        checkPermission (Manifest.permission.CAMERA,
                CAMERA_PERMISSION_CODE);

        fetchTheValues();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == INTERNET_PERMISSION_CODE) {

            // Checking whether user granted the permission or not.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                // Showing the toast message
                Toast.makeText(WebRtc_MainActivity.this,
                        "INTERNET Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "INTERNET Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
        else if (requestCode == CHANGE_NETWORK_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
        else if (requestCode == ACCESS_NETWORK_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(WebRtc_MainActivity.this,
                        "ACCESS_NETWORK_STATE_ Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "ACCESS_NETWORK_STATE_ Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == RECORD_AUDIO_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == MODIFY_AUDIO_STATE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CHANGE_NETWORK_STATE Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CAMERA Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(WebRtc_MainActivity.this,
                        "CAMERA Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void checkPermission(String permission, int requestCode)    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(
                WebRtc_MainActivity.this,
                permission)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat
                    .requestPermissions(
                            WebRtc_MainActivity.this,
                            new String[] { permission },
                            requestCode);
        }
    }



    public void addListenerOnButton() {

        button = findViewById(R.id.connectBtn);

        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent myIntent = new Intent(WebRtc_MainActivity.this, CallActivity.class);
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
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                myIntent.putExtra("bun",bundle);
                startActivity(myIntent);
            }

        });

    }

    public static boolean hasPermissions(Context context, String... permissions)
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null)
        {
            for (String permission : permissions)
            {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                {
                    return false;
                }
            }
        }
        return true;
    }

    private void fetchTheValues(){

        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("bun");
        camStreamingAltSetting=bundle.getInt("camStreamingAltSetting",0);
        videoformat=bundle.getString("videoformat");
        camFormatIndex=bundle.getInt("camFormatIndex",0);
        imageWidth=bundle.getInt("imageWidth",0);
        imageHeight=bundle.getInt("imageHeight",0);
        camFrameIndex=bundle.getInt("camFrameIndex",0);
        camFrameInterval=bundle.getInt("camFrameInterval",0);
        packetsPerRequest=bundle.getInt("packetsPerRequest",0);
        maxPacketSize=bundle.getInt("maxPacketSize",0);
        activeUrbs=bundle.getInt("activeUrbs",0);
        bUnitID = bundle.getByte("bUnitID",(byte)0);
        bTerminalID = bundle.getByte("bTerminalID",(byte)0);
        bNumControlTerminal = bundle.getByteArray("bNumControlTerminal");
        bNumControlUnit = bundle.getByteArray("bNumControlUnit");
        bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
    }
}