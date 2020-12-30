/*
Copyright 2019 Peter Stoiber

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Please contact the author if you need another license.
This Repository is provided "as is", without warranties of any kind.

*/

package humer.UvcCamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;


import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.crowdfire.cfalertdialog.views.CFPushButton;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.tomer.fadingtextview.FadingTextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import humer.UvcCamera.AutomaticDetection.Jna_AutoDetect;
import humer.UvcCamera.AutomaticDetection.Jna_AutoDetect_Handler;
import humer.UvcCamera.AutomaticDetection.LibUsb_AutoDetect;
import humer.UvcCamera.LibUsb.JNA_I_LibUsb;
import humer.UvcCamera.LibUsb.unRootedSample;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;
import humer.UvcCamera.UsbIso64.USBIso;
import humer.UvcCamera.UsbIso64.usbdevice_fs_util;
import noman.zoomtextview.ZoomTextView;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SetUpTheUsbDevice extends Activity {
    // USB codes:
    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";
    private static final String DEFAULT_USBFS = "/dev/bus/usb";
    private static String autoFilePathFolder = "UVC_Camera/autoDetection";

    // Request types (bmRequestType):
    private static final int RT_STANDARD_INTERFACE_SET = 0x01;
    private static final int RT_CLASS_INTERFACE_SET = 0x21;
    private static final int RT_CLASS_INTERFACE_GET = 0xA1;

    // Video interface subclass codes:
    private static final int SC_VIDEOCONTROL = 0x01;
    private static final int SC_VIDEOSTREAMING = 0x02;

    // Standard request codes:
    private static final int SET_INTERFACE = 0x0b;

    // Video class-specific request codes:
    private static final int SET_CUR = 0x01;
    private static final int GET_CUR = 0x81;
    private static final int GET_MIN = 0x82;
    private static final int GET_MAX = 0x83;
    private static final int GET_RES = 0x84;

    // VideoControl interface control selectors (CS):
    private static final int VC_REQUEST_ERROR_CODE_CONTROL = 0x02;

    // VideoStreaming interface control selectors (CS):
    private static final int VS_PROBE_CONTROL = 0x01;
    private static final int VS_COMMIT_CONTROL = 0x02;
    private static final int PU_BRIGHTNESS_CONTROL = 0x02;
    private static final int VS_STILL_PROBE_CONTROL = 0x03;
    private static final int VS_STILL_COMMIT_CONTROL = 0x04;
    private static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    private static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;

    // Android USB Classes
    private UsbManager usbManager;
    private UsbDevice camDevice = null;
    private UsbDeviceConnection camDeviceConnection;
    private UsbInterface camControlInterface;
    private UsbInterface camStreamingInterface;
    private UsbEndpoint camControlEndpoint;
    private UsbEndpoint camStreamingEndpoint;
    private PendingIntent mPermissionIntent;

    // Camera Values
    public int camStreamingAltSetting;
    public int camFormatIndex;
    public int camFrameIndex;
    public int camFrameInterval;
    public int packetsPerRequest;
    public int maxPacketSize;
    public int imageWidth;
    public int imageHeight;
    public int activeUrbs;
    public String videoformat;
    public String deviceName;
    public byte bUnitID;
    public byte bTerminalID;
    public byte[] bNumControlTerminal;
    public byte[] bNumControlUnit;
    public static byte[] bcdUVC;
    public byte bStillCaptureMethod;
    public boolean libUsb;
    public static boolean moveToNative;
    public boolean transferSucessful;


    // Vales for debuging the camera
    private String controlltransfer;
    private String initStreamingParmsResult;
    private String initStreamingParms;
    private int[] initStreamingParmsIntArray;
    private String probedStreamingParms;
    private int[] probedStreamingParmsIntArray;
    private String finalStreamingParms_first;
    private int[] finalStreamingParmsIntArray_first;
    private String finalStreamingParms;
    private int[] finalStreamingParmsIntArray;
    private String controlErrorlog;
    public StringBuilder stringBuilder;
    public int [] convertedMaxPacketSize;
    public static boolean camIsOpen;
    private boolean bulkMode;

    private enum Options { searchTheCamera, testrun, listdevice, showTestRunMenu, setUpWithUvcSettings };

    //Buttons & Views
    public Button testrun;
    private ZoomTextView tv;
    public Button menu;

    //  Other Classes as Objects
    private UVC_Descriptor uvc_descriptor;
    private SaveToFile  stf;
    private volatile IsochronousRead runningTransfer;
    private volatile IsochronousRead1Frame runningTransfer1Frame;

    // Values for Auto Detection
    private static int ActivityLibUsbAutoDetectRequestCode = 3;
    private static int ActivityJnaAutoDetectRequestCode = 4;
    public static boolean completed;
    public boolean highQuality;
    public static boolean raiseMaxPacketSize;
    public static boolean lowerMaxPacketSize;
    public static boolean raisePacketsPerRequest;
    public static boolean raiseActiveUrbs;
    public boolean max_Framelength_cant_reached;
    public boolean maxPacketsPerRequestReached;
    public boolean maxActiveUrbsReached;

    public int last_camStreamingAltSetting;
    public int last_camFormatIndex;
    public int last_camFrameIndex;
    public int last_camFrameInterval;
    public int last_packetsPerRequest;
    public int last_maxPacketSize;
    public int last_imageWidth;
    public int last_imageHeight;
    public int last_activeUrbs;
    public String last_videoformat;
    public boolean last_transferSucessful;



    // Values for the Automatic Set Up
    public int spacketCnt = 0;
    public int spacket0Cnt = 0;
    public int spacket12Cnt = 0;
    public int spacketDataCnt = 0;
    public int spacketHdr8Ccnt = 0;
    public int spacketErrorCnt = 0;
    public int sframeCnt = 0;
    public int sframeLen = 0;
    public int [] sframeLenArray = new int [5];
    public int [] [] shighestFramesCube = new int [10] [5] ;
    public int srequestCnt = 0;
    public int sframeMaximalLen = 0;
    public boolean fiveFrames;
    public int doneTransfers = 0;
    public int sucessfulDoneTransfers = 0;

    public String progress;
    public boolean submiterror;

    // Debug Camera Variables
    private CountDownLatch latch;
    private boolean automaticStart ;
    private boolean highQualityStreamSucessful;
    private CFAlertDialog percentageBuilder;
    private CFAlertDialog percentageBuilder2;
    private int number = 0;
    private boolean thorthCTLfailed;
    private boolean l1ibusbAutoRunning;

    private static int fd;
    private static int productID;
    private static int vendorID;
    private static String adress;
    private static int camStreamingEndpointAdress;
    private static String mUsbFs;
    private static int busnum;
    private static int devaddr;
    private volatile boolean libusb_is_initialized;
    private volatile boolean camera_is_initialized_over_libusb;
    private boolean camDeviceIsClosed = false;



    public Handler buttonHandler = null;
    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            Button button = findViewById(R.id.raiseSize_setUp);
            button.setEnabled(false); button.setAlpha(0);
            Button button2 = findViewById(R.id.lowerSize_setUp);
            button2.setEnabled(false); button2.setAlpha(0);
            buttonHandler = null;
        }
    };


    // JNI METHODS
    private static boolean isLoaded;
    static {
        if (!isLoaded) {
            System.loadLibrary("usb1.0");
            System.loadLibrary("jpeg");
            System.loadLibrary("yuv");
            System.loadLibrary("Usb_Support");
            isLoaded = true;
        }
    }

    /*
    public native void JniIsoStreamActivity(final Surface surface, int a, int b);
    public native void JniProbeCommitControl(int bmHint,int camFormatIndex,int camFrameIndex,int  camFrameInterval);
     */

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log( "(on receive) String action = " +   action  );
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    camDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(camDevice != null){
                            log("camDevice from BraudcastReceiver");
                        }
                    }
                    else {
                        log( "(On receive) permission denied for device ");
                        displayMessage("permission denied for device " );
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                camDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                displayMessage("ACTION_USB_DEVICE_ATTACHED:");
                tv.setText("ACTION_USB_DEVICE_ATTACHED: \n");
                tv.setTextColor(Color.BLACK);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    displayMessage("Permissions Granted to Usb Device");
                }
                else {
                    log( "(Device attached) permission denied for device ");
                }
            }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                camDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                displayMessage("ACTION_USB_DEVICE_DETACHED: \n");
                tv.setText("ACTION_USB_DEVICE_DETACHED: \n");
                tv.setTextColor(Color.BLACK);
                if (camDeviceConnection != null) {
                    if (camControlInterface != null) camDeviceConnection.releaseInterface(camControlInterface);
                    if (camStreamingInterface != null) camDeviceConnection.releaseInterface(camStreamingInterface);
                    camDeviceConnection.close();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = getLayoutInflater().inflate(R.layout.set_up_the_device_layout_main, null);
        setContentView(v);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        fetchTheValues();
        stf = new SaveToFile(this, this, v);
        testrun = findViewById(R.id.testrun);
        testrun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTestRunMenu(view);
            }
        });
        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
        tv.setText("Explanation:\n\n-(this is a scrollable and zoomable Text)\n\nTo set up the userspace driver for your USB camera you have to set the values for your camera.\nYou can use the button (Set up with UVC Settings) to automatically set " +
                "up the camera with UVC settings.\nOr you can set up or change the Vales by Hand with the button (Edit / Save the Camera Values)\n" +
                "\nWhen you have setted up the camera with all the vales click on the button (Testrun) to see if you get a valid output.\nIf the testrun works, you will see a couple of frames, which you received from your camera." +
                "\nNow you can try out other settings and maybe your output changes a little bit" +
                "\nThe best Output is when you get the biggest Frames (with a long Framesize)" +
                "\n\nImportant Values for the camera were (packetsPerRequest) and (ActiveUrbs)" +
                "\nFor Example:\n" +
                "You can set packetsPerRequest to 1 and also ActiveUrbs to 1. " +
                "If all the other values were set you can perform a testrun. " +
                "If the testrun worked, you can raise up packetsPerRequest or ActiveUrbs, or both of them. \n" +
                "The values for the two fields can be raised up to 132 for some devices and cameras. \n" +
                "Each Device has other settings for the Camera, so when you change the device, you have to (maybe) use other settings for the same camera.\n" +
                "When you have setted up the camera and receive valid frames, then save the values and start the camera stream in the Main Screen\n\n" +
                "Sometimes it could be, that the driver of your device runns mad because of wrong settings for you camera. Here the best solution will be to restart the device and connect the camera again.\n" +
                "If you device dosn't find your camera any more, than simply restart your device and start the program again.\n" +
                "\nSo far,\n" +
                "And Good Luck for the camera testing\n\n" +
                "You can run this program with all kinds of Android Devices.\n" +
                "You alse can run this program with all kinds of UVC Cameras\n" +
                "If a camera doesn't work, you can contact the developer of this program for solutions.");
        tv.setTextColor(Color.BLACK);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        try {
            findCam();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ScrollView scrollView = findViewById(R.id.scrolli_setup);
            scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    final int TIME_TO_WAIT = 2500;
                    Button button = findViewById(R.id.raiseSize_setUp);
                    if (button.isEnabled()) {
                        buttonHandler.removeCallbacks(myRunnable);
                        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                        return ;
                    }
                    button.setEnabled(true);
                    button.setAlpha(0.8f);
                    Button button2 = findViewById(R.id.lowerSize_setUp);
                    button2.setEnabled(true); button2.setAlpha(0.8f);

                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);

                }
            });
        }
        Button button = findViewById(R.id.raiseSize_setUp);
        button.setEnabled(false); button.setAlpha(0);
        Button button2 = findViewById(R.id.lowerSize_setUp);
        button2.setEnabled(false); button2.setAlpha(0);


        ConstraintLayout fadingTextView = (ConstraintLayout) findViewById(R.id.fadingTextViewLayout);
        fadingTextView.setVisibility(View.INVISIBLE);
        fadingTextView.setVisibility(View.GONE);








    }

    @Override
    public void onBackPressed()
    {
        writeTheValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (automaticStart) {
            mPermissionIntent = null;
            try {
                unregisterReceiver(mUsbReceiver);
                unregisterReceiver(mUsbDeviceReceiver);
            } catch(IllegalArgumentException e) {
                e.printStackTrace();
            }
            return;
        }
        mPermissionIntent = null;
        try {
            unregisterReceiver(mUsbReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        beenden();
    }

    //////////////////////// BUTTONS Buttons ///////////////////////////////////////

    public void raiseSize(View view){
        final int TIME_TO_WAIT = 2500;
        Button button = findViewById(R.id.raiseSize_setUp);
        if (button.isEnabled()) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
            tv.raiseSize();
            return ;
        }
        button.setEnabled(true);
        button.setAlpha(0.8f);
        Button button2 = findViewById(R.id.lowerSize_setUp);
        button2.setEnabled(true); button2.setAlpha(0.8f);
        tv.raiseSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void lowerSize(View view){
        final int TIME_TO_WAIT = 2500;
        Button button = findViewById(R.id.raiseSize_setUp);
        if (button.isEnabled()) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
            tv.lowerSize();
            return;
        }
        button.setEnabled(true);
        button.setAlpha(0.8f);
        Button button2 = findViewById(R.id.lowerSize_setUp);
        button2.setEnabled(true); button2.setAlpha(0.8f);
        tv.lowerSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void showTestRunMenu(View v) {


        // unRootedAndroid unRooted unroot

        log ("unRoot start");

        camDeviceConnection = camDeviceConnection = usbManager.openDevice(camDevice);
        unRootedSample.INSTANCE.main( camDeviceConnection.getFileDescriptor());
        log ("unRoot finished");


        
















        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText("No Camera connected.");
                    tv.setTextColor(darker(Color.RED, 50));}
            });
            return;
        } else if (camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||packetsPerRequest == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0  ) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText("The Values for the Camera are not correct set.\n\nPlease set up all the values for the camera first!");
                    tv.setTextColor(darker(Color.RED, 50));}
            });
            return;
        } else {
            Context wrapper = new ContextThemeWrapper(this, R.style.YOURSTYLE);
            PopupMenu popup = new PopupMenu(wrapper, v);
            // This activity implements OnMenuItemClickListener
            popup.inflate(R.menu.set_up_the_device_testrun_menubutton);
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.videoProbeCommit:
                            videoProbeCommitTransfer();
                            return true;
                        case R.id.testrun5sec:
                            isoRead5sec();
                            return true;
                        case R.id.testrun1frame:
                            isoRead1Frame();
                            return true;
                        default:
                            break;
                    }
                    return false;
                }
            });
            popup.show();
        }
    }

    public void searchTheCamera (View view) {
        if (camDevice == null) {
            try {
                findCam();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camDevice != null) {
                if (usbManager.hasPermission(camDevice)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log ("Camera has Usb permissions = ");
                            tv.setText("A camera has been found.\n\nThe Permissions to the Camera have been granted");
                            displayMessage("A camera has been found.");
                            tv.setTextColor(darker(Color.GREEN, 100));
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log ("Camera has no USB permissions ");
                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                            tv.setText("A camera is connected to your Android Device\nNo Usb Permissions for the Camera");
                            displayMessage("A camera is connected to your Android Device");
                            tv.setTextColor(darker(Color.RED, 50));
                        }
                    });
                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("No camera found\n\nIf your Android Device is on PIE or Q, it could be, that your Device does not support Usb Cameras\n\nSolutions:" +
                                "\n- Connect a camera and try again ...\n- Use a Android Device with a lower Android Version (e.g. Oreo or lower");
                        displayMessage("No camera found\nSolutions:\n- Connect a camera and try again ...");
                        tv.setTextColor(darker(Color.RED, 50));
                    }
                });
            }
        } else {
            if (usbManager.hasPermission(camDevice)) {
                log ("Camera has USB permissions ");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("A camera was found\n\n- The camera has Usb Permissions");
                        tv.setTextColor(darker(Color.GREEN, 100));
                    }
                });
            } else {
                log ("Camera has no Usb permissions, try to request... ");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("A camera was found\n\n- NO USB CAMERA PERMISSIOMS");
                        tv.setTextColor(darker(Color.RED, 50));
                    }
                });
                usbManager.requestPermission(camDevice, mPermissionIntent);
            }
        }
    }

    public void listDeviceButtonClickEvent(View view) {
        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("No Camera found.\nPlease connect first a camera and run 'Search for a camera' from the menu");
                    tv.setTextColor(darker(Color.RED, 50));}
            });
        } else {
            listDevice(camDevice);
            log ("deviceName = "+ deviceName);
        }
    }

    public void setUpWithUvcSettings(View view) {
        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("No Camera found.\nPlease connect a camera, or if allready connected run 'Search for a camera' from the menu");
                    tv.setTextColor(darker(Color.RED, 50));}
            });
        } else {
            camIsOpen = false;
            try {
                closeCameraDevice();
            } catch (Exception e) {
                displayErrorMessage(e);
                return;
            }
            try {
                openCam(false);
            } catch (Exception e) {
                displayErrorMessage(e);
                return;
            }
            if (camIsOpen) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText(stringBuilder.toString());
                        tv.setTextColor(Color.BLACK);
                    }
                });
            }
        }
    }

    public void editCameraSettings (View view) {
        if(buttonHandler != null) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler = null;
        }
        stf.startEditSave();
    }

    public void returnToConfigScreen(View view) {
        writeTheValues();
    }

    ///////////////////////////////////   Camera spezific methods   ////////////////////////////////////////////

    private void findCam() throws Exception {
        camDevice = findCameraDevice();
        if (camDevice == null) {
            camDevice = checkDeviceVideoClass();
            if (camDevice == null) throw new Exception("No USB camera device found.");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(camDevice.toString());
        int index = sb.indexOf("mManufacturerName");
        index += 18;
        deviceName = new String();
        while ( Character.isLetter(sb.charAt(index)) ) {
            deviceName += sb.charAt(index);
            index ++;
        }
        log("deviceName = " + deviceName);
        usbManager.requestPermission(camDevice, mPermissionIntent);
    }

    private UsbDevice checkDeviceVideoClass() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (checkDeviceHasVideoControlInterface(usbDevice)) {
                return usbDevice;
            }
        }
        return null;
    }

    private UsbDevice findCameraDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (checkDeviceHasVideoStreamingInterface(usbDevice)) {
                moveToNative = false;
                return usbDevice;
            }
        }
        return null;
    }

    private boolean checkDeviceHasVideoStreamingInterface(UsbDevice usbDevice) {
        return getVideoStreamingInterface(usbDevice) != null;
    }

    private boolean checkDeviceHasVideoControlInterface(UsbDevice usbDevice) {
        return getVideoControlInterface(usbDevice) != null;
    }

    private UsbInterface getVideoControlInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOCONTROL, false);
    }

    private UsbInterface getVideoStreamingInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOSTREAMING, true);
    }

    private UsbInterface findInterface(UsbDevice usbDevice, int interfaceClass, int interfaceSubclass, boolean withEndpoint) {
        int interfaces = usbDevice.getInterfaceCount();
        log("So many Interfaces found: " + interfaces);
        for (int i = 0; i < interfaces; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            log("usbInterface.getInterfaceClass() =  " + usbInterface.getInterfaceClass() + "  /  usbInterface.getInterfaceSubclass() = " + usbInterface.getInterfaceSubclass() + "  /  +  " +
                    "usbInterface.getEndpointCount() = "  + usbInterface.getEndpointCount());
            if (usbInterface.getInterfaceClass() == interfaceClass && usbInterface.getInterfaceSubclass() == interfaceSubclass && (!withEndpoint || usbInterface.getEndpointCount() > 0)) {
                return usbInterface;
            }
        }
        return null;
    }

    private void listDevice(UsbDevice usbDevice) {
        int a = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (usbDevice.getConfigurationCount()>1) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
                builderSingle.setIcon(R.drawable.ic_menu_camera);
                builderSingle.setTitle("Your camera has more than one configurations:");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
                for (int i = 0; i<usbDevice.getConfigurationCount(); i++){
                    arrayAdapter.add(Integer.toString(i));
                }
                builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = arrayAdapter.getItem(which);
                        int configurations = Integer.parseInt(input.toString());
                        System.out.println("usbDevice.getConfigurationCount() = " + usbDevice.getConfigurationCount());
                        System.out.println("configurations = " + configurations);
                        //camDeviceConnection.setConfiguration(usbDevice.getConfiguration(configurations));
                    }
                });
                builderSingle.show();
            } else log("1 Configuration found");
        }
        if (usbDevice.getInterfaceCount()==0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //setContentView(R.layout.layout_main);
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText("There is something wrong with your camera\n\nThere have not been detected enought interfaces from your usb device\n\n" + usbDevice.getInterfaceCount() + " - Interfaces have been found, but there should be at least more than 2");
                    tv.setTextColor(darker(Color.RED, 50));
                    tv.bringToFront();
                }
            });
            return;
        }
        else if (usbDevice.getInterfaceCount()==1) {
            convertedMaxPacketSize = new int [(usbDevice.getInterfaceCount())];
            stringBuilder = new StringBuilder();
            int interfaces = usbDevice.getInterfaceCount();
            for (int i = 0; i < interfaces; i++) {
                UsbInterface usbInterface = usbDevice.getInterface(i);
                log("Interface " + interfaces + " opened");
                log("    usbInterface.getId() = " + usbInterface.getId());
                log("    usbInterface.getInterfaceClass() = " + usbInterface.getInterfaceClass());
                log("    usbInterface.getInterfaceSubclass() = " + usbInterface.getInterfaceSubclass());
                log("    usbInterface.getEndpointCount() = " + usbInterface.getEndpointCount());
                log("  Start counting the endpoints:");
                StringBuilder logEntry = new StringBuilder("InterfaceID " + usbInterface.getId() +   "\n  [ Interfaceclass = " + usbInterface.getInterfaceClass() + " / InterfaceSubclass = " + usbInterface.getInterfaceSubclass() + " ]");
                stringBuilder.append(logEntry.toString());
                stringBuilder.append("\n");
                int endpoints = usbInterface.getEndpointCount();
                log("usbInterface.getEndpointCount() = " + usbInterface.getEndpointCount());
                for (int j = 0; j < endpoints; j++) {
                    UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                    log("- Endpoint: addr=" + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + " maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " type=" + usbEndpoint.getType() + " ]");
                    StringBuilder logEntry2 = new StringBuilder("    [ Endpoint " + j + " - addr " + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + ", maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " ]");
                    stringBuilder.append(logEntry2.toString());
                    stringBuilder.append("\n");
                    if (usbInterface.getId() == 1) {
                        convertedMaxPacketSize[a] = returnConvertedValue(usbEndpoint.getMaxPacketSize());
                        a++;
                    }
                    if (usbEndpoint.getAddress() == 0x03) {
                        camStreamingEndpoint = usbEndpoint;
                        log ("Endpointadress set");
                    }
                }
            }
            stringBuilder.append("\n\nYour Camera looks like to be no UVC supported device.\nThis means your camera can't be used by this app, because your camera can't be acessed over the Universal Video Class Protocoll");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText(stringBuilder.toString());
                    tv.setTextColor(Color.BLACK);
                    tv.bringToFront();
                }
            });
            moveToNative = true;
        }
        else {
            convertedMaxPacketSize = new int [(usbDevice.getInterfaceCount()-2)];
            log("Interface count: " + usbDevice.getInterfaceCount());
            int interfaces = usbDevice.getInterfaceCount();
            stringBuilder = new StringBuilder();
            boolean cont =false , stream = false;
            for (int i = 0; i < interfaces; i++) {
                UsbInterface usbInterface = usbDevice.getInterface(i);
                log("[ - Interface: " + usbInterface.getId()  + " class=" + usbInterface.getInterfaceClass() + " subclass=" + usbInterface.getInterfaceSubclass() );
                // UsbInterface.getAlternateSetting() has been added in Android 5.
                int endpoints = usbInterface.getEndpointCount();
                StringBuilder logEntry = new StringBuilder("InterfaceID " + usbInterface.getId() +   "\n    [ Interfaceclass = " + usbInterface.getInterfaceClass() + " / InterfaceSubclass = " + usbInterface.getInterfaceSubclass() + " ]");
                if (!cont) {
                    stringBuilder.append(logEntry.toString());
                    stringBuilder.append("\n");
                }
                else if (!stream) {
                    stringBuilder.append(logEntry.toString());
                    stringBuilder.append("\n");
                }
                if (usbInterface.getId() == 0) cont =true;
                else if (usbInterface.getId() == 1) stream =true;
                for (int j = 0; j < endpoints; j++) {
                    UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                    log("- Endpoint: address=" + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + " maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " type=" + usbEndpoint.getType() + " ]");
                    StringBuilder logEntry2 = new StringBuilder("        [ Endpoint " + Math.max(0, (i-1))  + " - address " + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + " - maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " ]");
                    stringBuilder.append(logEntry2.toString());
                    stringBuilder.append("\n");
                    if (usbInterface.getId() == 1) {
                        convertedMaxPacketSize[a] = returnConvertedValue(usbEndpoint.getMaxPacketSize());
                        a++;
                    }
                }
            }
            stringBuilder.append("\n\n\n\nThe number of the Endpoint represents the value of the Altsetting\nIf the Altsetting is 0 than the Video Control Interface will be used.\nIf the Altsetting is higher, than the Video Stream Interface with its specific Max Packet Size will be used");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //setContentView(R.layout.layout_main);
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText(stringBuilder.toString());
                    tv.bringToFront();
                    tv.setTextColor(Color.BLACK);
                }
            });
        }
    }

    private int returnConvertedValue(int wSize){
        String st = Integer.toBinaryString(wSize);
        StringBuilder result = new StringBuilder();
        result.append(st);
        if (result.length()<12) return Integer.parseInt(result.toString(), 2);
        else if (result.length() == 12) {
            String a = result.substring(0, 1);
            String b = result.substring(1, 12);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c+1)*d;
        } else {
            String a = result.substring(0, 2);
            String b = result.substring(2,13);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c+1)*d;
        }
    }

    public void closeCameraDevice() {

        if (moveToNative) {
            camDeviceConnection = null;
        }
        else if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
            camDeviceConnection = null;
        }
        camDeviceIsClosed = true;
    }

    private void openCam(boolean init) throws Exception {
        if (!usbManager.hasPermission(camDevice)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("No Permissions were granted to the Camera Device.");
                    tv.setTextColor(darker(Color.RED, 50));}
            });
        } else {
            openCameraDevice(init);
            if (moveToNative) return;
            if (init) {
                initCamera();
                if (compareStreamingParmsValues()) camIsOpen = true;
                else camIsOpen = false;
            }
            log("Camera opened sucessfully");
        }
    }

    private boolean compareStreamingParmsValues() {
        if ( !Arrays.equals( initStreamingParmsIntArray, probedStreamingParmsIntArray ) || !Arrays.equals( initStreamingParmsIntArray, finalStreamingParmsIntArray_first )  )  {
            StringBuilder s = new StringBuilder(128);

            if (initStreamingParmsIntArray[0] != finalStreamingParmsIntArray_first[0]) {
                s.append("The Controltransfer returned differnt Format Index's\n\n");
                s.append("Your entered 'Camera Format Index' Values is: " + initStreamingParmsIntArray[0] + "\n");
                s.append("The 'Camera Format Index' from the Camera Controltransfer is: " + finalStreamingParmsIntArray_first[0] + "\n");
            }
            if (initStreamingParmsIntArray[1] != finalStreamingParmsIntArray_first[1]) {
                s.append("The Controltransfer returned differnt Frame Index's\n\n");
                s.append("Your entered 'Camera Frame Index' Values is: " + initStreamingParmsIntArray[1] + "\n");
                s.append("The 'Camera Frame Index' from the Camera Controltransfer is: " + finalStreamingParmsIntArray_first[1] + "\n");
            }
            if (initStreamingParmsIntArray[2] != finalStreamingParmsIntArray_first[2]) {
                s.append("The Controltransfer returned differnt FrameIntervall Index's\n\n");
                s.append("Your entered 'Camera FrameIntervall' Values is: " + initStreamingParmsIntArray[2] + "\n");
                s.append("The 'Camera FrameIntervall' Value from the Camera Controltransfer is: " + finalStreamingParmsIntArray_first[2] + "\n");
            }
            s.append("The Values for the Control Transfer have a grey color in the 'edit values' screen\n");
            s.append("To get the correct values for you camera, read out the UVC specifications of the camera manualy, or try out the 'Set Up With UVC Settings' Button");
            initStreamingParmsResult = s.toString();
            log ("compareStreamingParmsValues returned false");
            return false;
        } else {
            initStreamingParmsResult = "Camera Controltransfer Sucessful !\n\nThe returned Values from the Camera Controltransfer fits to your entered Values\nYou can proceed starting a test run!";
            return true;
        }
    }

    public void stopLibUsbStreaming () {
        JNA_I_LibUsb.INSTANCE.stopStreaming();
        l1ibusbAutoRunning = false;
    }

    public void closeLibUsb () {
        JNA_I_LibUsb.INSTANCE.closeLibUsb();
        l1ibusbAutoRunning = false;
    }

    public void exitLibUsb () {
        JNA_I_LibUsb.INSTANCE.exit();
        l1ibusbAutoRunning = false;
    }

    private void openCameraDevice(boolean init) throws Exception {
        if (moveToNative) {
            camControlInterface = getVideoControlInterface(camDevice);
            camDeviceConnection = usbManager.openDevice(camDevice);
            return;
        }
        if (!libUsb) {
            if(libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming();
                JNA_I_LibUsb.INSTANCE.closeLibUsb();
                JNA_I_LibUsb.INSTANCE.exit();
                try {
                    findCam();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                libusb_is_initialized = false;
            }
        }
        // (For transfer buffer sizes > 196608 the kernel file drivers/usb/core/devio.c must be patched.)
        camControlInterface = getVideoControlInterface(camDevice);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (camControlInterface.getName() != null) deviceName = camControlInterface.getName();
        }
        camStreamingInterface = getVideoStreamingInterface(camDevice);
        log("camControlInterface = " + camControlInterface + "  //  camStreamingInterface = " + camStreamingInterface);
        if (camStreamingInterface.getEndpointCount() < 1) {
            throw new Exception("Streaming interface has no endpoint.");
        }
        camStreamingEndpoint = camStreamingInterface.getEndpoint(0);
        camControlEndpoint = camControlInterface.getEndpoint(0);
        bulkMode = camStreamingEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK;
        camDeviceConnection = usbManager.openDevice(camDevice);
        if (camDeviceConnection == null) {
            displayMessage("Failed to open the device - Retry");
            log("Failed to open the device - Retry");
            throw new Exception("Unable to open camera device connection.");
        }
        if (!libUsb) {
            if (!camDeviceConnection.claimInterface(camControlInterface, true)) {
                log("Failed to claim camControlInterface");
                displayMessage("Unable to claim camera control interface.");
                throw new Exception("Unable to claim camera control interface.");
            }
            if (!camDeviceConnection.claimInterface(camStreamingInterface, true)) {
                log("Failed to claim camStreamingInterface");
                displayMessage("Unable to claim camera streaming interface.");
                throw new Exception("Unable to claim camera streaming interface.");
            }
        }
        if (!init) {
            byte[] a = camDeviceConnection.getRawDescriptors();
            ByteBuffer uvcData = ByteBuffer.wrap(a);
            uvc_descriptor = new UVC_Descriptor(uvcData);
            CFAlertDialog alertDialog;
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this);
            LayoutInflater li = LayoutInflater.from(this);
            View setup_auto_manual_view = li.inflate(R.layout.set_up_the_device_manual_automatic, null);
            builder.setHeaderView(setup_auto_manual_view);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            Switch libUsbActivate = setup_auto_manual_view.findViewById(R.id.activateLibUsb);
            if (!(camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||packetsPerRequest == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0 )) {
                if (libUsb) libUsbActivate.setChecked(true);
                else libUsbActivate.setChecked(false);
            } else libUsb = true;
            alertDialog = builder.show();
            libUsbActivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) libUsb = true;
                    else libUsb = false;
                }
            });
            CFPushButton automatic = setup_auto_manual_view.findViewById(R.id.automatic) ;
            automatic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    automaticStart = true;
                    ProgressBar progressBar = findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.VISIBLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                            tv.setText("");
                            tv.setTextColor(Color.BLACK);
                        }
                    });

                    alertDialog.dismiss();
                }
            });
            CFPushButton manual = setup_auto_manual_view.findViewById(R.id.manual) ;
            manual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Set up from UVC manually
                    if (uvc_descriptor.phraseUvcData() == 0) {
                        if (convertedMaxPacketSize == null) listDevice(camDevice);
                        stf.setUpWithUvcValues(uvc_descriptor, convertedMaxPacketSize, false);
                    }
                    alertDialog.dismiss();
                }
            });
            alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    progress = "1% done";
                    if (automaticStart) {
                        // Automatic UVC Detection
                        packetsPerRequest = 1;
                        activeUrbs = 1;
                        closeCameraDevice();
                        doneTransfers = 0;
                        if (libUsb) {
                            startLibUsbAutoDetection();
                        } else {
                            startJnaAutoDetection();
                        }
                    }
                }
            });
        }
    }

    private final String getUSBFSName(final UsbDevice ctrlBlock) {
        String result = null;
        final String name = ctrlBlock.getDeviceName();
        final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
        if ((v != null) && (v.length > 2)) {
            final StringBuilder sb = new StringBuilder(v[0]);
            for (int i = 1; i < v.length - 2; i++)
                sb.append("/").append(v[i]);
            result = sb.toString();
        }
        if (TextUtils.isEmpty(result)) {
            log( "failed to get USBFS path, try to use default path:" + name);
            result = DEFAULT_USBFS;
        }
        return result;
    }


    public int getBus(String myString) {
        if(myString.length() > 3)
            return parseInt(myString.substring(myString.length()-7 , myString.length() - 4) ) ;
        else
            return 0;
    }

    public int getDevice(String myString) {
        if(myString.length() > 3)
            return parseInt(myString.substring(myString.length()-3)) ;
        else
            return 0;
    }


    private void initCamera() throws Exception {
        try {
            getVideoControlErrorCode();  // to reset previous error states
        }
        catch (Exception e) {
            log("Warning: getVideoControlErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        try{
            enableStreaming(false);
        }
        catch (Exception e){
            displayMessage("Warning: enable the Stream failed:\nPlease unplug and replug the camera, or reboot the device");
            log("Warning: enableStreaming(false) failed: " + e);
        }
        try {
            getVideoStreamErrorCode();
        }                // to reset previous error states
        catch (Exception e) {
            log("Warning: getVideoStreamErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        initStreamingParms();
    }

    private void initStreamingParms() throws Exception {
        thorthCTLfailed = false;
        controlErrorlog = new String();
        stringBuilder = new StringBuilder();
        final int timeout = 5000;
        int usedStreamingParmsLen;
        int len;
        byte[] streamingParms = new byte[26];
        // The e-com module produces errors with 48 bytes (UVC 1.5) instead of 26 bytes (UVC 1.1) streaming parameters! We could use the USB version info to determine the size of the streaming parameters.
        streamingParms[0] = (byte) 0x01;                // (0x01: dwFrameInterval) //D0: dwFrameInterval //D1: wKeyFrameRate // D2: wPFrameRate // D3: wCompQuality // D4: wCompWindowSize
        //if(convertedMaxPacketSize.length == 1) streamingParms[0] = (byte) 0x00;
        streamingParms[2] = (byte) camFormatIndex;                // bFormatIndex
        streamingParms[3] = (byte) camFrameIndex;                 // bFrameIndex
        packUsbInt(camFrameInterval, streamingParms, 4);         // dwFrameInterval
        initStreamingParms = dumpStreamingParms(streamingParms);
        initStreamingParmsIntArray = getStreamingParmsArray(streamingParms);
        log("Initial streaming parms: " + initStreamingParms);
        stringBuilder.append("Initial streaming parms: \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_PROBE_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            controlErrorlog += "Error during sending Probe Streaming Parms (1st)\nLength = " + len;


            throw new Exception("Camera initialization failed. Streaming parms probe set failed, len=" + len + ".");
        }
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_PROBE_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            controlErrorlog += "Error during receiving Probe Streaming Parms (2nd)\nLength = " + len;
            throw new Exception("Camera initialization failed. Streaming parms probe get failed.");
        }
        probedStreamingParms = dumpStreamingParms(streamingParms);
        probedStreamingParmsIntArray =  getStreamingParmsArray(streamingParms);
        log("Probed streaming parms: " + probedStreamingParms);
        stringBuilder.append("\nProbed streaming parms:  \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        usedStreamingParmsLen = len;
        // log("Streaming parms length: " + usedStreamingParmsLen);
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            controlErrorlog += "Error during sending Commit Streaming Parms (3rd)\nLength = " + len;
            throw new Exception("Camera initialization failed. Streaming parms commit set failed.");
        }
        finalStreamingParms_first =  dumpStreamingParms(streamingParms);
        finalStreamingParmsIntArray_first = getStreamingParmsArray(streamingParms);
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            thorthCTLfailed = true;
            controlErrorlog += "Error during receiving final Commit Streaming Parms (4th)\nLength = " + len;
            log("Camera initialization failed. Streaming parms commit get failed. Length = " + len);
            //throw new Exception("Camera initialization failed. Streaming parms commit get failed.");
        }
        finalStreamingParms = dumpStreamingParms(streamingParms);
        finalStreamingParmsIntArray = getStreamingParmsArray(streamingParms);
        log("Final streaming parms: " + finalStreamingParms);
        stringBuilder.append("\nFinal streaming parms: \n");
        stringBuilder.append(finalStreamingParms);
        controlltransfer = finalStreamingParms;
    }

    private String dumpStreamingParms(byte[] p) {
        StringBuilder s = new StringBuilder(128);
        s.append("[ hint=0x" + Integer.toHexString(unpackUsbUInt2(p, 0)));
        s.append(" / format=" + (p[2] & 0xf));
        s.append(" / frame=" + (p[3] & 0xf));
        s.append(" / frameInterval=" + unpackUsbInt(p, 4));
        s.append(" / keyFrameRate=" + unpackUsbUInt2(p, 8));
        s.append(" / pFrameRate=" + unpackUsbUInt2(p, 10));
        s.append(" / compQuality=" + unpackUsbUInt2(p, 12));
        s.append(" / compWindowSize=" + unpackUsbUInt2(p, 14));
        s.append(" / delay=" + unpackUsbUInt2(p, 16));
        s.append(" / maxVideoFrameSize=" + unpackUsbInt(p, 18));
        s.append(" / maxPayloadTransferSize=" + unpackUsbInt(p, 22));
        s.append(" ]");
        return s.toString();
    }

    private int[] getStreamingParmsArray(byte[] p) {
        int[] array = new int [3];
        array[0] = p[2] & 0xf;
        array[1] = p[3] & 0xf;
        array[2] = unpackUsbInt(p, 4);
        return array;
    }

    private static int unpackUsbInt(byte[] buf, int pos) {
        return unpackInt(buf, pos, false);
    }

    private static int unpackUsbUInt2(byte[] buf, int pos) {
        return ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
    }

    private static void packUsbInt(int i, byte[] buf, int pos) {
        packInt(i, buf, pos, false);
    }

    private static void packInt(int i, byte[] buf, int pos, boolean bigEndian) {
        if (bigEndian) {
            buf[pos] = (byte) ((i >>> 24) & 0xFF);
            buf[pos + 1] = (byte) ((i >>> 16) & 0xFF);
            buf[pos + 2] = (byte) ((i >>> 8) & 0xFF);
            buf[pos + 3] = (byte) (i & 0xFF);
        } else {
            buf[pos] = (byte) (i & 0xFF);
            buf[pos + 1] = (byte) ((i >>> 8) & 0xFF);
            buf[pos + 2] = (byte) ((i >>> 16) & 0xFF);
            buf[pos + 3] = (byte) ((i >>> 24) & 0xFF);
        }
    }

    private static int unpackInt(byte[] buf, int pos, boolean bigEndian) {
        if (bigEndian) {
            return (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
        } else {
            return (buf[pos + 3] << 24) | ((buf[pos + 2] & 0xFF) << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
        }
    }

    private void enableStreaming(boolean enabled) throws Exception {
        enableStreaming_usbFs(enabled);
    }

    private void enableStreaming_usbFs(boolean enabled) throws Exception {
        if (enabled && bulkMode) {
            // clearHalt(camStreamingEndpoint.getAddress());
        }
        int altSetting = enabled ? camStreamingAltSetting : 0;
        // For bulk endpoints, altSetting is always 0.
        log("setAltSetting");
        log("usbIso.setInterface(camDeviceConnection.getFileDescriptor(), altSetting);     =    InterfaceID = "  + camStreamingInterface.getId() + "  /  altsetting ="+   altSetting);
        usbdevice_fs_util.setInterface(camDeviceConnection.getFileDescriptor(), camStreamingInterface.getId(), altSetting);
    }

    // Resets the error code after retrieving it.
    private int getVideoControlErrorCode() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 99;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VC_REQUEST_ERROR_CODE_CONTROL << 8, 0, buf, 1, 1000);
        if (len != 1) {
            throw new Exception("VC_REQUEST_ERROR_CODE_CONTROL failed, len=" + len + ".");
        }
        return buf[0];
    }

    private int getVideoStreamErrorCode() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 99;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STREAM_ERROR_CODE_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len == 0) {
            return 0;
        }                   // ? (Logitech C310 returns len=0)
        if (len != 1) {
            throw new Exception("VS_STREAM_ERROR_CODE_CONTROL failed, len=" + len + ".");
        }
        return buf[0];
    }

    private static String hexDump(byte[] buf, int len) {
        StringBuilder s = new StringBuilder(len * 3);
        for (int p = 0; p < len; p++) {
            if (p > 0) {
                s.append(' ');
            }
            int v = buf[p] & 0xff;
            if (v < 16) {
                s.append('0');
            }
            s.append(Integer.toHexString(v));
        }
        return s.toString();
    }

    class IsochronousRead extends Thread {

        SetUpTheUsbDevice setUpTheUsbDevice;
        Context mContext;
        Activity activity;
        StringBuilder stringBuilder;

        public IsochronousRead(SetUpTheUsbDevice setUpTheUsbDevice, Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
            this.setUpTheUsbDevice = setUpTheUsbDevice;
            this.mContext = mContext;
            activity = (Activity)mContext;
        }

        public void run() {
            try {
                USBIso usbIso64 = new USBIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize, (byte) camStreamingEndpoint.getAddress());
                usbIso64.preallocateRequests(activeUrbs);
                //ArrayList<String> logArray = new ArrayList<>(512);
                int packetCnt = 0;
                int packet0Cnt = 0;
                int packet12Cnt = 0;
                int packetDataCnt = 0;
                int packetHdr8Ccnt = 0;
                int packetErrorCnt = 0;
                int frameCnt = 0;
                final long time0 = System.currentTimeMillis();
                int frameLen = 0;
                int requestCnt = 0;
                byte[] data = new byte[maxPacketSize];
                try {
                    enableStreaming(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                usbIso64.submitUrbs();
                final int time = 5000;
                int cnt = 0;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Controlltransfer:\n");
                stringBuilder.append(controlltransfer);
                stringBuilder.append(String.format("\n\nCounted Frames in a Time of %d seconds:\n", (time/1000)));
                Thread th = new Thread(new Runnable() {
                    private long startTime = System.currentTimeMillis();
                    public void run() {
                        while ((time0+time) > System.currentTimeMillis()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                                    tv.setText(String.format("The camera stream will be read out for %d Seconds\nLasting seconds: ",(time/1000), (time/1000))+((System.currentTimeMillis()-startTime)/1000));
                                    tv.setTextColor(Color.BLACK);
                                }
                            });
                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                th.start();
                while (System.currentTimeMillis() - time0 < time) {
                    boolean stopReq = false;
                    USBIso.Request req = usbIso64.reapRequest(true);
                    for (int packetNo = 0; packetNo < req.getNumberOfPackets(); packetNo++) {
                        packetCnt++;
                        int packetLen = req.getPacketActualLength(packetNo);
                        if (packetLen == 0) {
                            packet0Cnt++;
                        }
                        if (packetLen == 12) {
                            packet12Cnt++;
                        }
                        if (packetLen == 0) {
                            continue;
                        }
                        StringBuilder logEntry = new StringBuilder(requestCnt + "/" + packetNo + " len=" + packetLen);
                        int packetStatus = req.getPacketStatus(packetNo);
                        if (packetStatus != 0) {
                            System.out.println("Packet status=" + packetStatus);
                            stopReq = true;
                            break;
                        }
                        if (packetLen > 0) {
                            if (packetLen > maxPacketSize) {
                                //throw new Exception("packetLen > maxPacketSize");
                            }
                            req.getPacketData(packetNo, data, packetLen);
                            logEntry.append(" data=" + hexDump(data, Math.min(32, packetLen)));
                            int headerLen = data[0] & 0xff;

                            try {
                                if (headerLen < 2 || headerLen > packetLen) {
                                    //    skipFrames = 1;
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid payload header length.");
                            }
                            int headerFlags = data[1] & 0xff;
                            if (headerFlags == 0x8c) {
                                packetHdr8Ccnt++;
                            }
                            // logEntry.append(" hdrLen=" + headerLen + " hdr[1]=0x" + Integer.toHexString(headerFlags));
                            int dataLen = packetLen - headerLen;
                            if (dataLen > 0) {
                                packetDataCnt++;
                            }
                            frameLen += dataLen;
                            if ((headerFlags & 0x40) != 0) {
                                logEntry.append(" *** Error ***");
                                packetErrorCnt++;
                            }
                            if ((headerFlags & 2) != 0) {
                                logEntry.append(" EOF frameLen=" + frameLen);
                                frameCnt++;
                                stringBuilder.append(String.format("Frame %d frameLen = %d\n", ++cnt, frameLen));
                                frameLen = 0;
                            }
                        }
                        //logArray.add(logEntry.toString());
                    }
                    if (stopReq) {
                        break;
                    }else if (packetErrorCnt > 800) break;
                    requestCnt++;
                    req.initialize();
                    try {
                        req.submit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    enableStreaming(false);
                } catch (Exception e) {
                    log("Exception during enableStreaming(false): " + e);
                }
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" +
                        packet12Cnt + ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);
                if (packetErrorCnt > 800) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Your Camera only return Error frames!\nPlease change your camera values\n");
                    stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" +
                            "\npacketCnt= " + packetCnt + " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +
                            "\npacket0Cnt= " + packet0Cnt + " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= "
                            + packetDataCnt +" (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText(stringBuilder.toString());
                        tv.setTextColor(Color.BLACK);
                    }
                });
                runningTransfer = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class IsochronousRead1Frame extends Thread {
        SetUpTheUsbDevice setUpTheUsbDevice;
        Context mContext;
        Activity activity;
        StringBuilder stringBuilder;
        public IsochronousRead1Frame(SetUpTheUsbDevice setUpTheUsbDevice, Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
            this.setUpTheUsbDevice = setUpTheUsbDevice;
            this.mContext = mContext;
            activity = (Activity)mContext;
        }
        public void run() {
            try {
                USBIso usbIso64 = new USBIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize, (byte) camStreamingEndpoint.getAddress());
                usbIso64.preallocateRequests(activeUrbs);
                //Thread.sleep(500);
                ArrayList<String> logArray = new ArrayList<>(512);
                int packetCnt = 0;
                int packet0Cnt = 0;
                int packet12Cnt = 0;
                int packetDataCnt = 0;
                int packetHdr8Ccnt = 0;
                int packetErrorCnt = 0;
                int frameCnt = 0;
                final long time0 = System.currentTimeMillis();
                int frameLen = 0;
                int requestCnt = 0;
                byte[] data = new byte[maxPacketSize];
                try {
                    enableStreaming(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                usbIso64.submitUrbs();
                int cnt = 0;
                stringBuilder = new StringBuilder();
                stringBuilder.append("One Frame received:\n\n");
                while (frameCnt < 1) {
                    boolean stopReq = false;
                    USBIso.Request req = usbIso64.reapRequest(true);
                    for (int packetNo = 0; packetNo < req.getNumberOfPackets(); packetNo++) {
                        packetCnt++;
                        int packetLen = req.getPacketActualLength(packetNo);
                        if (packetLen == 0) {
                            packet0Cnt++;
                        }
                        if (packetLen == 12) {
                            packet12Cnt++;
                        }
                        if (packetLen == 0) {
                            continue;
                        }
                        StringBuilder logEntry = new StringBuilder(requestCnt + "/" + packetNo + " len=" + packetLen);
                        int packetStatus = req.getPacketStatus(packetNo);
                        if (packetStatus != 0) {
                            System.out.println("Packet status=" + packetStatus);
                            stopReq = true;
                            break;
                        }
                        if (packetLen > 0) {
                            if (packetLen > maxPacketSize) {
                                //throw new Exception("packetLen > maxPacketSize");
                            }
                            req.getPacketData(packetNo, data, packetLen);
                            logEntry.append("bytes // data = " + hexDump(data, Math.min(32, packetLen)));
                            int headerLen = data[0] & 0xff;
                            try {
                                if (headerLen < 2 || headerLen > packetLen) {
                                    //    skipFrames = 1;
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid payload header length.");
                            }
                            int headerFlags = data[1] & 0xff;
                            if (headerFlags == 0x8c) {
                                packetHdr8Ccnt++;
                            }
                            int dataLen = packetLen - headerLen;
                            if (dataLen > 0) {
                                packetDataCnt++;
                            }
                            frameLen += dataLen;
                            if ((headerFlags & 0x40) != 0) {
                                logEntry.append(" *** Error ***");
                                packetErrorCnt++;
                            }
                            if ((headerFlags & 2) != 0) {
                                logEntry.append(" EOF frameLen=" + frameLen);
                                frameCnt++;
                                stringBuilder.append("  -  " + frameLen + "  bytes  - \n\n");
                                stringBuilder.append(String.format("The first Frame is %d byte long\n", frameLen));
                                break;
                            }
                        }
                        logArray.add(logEntry.toString());
                    }
                    if (frameCnt > 0)  break;
                    else if (packetErrorCnt > 800) break;
                    requestCnt++;
                    req.initialize();
                    try {
                        req.submit();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    enableStreaming(false);
                } catch (Exception e) {
                    log("Exception during enableStreaming(false): " + e);
                }
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" + packet12Cnt +
                        ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);
                if (packetErrorCnt > 800) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Your Camera only return Error frames!\nPlease change your camera values\n");
                    stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" + "\npacketCnt= " +
                            packetCnt + " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +  "\npacket0Cnt= " + packet0Cnt +
                            " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= " + packetDataCnt +
                            " (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");
                }
                stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" + "\npacketCnt= " + packetCnt +
                        " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +  "\npacket0Cnt= " + packet0Cnt +
                        " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= " + packetDataCnt +
                        " (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");
                stringBuilder.append("Explaination: The first number is the Requestnumber and the second number is the data packet from this request.\n" +
                        "The comes the data length of this packet with: 'len='" +
                        "\nThe 'data= ' shows the first 20 Hex values wich were stored in this packet\n(There are more values stored in this packet, but not displayed, ...)");
                stringBuilder.append("Here is the structure of the Frame:\n\n");
                for (String s : logArray) {
                    stringBuilder.append("\n\n");
                    stringBuilder.append(s);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText(stringBuilder.toString() );
                        tv.setTextColor(Color.BLACK);
                    }
                });
                runningTransfer1Frame = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int videoFormatToInt () {
        if(videoformat.equals("MJPEG")) return 1;
        else if (videoformat.equals("YUY2")) return 0;
        else return 0;
    }

    private void  isoRead1Frame() {
        if (!usbManager.hasPermission(camDevice)) {
            int a;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            // IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            // registerReceiver(mUsbReceiver, filter);
            usbManager.requestPermission(camDevice, permissionIntent);
            while (!usbManager.hasPermission(camDevice)) {
                long time0 = System.currentTimeMillis();
                for (a = 0; a < 10; a++) {
                    while (System.currentTimeMillis() - time0 < 1000) {
                        if (usbManager.hasPermission(camDevice)) break;
                    }
                }
                if (usbManager.hasPermission(camDevice)) break;
                if ( a >= 10) break;
            }
        }
        //if (camDeviceConnection != null || camStreamingInterface != null) closeCameraDevice();
        if(libUsb) {
            if(!libusb_is_initialized) {
                libusb_is_initialized = true;
            }

            try {
                if (camDeviceConnection == null) {
                    findCam();
                    openCameraDevice(true);
                }
                if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                if(productID == 0) productID = camDevice.getProductId();
                if(vendorID == 0) vendorID = camDevice.getVendorId();
                if(adress == null)  adress = camDevice.getDeviceName();


                if (moveToNative) {
                    // fetch The camStreamingEndpointAdressJNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                    //                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int);
                    if (camStreamingEndpointAdress == 0) camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(camDeviceConnection.getFileDescriptor());
                } else if (camStreamingEndpointAdress == 0) camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);
                int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                        camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, 0, bcdUVC_int);
            } catch (Exception e) {
                e.printStackTrace();
            }

            latch = new CountDownLatch(1);
            JNA_I_LibUsb.INSTANCE.setCallback(new JNA_I_LibUsb.eventCallback(){
                public boolean callback(Pointer videoFrame, int frameSize) {
                    log("frame received");
                    sframeCnt ++;
                    log("Event Callback called:\nFrameLength = " + frameSize);
                    latch.countDown();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Received one Frame with LibUsb:\n\n");
                    stringBuilder.append("Length = " + frameSize + "\n");
                    if (frameSize == (imageWidth*imageHeight*2)) stringBuilder.append("\nThe Frame length matches it's expected size.\nThis are the first 20 bytes of the frame:");
                    stringBuilder.append("\ndata = " + hexDump(videoFrame.getByteArray(0,50), Math.min(32, 50)));
                    /*
                    byte [] data = videoFrame.getByteArray(0, frameSize);
                    String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                    File file = new File(rootPath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    String fileName = new File(rootPath + "bin" + ".yuv").getPath() ;
                    FileOutputStream fileOutputStream = null;
                    try {
                        fileOutputStream = null;
                        try {
                            fileOutputStream = new FileOutputStream(fileName);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                        try {
                            fileOutputStream.write(data);
                            fileOutputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally {
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                     */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                            tv.setText(stringBuilder.toString());
                            tv.setTextColor(Color.BLACK);
                        }
                    });
                    return true;
                }
            });
            //JniIsoStreamActivity(null, 0, 0);
            JNA_I_LibUsb.INSTANCE.getFramesOverLibUsb( videoFormatToInt(), 0, 1);
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            JNA_I_LibUsb.INSTANCE.stopStreaming();
        } else {
            if(libusb_is_initialized) {
                //JNA_I_LibUsb.INSTANCE.stopStreaming();
                //JNA_I_LibUsb.INSTANCE.closeLibUsb();
                //JNA_I_LibUsb.INSTANCE.exit();
                try {
                    findCam();
                    openCam(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                libusb_is_initialized = false;
            }
            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camIsOpen) {
                if (runningTransfer1Frame != null) {
                    return;
                }
                runningTransfer1Frame = new IsochronousRead1Frame(this, this);
                runningTransfer1Frame.start();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Failed to initialise the camera" + initStreamingParmsResult);
                        tv.setTextColor(Color.BLACK);
                    }
                });
            }
        }
    }

    private void isoRead5sec() {
        if (!usbManager.hasPermission(camDevice)) {
            int a;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(camDevice, permissionIntent);
            while (!usbManager.hasPermission(camDevice)) {
                long time0 = System.currentTimeMillis();
                for (a = 0; a < 10; a++) {
                    while (System.currentTimeMillis() - time0 < 1000) {
                        if (usbManager.hasPermission(camDevice)) break;
                    }
                }
                if (usbManager.hasPermission(camDevice)) break;
                if ( a >= 10) break;
            }
        }
        if(libUsb) {
            //if (camDeviceConnection != null || camStreamingInterface != null) closeCameraDevice();
            if(!libusb_is_initialized) {
                libusb_is_initialized = true;
            }
            try {
                if (camDeviceConnection == null) {
                    findCam();
                    openCameraDevice(true);
                }
                if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                if(productID == 0) productID = camDevice.getProductId();
                if(vendorID == 0) vendorID = camDevice.getVendorId();
                if(adress == null)  adress = camDevice.getDeviceName();
                if (moveToNative) {
                    // fetch The camStreamingEndpointAdressJNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                    //                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int);
                    if(camStreamingEndpointAdress == 0) camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(camDeviceConnection.getFileDescriptor());
                } else if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);
                int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                        camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, 0, bcdUVC_int);
            } catch (Exception e) {
                e.printStackTrace();
            }
            sframeCnt = 0;
            final long time0 = System.currentTimeMillis();
            final int time = 5000;
            //latch = new CountDownLatch(1);
            List<Integer> myVar = new ArrayList<Integer>();
            ArrayList<String> logArray = new ArrayList<>(512);
            stringBuilder = new StringBuilder();
            Thread th = new Thread(new Runnable() {
                private long startTime = System.currentTimeMillis();
                private boolean started = false;
                public void run() {
                    while ((time0+time) > System.currentTimeMillis()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                                tv.setVisibility(View.GONE);
                                ConstraintLayout fadingTextView = (ConstraintLayout) findViewById(R.id.fadingTextViewLayout);
                                fadingTextView.setVisibility(View.VISIBLE);
                                FadingTextView FTV = (FadingTextView) findViewById(R.id.fadingTextView);
                                FTV.setTimeout(250, MILLISECONDS);
                                FTV.setVisibility(View.VISIBLE);
                                String[] texts = {"0   Sec","0,5 Sec","1   Sec","1,5 Sec","2   Sec","2,5 Sec","3   Sec", "3,5 Sec","4   Sec","4,5 Sec","5   Sec","5,5 Sec",
                                        "6   Sec","6,5 Sec","7   Sec", "7,5 Sec","8   Sec", "8,5 Sec","9   Sec","9,5 Sec"};
                                FTV.setTexts(texts);
                                FTV.forceRefresh();
                            }
                        });
                        try {
                            Thread.sleep(100);
                            if (!started) {
                                JNA_I_LibUsb.INSTANCE.setCallback(new JNA_I_LibUsb.eventCallback(){
                                    public boolean callback(Pointer videoFrame, int frameSize) {
                                        log("frame received " + sframeCnt);
                                        sframeCnt ++;
                                        myVar.add(frameSize);
                                        logArray.add("bytes // data = " + hexDump(videoFrame.getByteArray(0,50), Math.min(32, 50)));
                                        return true;
                                    }
                                });
                                JNA_I_LibUsb.INSTANCE.getFramesOverLibUsb(videoFormatToInt(), 0, 5);
                                started = true;
                            }
                            Thread.sleep(900);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    JNA_I_LibUsb.INSTANCE.stopStreaming();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                            tv.setVisibility(View.VISIBLE);
                            stringBuilder.append(String.format("Counted Frames in a Time of %d seconds:\n", (time/1000)));
                            stringBuilder.append("You received " + myVar.size() + " Frames over LibUsb\n\n");
                            for (Integer s : myVar) {
                                stringBuilder.append("\n\n");
                                stringBuilder.append("Frame len = " + s);
                            }
                            for (String s : logArray) {
                                stringBuilder.append("\n\n");
                                stringBuilder.append(s);
                            }
                            tv.setText(stringBuilder.toString());
                            ConstraintLayout fadingTextView = (ConstraintLayout) findViewById(R.id.fadingTextViewLayout);
                            fadingTextView.setVisibility(View.GONE);
                            fadingTextView.setVisibility(View.INVISIBLE);
                            FadingTextView FTV = (FadingTextView) findViewById(R.id.fadingTextView);
                            FTV.setVisibility(View.GONE);
                            FTV.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            });
            th.start();
        } else {
            if(libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming();
                JNA_I_LibUsb.INSTANCE.closeLibUsb();
                JNA_I_LibUsb.INSTANCE.exit();
                try {
                    findCam();
                    openCam(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                libusb_is_initialized = false;
            }
            closeCameraDevice();
            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camIsOpen) {
                if (runningTransfer != null) {
                    return;
                }
                runningTransfer = new IsochronousRead(this, this);
                runningTransfer.start();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Failed to initialise the camera" + initStreamingParmsResult);
                        tv.setTextColor(Color.BLACK);
                    }
                });
            }
        }
    }

    private void videoProbeCommitTransfer() {
        log("VideoProbeCommitControl");
        if (!usbManager.hasPermission(camDevice)) {
            int a;
            PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            usbManager.requestPermission(camDevice, permissionIntent);
            while (!usbManager.hasPermission(camDevice)) {
                long time0 = System.currentTimeMillis();
                for (a = 0; a < 10; a++) {
                    while (System.currentTimeMillis() - time0 < 1000) {
                        if (usbManager.hasPermission(camDevice)) break;
                    }
                }
                if (usbManager.hasPermission(camDevice)) break;
                if ( a >= 10) break;
            }
        }
        if(libUsb) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!libusb_is_initialized) {
                        try {
                            if (camDeviceConnection == null) {
                                log("camDeviceConnection == null");
                                findCam();
                                openCameraDevice(true);
                            }
                            if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                            if(productID == 0) productID = camDevice.getProductId();
                            if(vendorID == 0) vendorID = camDevice.getVendorId();
                            if(adress == null)  adress = camDevice.getDeviceName();
                            if (moveToNative) {
                                // fetch The camStreamingEndpointAdressJNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                //                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int);
                                if(camStreamingEndpointAdress == 0) camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(camDeviceConnection.getFileDescriptor());
                            } else if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                            if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);
                            int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                            log("JNA_I_LibUsb.INSTANCE.set_the_native_Values");
                            JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                    camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat,0, bcdUVC_int );
                            libusb_is_initialized = true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    boolean one = false, two = false, three = false, four = false;
                    int rc = -1;
                    try {
                        log("libusb_openCam(true)");
                        rc = libusb_openCam(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (camDeviceIsClosed) {
                        try {
                            if (!moveToNative) openCam(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        camDeviceIsClosed = false;
                    }
                    if (camDeviceConnection == null) {
                        log("2 camDeviceConnection == null 2");

                        if (moveToNative) return;
                        try {
                            findCam();
                            openCameraDevice(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    log("2 camDeviceConnection == null 2");
                    Pointer ctlValues = JNA_I_LibUsb.INSTANCE.probeCommitControl(1, camFormatIndex, camFrameIndex,  camFrameInterval, camDeviceConnection.getFileDescriptor());
                    if (ctlValues == null) {
                        //JNA_I_LibUsb.INSTANCE.probeCommitControl_cleanup();
                        tv.setText("The Control Transfers to the Camera failed!\n\nSolutions: Try to connect the camera without LibUsb\n\n" );
                        return ;
                    }
                    byte[] buf = new byte[26];
                    buf = ctlValues.getByteArray(4, 26);
                    if(isEmpty(buf)) one = true;
                    log("1st Values:\n" + (Arrays.toString(buf)));
                    initStreamingParms = dumpStreamingParms(buf);
                    initStreamingParmsIntArray = getStreamingParmsArray(buf);
                    buf = ctlValues.getByteArray(51, 26);
                    if(isEmpty(buf)) two = true;
                    log("2nd Values:\n" + (Arrays.toString(buf)));
                    probedStreamingParms = dumpStreamingParms(buf);
                    probedStreamingParmsIntArray =  getStreamingParmsArray(buf);
                    buf = ctlValues.getByteArray(99, 26);
                    if(isEmpty(buf)) three = true;
                    log("3rd Values:\n" + (Arrays.toString(buf)));
                    finalStreamingParms_first = dumpStreamingParms(buf);
                    finalStreamingParmsIntArray_first = getStreamingParmsArray(buf);
                    buf = ctlValues.getByteArray(147, 26);
                    if(isEmpty(buf)) four = true;
                    log("4th Values:\n" + (Arrays.toString(buf)));
                    finalStreamingParms = dumpStreamingParms(buf);
                    finalStreamingParmsIntArray = getStreamingParmsArray(buf);
                    if (compareStreamingParmsValues()) camIsOpen = true;
                    else camIsOpen = false;
                    StringBuilder sb = new StringBuilder();
                    sb.append(initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" );
                    if (one) sb.append("FAILED - The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "");
                    else sb.append("The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "" );
                    if (two) sb.append("\n\nFAILED - The second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "");
                    else sb.append("\n\nThe second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "");
                    if (three) sb.append("\n\nFAILED - The third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first);
                    else sb.append("\n\nThe third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first );
                    if (four) sb.append( "\n\nFAILED - The Last Commit Controltransfer for receiving the final Camera Values:\n" + finalStreamingParms);
                    else sb.append( "\n\nThe Last Commit Controltransfer for receiving the final Camera Values:\n" + finalStreamingParms);


                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText(sb.toString());
                    tv.setTextColor(Color.BLACK);
                    //JNA_I_LibUsb.INSTANCE.probeCommitControl_cleanup();
                    log("Control probeCommitControl End");
                }
            });
        } else {
            if(libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming();
                JNA_I_LibUsb.INSTANCE.closeLibUsb();
                JNA_I_LibUsb.INSTANCE.exit();
                try {
                    findCam();
                    openCam(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                libusb_is_initialized = false;
            }
            closeCameraDevice();
            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camIsOpen) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        if (thorthCTLfailed == false) tv.setText(initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" +
                                "The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "" +
                                "\n\nThe second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "" +
                                "\n\nThe Last Commit Controltransfer for receiving the final Camera Values from the Camera: \n" + finalStreamingParms);
                        else tv.setText(initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" +
                                "The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "" +
                                "\n\nThe second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "" +
                                "\n\nThe third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first +
                                "\n\nThe Last Commit Controltransfer for receiving the final Camera Values from the Camera failed");
                        tv.setTextColor(Color.BLACK);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Failed to initialise the camera\n\n" + initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" +
                                "The first Controltransfer for sending the Values to the Camera: \n" + initStreamingParms +
                                "\n\nThe second Controltransfer for probing the values with the camera:\n" + probedStreamingParms +
                                "\n\nThe third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first +
                                "\n\nThe Last Controltransfer for receiving the final Camera Values from the Camera: \n" + finalStreamingParms +
                                "\n\nErrorlog:\n" + controlErrorlog
                        );
                        tv.setTextColor(darker(Color.RED, 50));
                    }
                });
            }
        }
    }

    private int libusb_openCam(boolean init)  {
        libusb_openCameraDevice();
        if (init) {
            // Libusb Camera initialisation
            // -1 on error
            // 0 onSucess
            int ret = libusb_initCamera();
            if (ret < 0) log("Libusb Camera Initialisation failed");
            else if (ret == 0) log("Libusb Camera already initialized");
            else log("Libusb Camera Initialisation sucessful");
            return ret;
        }
        log("Camera opened sucessfully");
        return -1;
    }

    private int libusb_initCamera() {
        if (!camera_is_initialized_over_libusb) {
            camera_is_initialized_over_libusb = true;
            return JNA_I_LibUsb.INSTANCE.initStreamingParms(camDeviceConnection.getFileDescriptor());
        }
        return 0;
    }

    private void libusb_openCameraDevice() {
        fd = camDeviceConnection.getFileDescriptor();
        if (moveToNative) {
            // fetch The camStreamingEndpointAdressJNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
            //                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int);
            if(camStreamingEndpointAdress == 0) camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(camDeviceConnection.getFileDescriptor());
        } else if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
        int framesReceive = 1;
        if (fiveFrames) framesReceive = 5;
        int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
        if (moveToNative) JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, 1, videoformat, framesReceive, bcdUVC_int);
        else JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int);
        libusb_is_initialized = true;
    }

    //////////////////////////////////  General Methods    //////////////////////////////////

    private static void packIntBrightness(int i, byte[] buf) {
        buf[0] = (byte) (i & 0xFF);
        buf[0 + 1] = (byte) ((i >>> 8) & 0xFF);
    }

    private static int unpackIntBrightness(byte[] buf) {
            return (((buf[1] ) << 8) | (buf[0] & 0xFF));
    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SetUpTheUsbDevice.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void log(String msg) {
        Log.i("UVC_Camera_Set_Up", msg);
    }

    public void displayErrorMessage(Throwable e) {
        Log.e("UVC_Camera", "Error in MainActivity", e);
        displayMessage("Error: " + e);
    }

    private void fetchTheValues(){
        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("bun");
        if (bundle.getBoolean("edit") == true) {
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
            deviceName=bundle.getString("deviceName");
            bUnitID = bundle.getByte("bUnitID",(byte)0);
            bTerminalID = bundle.getByte("bTerminalID",(byte)0);
            bNumControlTerminal = bundle.getByteArray("bNumControlTerminal");
            bNumControlUnit = bundle.getByteArray("bNumControlUnit");
            bcdUVC = bundle.getByteArray("bcdUVC");
            bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
            libUsb = bundle.getBoolean("libUsb" );
            moveToNative = bundle.getBoolean("moveToNative" );
        } else {
            stf.restoreValuesFromFile();
            mPermissionIntent = null;
            unregisterReceiver(mUsbReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
            writeTheValues();
        }
    }


    private void writeTheValues(){
        Intent resultIntent = new Intent();
        resultIntent.putExtra("camStreamingAltSetting", camStreamingAltSetting);
        resultIntent.putExtra("videoformat", videoformat);
        resultIntent.putExtra("camFormatIndex", camFormatIndex);
        resultIntent.putExtra("imageWidth", imageWidth);
        resultIntent.putExtra("imageHeight", imageHeight);
        resultIntent.putExtra("camFrameIndex", camFrameIndex);
        resultIntent.putExtra("camFrameInterval", camFrameInterval);
        resultIntent.putExtra("packetsPerRequest", packetsPerRequest);
        resultIntent.putExtra("maxPacketSize", maxPacketSize);
        resultIntent.putExtra("activeUrbs", activeUrbs);
        resultIntent.putExtra("deviceName", deviceName);
        resultIntent.putExtra("bUnitID", bUnitID);
        resultIntent.putExtra("bTerminalID", bTerminalID);
        resultIntent.putExtra("bNumControlTerminal", bNumControlTerminal);
        resultIntent.putExtra("bNumControlUnit", bNumControlUnit);
        resultIntent.putExtra("bcdUVC", bcdUVC);
        resultIntent.putExtra("bStillCaptureMethod", bStillCaptureMethod);
        resultIntent.putExtra("libUsb", libUsb);
        resultIntent.putExtra("moveToNative", moveToNative);


        setResult(Activity.RESULT_OK, resultIntent);
        if (camDeviceConnection != null) {
            if (camControlInterface != null)           camDeviceConnection.releaseInterface(camControlInterface);
            if (camStreamingInterface != null)         camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
        }
        if (libUsb) {
            if (libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming();
            }
        }
        finish();
    }

    public void beenden() {
        if (camIsOpen) {
            try {
                closeCameraDevice();
            } catch (Exception e) {
                displayErrorMessage(e);
                return;
            }
        }
        else if (camDeviceConnection != null) {
            if (moveToNative) {
                camDeviceConnection = null;
                finish();
            } else {
                if (camControlInterface!= null) camDeviceConnection.releaseInterface(camControlInterface);
                if (camStreamingInterface!= null) camDeviceConnection.releaseInterface(camStreamingInterface);
                camDeviceConnection.close();
            }
        }
        finish();
    }

    ////////// Other Methods ///////////////////

    public static String print(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append("]");
        return sb.toString();
    }

    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }

    // Methods for Automatic Setup

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (requestCode == ActivityJnaAutoDetectRequestCode && resultCode == RESULT_OK && data != null) {
            // TODO Extract the data returned from the child Activity.
            doneTransfers++;

            camStreamingAltSetting = data.getIntExtra("camStreamingAltSetting", 0);
            videoformat = data.getStringExtra("videoformat");
            camFormatIndex = data.getIntExtra("camFormatIndex", 0);
            imageWidth = data.getIntExtra("imageWidth", 0);
            imageHeight = data.getIntExtra("imageHeight", 0);
            camFrameIndex = data.getIntExtra("camFrameIndex", 0);
            camFrameInterval = data.getIntExtra("camFrameInterval", 0);
            packetsPerRequest = data.getIntExtra("packetsPerRequest", 0);
            maxPacketSize = data.getIntExtra("maxPacketSize", 0);
            activeUrbs = data.getIntExtra("activeUrbs", 0);
            deviceName = data.getStringExtra("deviceName");
            bUnitID = data.getByteExtra("bUnitID", (byte) 0);
            bTerminalID = data.getByteExtra("bTerminalID", (byte) 0);
            bNumControlTerminal = data.getByteArrayExtra("bNumControlTerminal");
            bNumControlUnit = data.getByteArrayExtra("bNumControlUnit");
            bcdUVC =  data.getByteArrayExtra("bcdUVC");
            bStillCaptureMethod = data.getByteExtra("bStillCaptureMethod", (byte) 0);
            libUsb = data.getBooleanExtra("libUsb", false);
            moveToNative = data.getBooleanExtra("moveToNative", false);



            spacketCnt = data.getIntExtra("spacketCnt", 0);
            spacket0Cnt = data.getIntExtra("spacket0Cnt", 0);
            spacket12Cnt = data.getIntExtra("spacket12Cnt", 0);
            spacketDataCnt = data.getIntExtra("spacketDataCnt", 0);
            spacketHdr8Ccnt = data.getIntExtra("spacketHdr8Ccnt", 0);
            spacketErrorCnt = data.getIntExtra("spacketErrorCnt", 0);
            sframeCnt = data.getIntExtra("sframeCnt", 0);
            sframeLen = data.getIntExtra("sframeLen", 0);
            srequestCnt = data.getIntExtra("srequestCnt", 0);
            fiveFrames = data.getBooleanExtra("fiveFrames", false);
            submiterror = data.getBooleanExtra("submiterror", false);
            Jna_AutoDetect_Handler jnaAutoDetectHandler = new Jna_AutoDetect_Handler(SetUpTheUsbDevice.this, SetUpTheUsbDevice.this);
            if(data.getBooleanExtra("stopAutoDetecton", false))   progressBar.setVisibility(View.INVISIBLE);
            switch (jnaAutoDetectHandler.compare()) {
                case -1:
                    //displayMessage("Error");
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case 0:
                    //displayMessage("Finished");
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case 1:

                    jnaAutoDetectHandler.spacketsPerRequest = packetsPerRequest;
                    jnaAutoDetectHandler.sactiveUrbs = activeUrbs;
                    log("packetPerRequest = " + packetsPerRequest);
                    startJnaAutoDetection();
                    break;
                default:
                    break;
            }
        }
        if (requestCode == ActivityLibUsbAutoDetectRequestCode && resultCode == RESULT_OK && data != null) {
            doneTransfers++;
            if(data.getBooleanExtra("stopAutoDetecton", false))   progressBar.setVisibility(View.INVISIBLE);
            boolean exit = data.getBooleanExtra("closeProgram", false);
            Jna_AutoDetect_Handler jnaAutoDetectHandler = new Jna_AutoDetect_Handler(SetUpTheUsbDevice.this, SetUpTheUsbDevice.this);
            if(data.getBooleanExtra("stopAutoDetecton", false))   progressBar.setVisibility(View.INVISIBLE);
            displayMessage("Result received");
            log ("result received");
            switch (jnaAutoDetectHandler.compare()) {
                case -1:
                    //displayMessage("Error");
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case 0:
                    //displayMessage("Finished");
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                case 1:
                    progressBar.setVisibility(View.INVISIBLE);
                    log("1 returned");
                    //startLibUsbAutoDetection();
                    break;
                default:
                    break;
            }
        }
    }

    private void startLibUsbAutoDetection() {
        saveLastValues();
        Intent intent = new Intent(getApplicationContext(), LibUsb_AutoDetect.class);
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
        bundle.putByteArray("bcdUVC", bcdUVC);
        bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
        bundle.putBoolean("libUsb", libUsb);
        bundle.putBoolean("moveToNative", moveToNative);
        bundle.putBoolean("fiveFrames", fiveFrames);
        bundle.putString("progress",progress);
        intent.putExtra("bun",bundle);
        startActivityForResult(intent, ActivityLibUsbAutoDetectRequestCode);
    }

    private void saveLastValues() {
        last_camStreamingAltSetting = camStreamingAltSetting;
        last_camFormatIndex = camFormatIndex;
        last_camFrameIndex = camFrameIndex;
        last_camFrameInterval = camFrameInterval;
        last_packetsPerRequest = packetsPerRequest;
        last_maxPacketSize = maxPacketSize;
        last_imageWidth = imageWidth;
        last_imageHeight = imageHeight;
        last_activeUrbs = activeUrbs;
        last_videoformat = videoformat;
        last_transferSucessful = transferSucessful;
    }

    private void startJnaAutoDetection() {
        Intent intent = new Intent(getApplicationContext(), Jna_AutoDetect.class);
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
        bundle.putByteArray("bcdUVC", bcdUVC);
        bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
        bundle.putBoolean("libUsb", libUsb);
        bundle.putBoolean("moveToNative", moveToNative);
        bundle.putBoolean("fiveFrames", fiveFrames);
        bundle.putString("progress",progress);
        intent.putExtra("bun",bundle);
        startActivityForResult(intent, ActivityJnaAutoDetectRequestCode);
    }

    public static boolean isEmpty(final byte[] data){
        int hits = 0;
        for (byte b : data) {
            if (b != 0) {
                hits++;
            }
        }
        return (hits == 0);
    }

}