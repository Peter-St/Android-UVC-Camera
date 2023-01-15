
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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.crowdfire.cfalertdialog.views.CFPushButton;
import com.serenegiant.usb.IFrameCallback;
import com.sun.jna.Pointer;
import com.tomer.fadingtextview.FadingTextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import humer.UvcCamera.JNA_I_LibUsb.JNA_I_LibUsb;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;
import noman.zoomtextview.ZoomTextView;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class SetUpTheUsbDeviceUvc extends Activity {

    // Native UVC Camera
    private long mNativePtr;
    private int connected_to_camera;


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

    // LIBUSB VALUE
    private static final int LIBUSB_DT_SS_ENDPOINT_COMPANION = 0x30;


    // Android USB Classes
    private UsbManager usbManager;
    private UsbDevice camDevice = null;
    private UsbDeviceConnection camDeviceConnection;
    private UsbInterface camControlInterface;
    private UsbInterface camStreamingInterface;
    private UsbEndpoint camControlEndpoint;
    private UsbEndpoint camStreamingEndpoint;
    private PendingIntent mPermissionIntent;

    // Camera Valueslib_usb_set_option
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
    public short bcdUVC_short;
    public static byte[] bcdUSB;
    public byte bStillCaptureMethod;
    public boolean libUsb;
    public static boolean moveToNative;
    public boolean transferSucessful;
    public boolean bulkMode;
    public boolean isochronous;


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
    public int[] convertedMaxPacketSize;
    public static boolean camIsOpen;
    private boolean videoProbeCommitTransferDone;

    private enum Options {searchTheCamera, testrun, listdevice, showTestRunMenu, setUpWithUvcSettings}

    //Buttons & Views
    public Button testrun;
    private ZoomTextView tv;
    public Button menu;

    //  Other Classes as Objects
    private UVC_Descriptor uvc_descriptor;
    private SaveToFile stf;


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
    public int[] sframeLenArray = new int[5];
    public int[][] shighestFramesCube = new int[10][5];
    public int srequestCnt = 0;
    public int sframeMaximalLen = 0;
    public boolean fiveFrames;
    public int doneTransfers = 0;
    public int sucessfulDoneTransfers = 0;

    public String progress;
    public boolean submiterror;

    // Debug Camera Variables
    private CountDownLatch latch;
    private boolean automaticStart;
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

    /// UVC
    private JNA_I_LibUsb.uvc_stream_ctrl.ByValue ControlValues ;

    // Log to File
    private String logString;

    // JNI METHODS
    public native int PreviewPrepareTest(long camer_pointer, final IFrameCallback callback);
    public native int PreviewStartTest(long camer_pointer);
    public native int PreviewStopTest(long camer_pointer);

    public Handler buttonHandler = null;
    public Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            Button button = findViewById(R.id.raiseSize_setUp);
            button.setEnabled(false);
            button.setAlpha(0);
            Button button2 = findViewById(R.id.lowerSize_setUp);
            button2.setEnabled(false);
            button2.setAlpha(0);
            buttonHandler = null;
        }
    };

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            log("(on receive) String action = " + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    camDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (camDevice != null) {
                            log("camDevice from BraudcastReceiver");
                        }
                    } else {
                        log("(On receive) permission denied for device ");
                        displayMessage("permission denied for device ");
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
                camDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                displayMessage("ACTION_USB_DEVICE_ATTACHED:");
                tv.setText("ACTION_USB_DEVICE_ATTACHED: \n");
                tv.setTextColor(Color.BLACK);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    displayMessage("Permissions Granted to Usb Device");
                } else {
                    log("(Device attached) permission denied for device ");
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                camDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                displayMessage("ACTION_USB_DEVICE_DETACHED: \n");
                tv.setText("ACTION_USB_DEVICE_DETACHED: \n");
                tv.setTextColor(Color.BLACK);
                if (camDeviceConnection != null) {
                    if (camControlInterface != null)
                        camDeviceConnection.releaseInterface(camControlInterface);
                    if (camStreamingInterface != null)
                        camDeviceConnection.releaseInterface(camStreamingInterface);
                    camDeviceConnection.close();
                }
            }
        }
    };

    //BroadCastReceiver to let the MainActivity know that there's message has been recevied
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                handleMessage(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    //The Values that comes from the LibUsb Manager Service
    private void handleMessage(Intent msg) throws Exception {
        Bundle data = msg.getExtras();
        log("handleMessage");
        displayMessage("handleMessage called");
        String message = msg.getExtras().getString("message"); // Contains "Hello World!"
        log("message called");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("STATE", "onStart() is called");
        /*
        // Bind to Service
        if (!mBound) {
            Intent intent = new Intent(this, LibUsbManagerService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mMessageReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
            unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            log("Exception = " + e);
        }
        log("stopping the handler thread if necessary");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View v = getLayoutInflater().inflate(R.layout.set_up_the_device_layout_main, null);
        setContentView(v);
        logString = new String();
        // receive LibUsb Status
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("REQUEST_PROCESSED"));
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
                        return;
                    }
                    button.setEnabled(true);
                    button.setAlpha(0.8f);
                    Button button2 = findViewById(R.id.lowerSize_setUp);
                    button2.setEnabled(true);
                    button2.setAlpha(0.8f);

                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);

                }
            });
        }
        Button button = findViewById(R.id.raiseSize_setUp);
        button.setEnabled(false);
        button.setAlpha(0);
        Button button2 = findViewById(R.id.lowerSize_setUp);
        button2.setEnabled(false);
        button2.setAlpha(0);
        ConstraintLayout fadingTextView = (ConstraintLayout) findViewById(R.id.fadingTextViewLayout);
        fadingTextView.setVisibility(View.INVISIBLE);
        fadingTextView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
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
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            return;
        }
        mPermissionIntent = null;
        try {
            unregisterReceiver(mUsbReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        beenden();
    }

    //////////////////////// BUTTONS Buttons ///////////////////////////////////////

    public void raiseSize(View view) {
        log("raiseSize pressed;\n");
        final int TIME_TO_WAIT = 2500;
        Button button = findViewById(R.id.raiseSize_setUp);
        if (button.isEnabled()) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
            tv.raiseSize();
            return;
        }
        button.setEnabled(true);
        button.setAlpha(0.8f);
        Button button2 = findViewById(R.id.lowerSize_setUp);
        button2.setEnabled(true);
        button2.setAlpha(0.8f);
        tv.raiseSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void lowerSize(View view) {
        log("lowerSize pressed;\n");
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
        button2.setEnabled(true);
        button2.setAlpha(0.8f);
        tv.lowerSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void showTestRunMenu(View v) {
        log("showTestRunMenu pressed;\n");
        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText("No Camera connected.");
                    tv.setTextColor(darker(Color.RED, 50));
                }
            });
            return;
        } else if (camFormatIndex == 0 || camFrameIndex == 0 || camFrameInterval == 0 || maxPacketSize == 0 || imageWidth == 0 || activeUrbs == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv.setText("The Values for the Camera are not correct set.\n\nPlease set up all the values for the camera first!");
                    tv.setTextColor(darker(Color.RED, 50));
                }
            });
            return;
        } else {
            Context wrapper = new ContextThemeWrapper(this, R.style.YOURSTYLE);
            PopupMenu popup = new PopupMenu(wrapper, v);


            if (!bulkMode) {
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
            } else {
                // This activity implements OnMenuItemClickListener
                popup.inflate(R.menu.set_up_dev_bulk_menu);
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.videoProbeCommit:
                                videoProbeCommitTransfer();
                                return true;
                            case R.id.bulk_read_1_frame:
                                try {
                                    isoRead1Frame();
                                    //testBulkRead1();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return true;

                            case R.id.bulk_read_5_sec:
                                try {
                                    isoRead5sec();
                                    //testBulkRead4();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return true;
                            default:
                                break;
                        }
                        return false;
                    }
                });
            }

            popup.show();
        }
    }

    public void searchTheCamera(View view) {
        log("searchTheCamera pressed;\n");
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
                            if (moveToNative) {
                                log("Camera has Usb permissions = ");
                                tv.setText("A camera has been found.\n\nThe Permissions to the Camera have been granted" + "\nOnly native mode supported.");
                                tv.setTextColor(darker(Color.GREEN, 100));
                            } else {
                                log("Camera has Usb permissions = ");
                                tv.setText("A camera has been found.\n\nThe Permissions to the Camera have been granted");
                                displayMessage("A camera has been found.");
                                tv.setTextColor(darker(Color.GREEN, 100));
                            }
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            log("Camera has no USB permissions ");
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
                log("Camera has USB permissions ");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("A camera was found\n\n- The camera has Usb Permissions");
                        tv.setTextColor(darker(Color.GREEN, 100));
                    }
                });
            } else {
                log("Camera has no Usb permissions, try to request... ");
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
        log("listDeviceButton pressed;\n");
        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("No Camera found.\nPlease connect first a camera and run 'Search for a camera' from the menu");
                    tv.setTextColor(darker(Color.RED, 50));
                }
            });
        } else {
            listDevice(camDevice);
            log("deviceName = " + deviceName);
        }
    }

    public void setUpWithUvcSettings(View view) {

        //if (bulkMode) libUsb = false;

        log("setUpWithUvcSettings pressed;\n");
        if (camDevice == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("No Camera found.\nPlease connect a camera, or if allready connected run 'Search for a camera' from the menu");
                    tv.setTextColor(darker(Color.RED, 50));
                }
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
                        if (stringBuilder != null) tv.setText(stringBuilder.toString());
                        else tv.setText("Camera opened.");
                        tv.setTextColor(Color.BLACK);
                    }
                });
            }
        }
    }

    public void editCameraSettings(View view) {
        log("editCameraSettings pressed;\n");
        if (buttonHandler != null) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler = null;
        }
        stf.startEditSave();
    }

    public void returnToConfigScreen(View view) {
        log("returnToConfigScreen pressed;\n");
        writeTheValues();
    }

    ///////////////////////////////////   Camera spezific methods   ////////////////////////////////////////////

    private void findCam() throws Exception {
        log("findCam ..... ;\n");
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
        while (Character.isLetter(sb.charAt(index))) {
            deviceName += sb.charAt(index);
            index++;
        }
        log("deviceName = " + deviceName);
        usbManager.requestPermission(camDevice, mPermissionIntent);
    }

    private UsbDevice checkDeviceVideoClass() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (usbDevice.getDeviceClass() == 14 && usbDevice.getDeviceSubclass() == 2) {
                moveToNative = true;
                return usbDevice;
            } else if (usbDevice.getDeviceClass() == 239 && usbDevice.getDeviceSubclass() == 2) {
                moveToNative = true;
                return usbDevice;
            } else if (usbDevice.getDeviceClass() == 0 && usbDevice.getDeviceSubclass() == 0) {
                moveToNative = true;
                return usbDevice;
            }
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
                    "usbInterface.getEndpointCount() = " + usbInterface.getEndpointCount());
            if (usbInterface.getInterfaceClass() == interfaceClass && usbInterface.getInterfaceSubclass() == interfaceSubclass && (!withEndpoint || usbInterface.getEndpointCount() > 0)) {
                return usbInterface;
            }
        }
        return null;
    }

    private void listDevice(UsbDevice usbDevice) {
        if (camDevice == null) return;
        if (camDeviceConnection == null) camDeviceConnection = usbManager.openDevice(usbDevice);
        // Get the Device Info
        // We initialize Libusb and connect to the camera
        final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info = JNA_I_LibUsb.INSTANCE.listDeviceUvc(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
        if (uvc_device_info != null) {
            connected_to_camera = 1;
            log ("DeviceInfo obtained !");
            StringBuilder streamInterfaceEntries = new StringBuilder();
            streamInterfaceEntries.append("List the Camera\n\n");
            log("uvc_device_info.stream_ifs.bInterfaceNumber = " + uvc_device_info.stream_ifs.bInterfaceNumber);
            log("uvc_device_info.stream_ifs.format_descs.bFormatIndex = " + uvc_device_info.stream_ifs.format_descs.bFormatIndex);
            log("FrameIndex = " + uvc_device_info.stream_ifs.format_descs.frame_descs.bFrameIndex);
            JNA_I_LibUsb.uvc_format_desc uvc_format_desc;
            JNA_I_LibUsb.uvc_frame_desc uvc_frame_desc;
            uvc_format_desc = uvc_device_info.stream_ifs.format_descs;
            while (uvc_format_desc != null ) {
                streamInterfaceEntries.append("\n");
                streamInterfaceEntries.append("FormatDescriptor " + uvc_format_desc.bFormatIndex + "\n");
                log("uvc_format_desc.bFormatIndex = " + uvc_format_desc.bFormatIndex);
                uvc_frame_desc = uvc_format_desc.frame_descs;
                while (uvc_frame_desc != null ) {
                    streamInterfaceEntries.append("   FrameIndex " + uvc_frame_desc.bFrameIndex + "\n");
                    streamInterfaceEntries.append("      " + uvc_frame_desc.wWidth + " x " + uvc_frame_desc.wHeight + "\n");
                    //streamInterfaceEntries.append("      Interval = " + (10000000 / uvc_frame_desc.intervals.getValue() )  + " frames/sec\n");
                    log("FrameIndex = " + uvc_frame_desc.bFrameIndex);
                    uvc_frame_desc = uvc_frame_desc.next;
                }
                uvc_format_desc = uvc_format_desc.next;
            }
            JNA_I_LibUsb.uvc_streaming_interface streaming_interface = uvc_device_info.stream_ifs.next;
            if (streaming_interface != null) log("streaming_interface.bInterfaceNumber = " + streaming_interface.bInterfaceNumber);
            final JNA_I_LibUsb.libusb_interface[] interfaceArray = (JNA_I_LibUsb.libusb_interface[])uvc_device_info.config.interFace.toArray(uvc_device_info.config.bNumInterfaces)  ;
            // Check if the Device is UVC
            isochronous = (interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].num_altsetting > 1);
            if (isochronous) {
                bulkMode = false;
                log("VS interface has multiple altsettings --> isochronous transfer supported");
                streamInterfaceEntries.append("  Isochronous transfer supported\n");
            } else {
                bulkMode = true;
                log("VS interface has only one altsetting --> isochronous transfer not supported");
                streamInterfaceEntries.append(" VS interface has only one altsettings --> isochronous transfer not supported\n");
                //return;
            }
            bcdUVC_short = uvc_device_info.ctrl_if.bcdUVC;
            List<Integer> maxPacketSizeArray = new ArrayList<Integer>();
            for (int intLoop=0; intLoop<uvc_device_info.config.bNumInterfaces; intLoop++) {
                log("Interface " + intLoop +  " has " + interfaceArray[intLoop].num_altsetting + " altsettings\n");
                streamInterfaceEntries.append("\nInterface " + intLoop +  " has " + interfaceArray[intLoop].num_altsetting + " altsettings\n");

                final JNA_I_LibUsb.libusb_interface_descriptor[] altsettingArray = (JNA_I_LibUsb.libusb_interface_descriptor[])
                        interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].altsetting.toArray(interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].num_altsetting)  ;
                //log("altsettingArray obtained");
                for (int altLoop=0; altLoop<interfaceArray[intLoop].num_altsetting; altLoop++) {
                    if(altsettingArray[altLoop].endpoint != null) {
                        log("Altsetting " + altLoop +  " has a packetSize of: " + returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize)  + " \n");
                        streamInterfaceEntries.append("   Altsetting " + altLoop +  " maxPacketSize: " + returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize) + " \n");
                        if (!isochronous) {
                            if(intLoop > 0) maxPacketSizeArray.add(returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize));
                        } else maxPacketSizeArray.add(returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize));
                        camStreamingEndpointAdress = altsettingArray[altLoop].endpoint.bEndpointAddress;
                    } else {
                        log("Altsetting has no endpoint");
                        streamInterfaceEntries.append("  Altsetting has no endpoint\n");
                    }
                }
            }
            convertedMaxPacketSize = new int[maxPacketSizeArray.size()];
            for (int intLoop=0; intLoop<maxPacketSizeArray.size(); intLoop++) {
                convertedMaxPacketSize[intLoop] = maxPacketSizeArray.get(intLoop);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText(streamInterfaceEntries.toString());
                    tv.setTextColor(Color.BLACK);
                    tv.bringToFront();
                }
            });

            saveDeviceInfoToFile(usbDevice, uvc_device_info);



        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //setContentView(R.layout.layout_main);
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText("There is something wrong with your camera\n\nThere have not been detected enought interfaces from your usb device\n\n"
                            + usbDevice.getInterfaceCount() + " - Interfaces have been found, but there should be at least more than 2");
                    tv.setTextColor(darker(Color.RED, 50));
                    tv.bringToFront();
                }
            });
            log ("DeviceInfo = null   ;-/");
        }

    }

    private void saveDeviceInfoToFile(UsbDevice usbDevice, JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info) {

        if (!(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT))  return;

        StringBuilder text = new StringBuilder();

        if (uvc_device_info != null) {
            text.append("");
            text.append("libusb_config_descriptor:\n\n");
            try {
                text.append("\nbLength = " + uvc_device_info.config.bLength);
                text.append("\nbDescriptorType = " + uvc_device_info.config.bDescriptorType);
                text.append("\nwTotalLength = " + uvc_device_info.config.wTotalLength);
                text.append("\nbNumInterfaces = " + uvc_device_info.config.bNumInterfaces);
                text.append("\nbConfigurationValue = " + uvc_device_info.config.bConfigurationValue);
                text.append("\niConfiguration = " + uvc_device_info.config.iConfiguration);
                text.append("\nbmAttributes = " + uvc_device_info.config.bmAttributes);
                text.append("\nMaxPower = " + uvc_device_info.config.MaxPower);
                final JNA_I_LibUsb.libusb_interface[] interfaceArray = (JNA_I_LibUsb.libusb_interface[])uvc_device_info.config.interFace.toArray(uvc_device_info.config.bNumInterfaces)  ;
                text.append("\ninterfaceArray [" + interfaceArray.length + "]");
                text.append("\nextra = " + uvc_device_info.config.extra);
                text.append("\nextra_length = " + uvc_device_info.config.extra_length + "\n");
            } catch (Exception e) {}
            text.append("\nuvc_control_interface_t\n\n");
            try {
                text.append("\nuvc_input_terminal = " + uvc_device_info.ctrl_if.bcdUVC);
                text.append("\nbEndpointAddress = " + uvc_device_info.ctrl_if.bEndpointAddress);
                text.append("\nbInterfaceNumber = " + uvc_device_info.ctrl_if.bcdUVC);
            } catch (Exception e) {}
            text.append("  uvc_input_terminal\n\n");
            try {
                text.append("\n  bTerminalID = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.bTerminalID));
                text.append("\n  wTerminalType = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.wTerminalType));
                text.append("\n  wObjectiveFocalLengthMin = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.wObjectiveFocalLengthMin));
                text.append("\n  wObjectiveFocalLengthMax = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.wObjectiveFocalLengthMax));
                text.append("\n  wOcularFocalLength = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.wOcularFocalLength));
                text.append("\n  bmControls = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.bmControls));
                text.append("\n  request = " + checkNull(uvc_device_info.ctrl_if.input_term_descs.request));
            } catch (Exception e) {}
            text.append("\n\n  uvc_output_terminal");
            try {
                text.append("\n  bTerminalID = " + checkNull(uvc_device_info.ctrl_if.output_term_descs.bTerminalID));
                text.append("\n\n  UVC_EXTENSION_UNIT");
                text.append("\n  guidExtensionCode = " + Arrays.toString(uvc_device_info.ctrl_if.extension_unit_descs.guidExtensionCode));
                text.append("\n  bUnitID = " + uvc_device_info.ctrl_if.extension_unit_descs.bUnitID);
                text.append("\n  bmControls = " + uvc_device_info.ctrl_if.extension_unit_descs.bmControls);
                text.append("\n  request = " + uvc_device_info.ctrl_if.extension_unit_descs.request);
            } catch (Exception e) {}
            JNA_I_LibUsb.uvc_streaming_interface streaming_interface = uvc_device_info.stream_ifs;
            text.append("\n\nuvc_stream_interface_t\n");
            try {
                text.append("bInterfaceNumber = " + uvc_device_info.stream_ifs.bInterfaceNumber);
                text.append("\nbmaControls = " + uvc_device_info.stream_ifs.bmaControls);
                text.append("\nbEndpointAddress = " + uvc_device_info.stream_ifs.bEndpointAddress);
                text.append("\nbTerminalLink = " + uvc_device_info.stream_ifs.bTerminalLink);
                text.append("\nbmInfo = " + uvc_device_info.stream_ifs.bmInfo);
                text.append("\nbStillCaptureMethod = " + uvc_device_info.stream_ifs.bStillCaptureMethod);
                text.append("\nbTriggerSupport = " + uvc_device_info.stream_ifs.bTriggerSupport);
                text.append("\nbTriggerSupport = " + uvc_device_info.stream_ifs.bTriggerSupport);
            } catch (Exception e) {}

            JNA_I_LibUsb.uvc_format_desc format =  uvc_device_info.stream_ifs.format_descs;

            do {
                if (format != null) {
                    try {
                        text.append("\n\n\nUVC_FORMAT_DESC:\n");
                        text.append("\n  bDescriptorSubtype = " + (uvc_device_info.stream_ifs.format_descs.bDescriptorSubtype) );
                        text.append("\n  bFormatIndex = " + (uvc_device_info.stream_ifs.format_descs.bFormatIndex));
                        text.append("\n  bNumFrameDescriptors = " + (uvc_device_info.stream_ifs.format_descs.bNumFrameDescriptors));
                        text.append("\n  guidFormat / fourccFormat = " + (Arrays.toString(uvc_device_info.stream_ifs.format_descs.formatSpecifier.guidFormat)));
                        text.append("\n  bDefaultFrameIndex = " + (uvc_device_info.stream_ifs.format_descs.bDefaultFrameIndex));
                    } catch (Exception e) {}

                    JNA_I_LibUsb.uvc_frame_desc frame;
                    frame = uvc_device_info.stream_ifs.format_descs.frame_descs;
                    do {
                        if (frame != null) {
                            text.append("\n\n    UVC FRAME DESCRIPTOR:");
                            try {
                                text.append("\n     bFrameIndex = " + (frame.bFrameIndex));
                                text.append("\n     wWidth = " + (frame.wWidth));
                                text.append("\n     wHeight = " + (frame.wHeight));
                                text.append("\n     dwMaxVideoFrameBufferSize = " + (frame.dwMaxVideoFrameBufferSize));
                                text.append("\n     dwDefaultFrameInterval = " + (frame.dwDefaultFrameInterval));
                                text.append("\n     intervals = " + (frame.intervals));
                            } catch (Exception e) {}
                        }
                        JNA_I_LibUsb.uvc_frame_desc frame2 = frame;
                        frame = frame2.next;
                    } while(frame != null);
                    JNA_I_LibUsb.uvc_format_desc format2 = format;
                    format = format2.next;
                }
            } while (format != null);







            text.append("\n\n\nlibusb_interface_descriptor:");

            try {
                final JNA_I_LibUsb.libusb_interface[] interfaceArray = (JNA_I_LibUsb.libusb_interface[])uvc_device_info.config.interFace.toArray(uvc_device_info.config.bNumInterfaces)  ;
                // Check if the Device is UVC
                isochronous = (interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].num_altsetting > 1);
                if (isochronous) {
                    bulkMode = false;
                    log("VS interface has multiple altsettings --> isochronous transfer supported");
                    text.append("  Isochronous transfer supported\n");
                } else {
                    bulkMode = true;
                    log("VS interface has only one altsetting --> isochronous transfer not supported");
                    text.append(" VS interface has only one altsettings --> isochronous transfer not supported\n");
                    //return;
                }
                bcdUVC_short = uvc_device_info.ctrl_if.bcdUVC;
                List<Integer> maxPacketSizeArray = new ArrayList<Integer>();
                for (int intLoop=0; intLoop<uvc_device_info.config.bNumInterfaces; intLoop++) {
                    log("Interface " + intLoop +  " has " + interfaceArray[intLoop].num_altsetting + " altsettings\n");
                    text.append("\nInterface " + intLoop +  " has " + interfaceArray[intLoop].num_altsetting + " altsettings\n");

                    final JNA_I_LibUsb.libusb_interface_descriptor[] altsettingArray = (JNA_I_LibUsb.libusb_interface_descriptor[])
                            interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].altsetting.toArray(interfaceArray[uvc_device_info.stream_ifs.bInterfaceNumber].num_altsetting)  ;
                    //log("altsettingArray obtained");
                    for (int altLoop=0; altLoop<interfaceArray[intLoop].num_altsetting; altLoop++) {
                        if(altsettingArray[altLoop].endpoint != null) {
                            log("Altsetting " + altLoop +  " has a packetSize of: " + returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize)  + " \n");
                            text.append("   Altsetting " + altLoop +  " maxPacketSize: " + returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize) + " \n");
                            if (!isochronous) {
                                if(intLoop > 0) maxPacketSizeArray.add(returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize));
                            } else maxPacketSizeArray.add(returnConvertedValue(altsettingArray[altLoop].endpoint.wMaxPacketSize));
                            camStreamingEndpointAdress = altsettingArray[altLoop].endpoint.bEndpointAddress;
                        } else {
                            log("Altsetting has no endpoint");
                            text.append("  Altsetting has no endpoint\n");
                        }
                    }
                }
            } catch (Exception e) {}

        }

        try {
            saveFile(getApplicationContext(),new String ("usb_device_info"), text.toString(), new String("txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private Object checkNull(Object obj) {
        if (obj == null) return new String ("null");
        else return obj;
    }

    public void saveFile(Context context, String fileName, String text, String extension) throws IOException{

        String DIRECTORY = "/UVC_Camera/config/";

        OutputStream outputStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues values = new ContentValues();

            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName +"."+ extension);   // file name
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents" + DIRECTORY);

            Uri extVolumeUri = MediaStore.Files.getContentUri("external");
            Uri fileUri = context.getContentResolver().insert(extVolumeUri, values);

            outputStream = context.getContentResolver().openOutputStream(fileUri);
        }
        else {
            String rootPath = null;

            rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/config/";
            log("rootPath = " + rootPath);
            File file = new File(rootPath);
            if (!file.exists()) {
                file.mkdirs();
            }


            file = new File(rootPath, fileName + "." + extension);
            log( "saveFile: file path - " + file.getAbsolutePath());
            outputStream = new FileOutputStream(file);
        }

        byte[] bytes = text.getBytes();
        outputStream.write(bytes);
        outputStream.close();
    }


    private int returnConvertedValue(int wSize) {
        String st = Integer.toBinaryString(wSize);
        StringBuilder result = new StringBuilder();
        result.append(st);
        if (result.length() < 12) return Integer.parseInt(result.toString(), 2);
        else if (result.length() == 12) {
            String a = result.substring(0, 1);
            String b = result.substring(1, 12);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c + 1) * d;
        } else {
            String a = result.substring(0, 2);
            String b = result.substring(2, 13);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c + 1) * d;
        }
    }

    public void closeCameraDevice() {

        if (moveToNative) {
            camDeviceConnection = null;
        } else if (camDeviceConnection != null) {
            if (!libUsb) {
                camDeviceConnection.releaseInterface(camControlInterface);
                camDeviceConnection.releaseInterface(camStreamingInterface);
                camDeviceConnection.close();
                camDeviceConnection = null;
            }
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
                    tv.setTextColor(darker(Color.RED, 50));
                }
            });
        } else {
            openCameraDevice(init);
            if (moveToNative) return;
            if (init) {
                //initCamera();
                if (compareStreamingParmsValues()) camIsOpen = true;
                else camIsOpen = false;
            }
            log("Camera opened sucessfully");
        }
    }

    private boolean compareStreamingParmsValues() {
        if (!Arrays.equals(initStreamingParmsIntArray, probedStreamingParmsIntArray) || !Arrays.equals(initStreamingParmsIntArray, finalStreamingParmsIntArray_first)) {
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
            log("compareStreamingParmsValues returned false");
            return false;
        } else {
            initStreamingParmsResult = "Camera Controltransfer Sucessful !\n\nThe returned Values from the Camera Controltransfer fits to your entered Values\nYou can proceed starting a test run!";
            return true;
        }
    }

    public void stopLibUsbStreaming() {
        JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));
        l1ibusbAutoRunning = false;
    }

    private void openCameraDevice(boolean init) throws Exception {
        log("open Camera Device Method ;\n");
        if (moveToNative) {
            log("moveToNative true");
            camDeviceConnection = usbManager.openDevice(camDevice);
            if (camDeviceConnection == null) {
                displayMessage("Failed to open the device - Retry");
                log("Failed to open the device - Retry");
                throw new Exception("Unable to open camera device connection.");
            } else {
                camIsOpen = true;
                log("camDeviceConnection established!");
                if (!init) moveToNativeSetUpTheValues();
            }
            return;
        }
        camControlInterface = getVideoControlInterface(camDevice);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (camControlInterface.getName() != null) deviceName = camControlInterface.getName();
        }
        camStreamingInterface = getVideoStreamingInterface(camDevice);
        log("camControlInterface = " + camControlInterface + "  //  camStreamingInterface = " + camStreamingInterface);
        if (camStreamingInterface.getEndpointCount() < 1) {
            throw new Exception("Streaming interface has no endpoint.");
        } else {
            log("setting Endpoints");
            camStreamingEndpoint = camStreamingInterface.getEndpoint(0);
        }
        if (camControlInterface.getEndpointCount() > 0) {
            camControlEndpoint = camControlInterface.getEndpoint(0);
        }
        bulkMode = camStreamingEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK;
        if (bulkMode) log("\n bulkMode detected !! \n");
        camControlInterface = getVideoControlInterface(camDevice);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (camControlInterface.getName() != null) deviceName = camControlInterface.getName();
        }
        camStreamingInterface = getVideoStreamingInterface(camDevice);
        log("camControlInterface = " + camControlInterface + "  //  camStreamingInterface = " + camStreamingInterface);
        if (camStreamingInterface.getEndpointCount() < 1) {
            throw new Exception("Streaming interface has no endpoint.");
        } else {
            log("setting Endpoints");
            camStreamingEndpoint = camStreamingInterface.getEndpoint(0);
        }
        if (camControlInterface.getEndpointCount() > 0) {
            camControlEndpoint = camControlInterface.getEndpoint(0);
        }
        bulkMode = camStreamingEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK;
        if (bulkMode) log("\n bulkMode detected !! \n");

        // (For transfer buffer sizes > 196608 the kernel file drivers/usb/core/devio.c must be patched.)

        log("opening the usb device");
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
        //if (bulkMode) return;
        if (!init) {
            if (convertedMaxPacketSize == null) listDevice(camDevice);
            if(!isochronous) {
                //displayMessage("Camera not supported");
                log("No Isochronous Camera");
                CFAlertDialog alertDialog;
                CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this);
                LayoutInflater li = LayoutInflater.from(this);
                View setup_auto_manual_view = li.inflate(R.layout.set_up_the_device_manual_automatic, null);
                builder.setHeaderView(setup_auto_manual_view);
                builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
                alertDialog = builder.show();
                CFPushButton automatic = setup_auto_manual_view.findViewById(R.id.automatic);
                automatic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //if (convertedMaxPacketSize == null) listDevice(camDevice);
                        if (!bulkMode) {
                            //displayMessage("Please select the manual Method");
                            //return;
                        }
                        log("Automatic Button Pressed");
                        automaticStart = true;
                        if (convertedMaxPacketSize == null) listDevice(camDevice);
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
                CFPushButton manual = setup_auto_manual_view.findViewById(R.id.manual);
                manual.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        log("Manual Button Pressed");
                        // Set up from UVC manually
                        if (convertedMaxPacketSize == null) listDevice(camDevice);
                        log("running stf.setUvcSettingsMethod");
                        final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info = JNA_I_LibUsb.INSTANCE.listDeviceUvc(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
                        stf.setUpWithUvcValues_libusb(uvc_device_info, convertedMaxPacketSize, false);
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        progress = "1% done";
                        if (automaticStart) {
                            // Automatic UVC Detection
                            final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info = JNA_I_LibUsb.INSTANCE.listDeviceUvc(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
                            stf.setUpWithUvcValues_libusb(uvc_device_info, convertedMaxPacketSize, true);
                            if (bcdUVC_short == 0) listDevice(camDevice);
                            bcdUVC = new byte[2];
                            bcdUVC[0] = (byte)(bcdUVC_short & 0xff);
                            bcdUVC[1] = (byte)((bcdUVC_short >> 8) & 0xff);
                            int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                            if (mUsbFs == null) mUsbFs = getUSBFSName(camDevice);
                            int framesReceive = 1;
                            if (fiveFrames) framesReceive = 5;
                            int lowAndroid = 0;
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                lowAndroid = 1;
                            }
                            JNA_I_LibUsb.INSTANCE.set_the_native_Values(new Pointer(mNativePtr), fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                    camFrameIndex, camFrameInterval, imageWidth, imageHeight, camStreamingEndpointAdress, 1, videoformat, framesReceive, bcdUVC_int, lowAndroid);
                            JNA_I_LibUsb.INSTANCE.setCallbackAuto(new JNA_I_LibUsb.eventCallbackAuto() {
                                public boolean callback(JNA_I_LibUsb.auto_detect_struct.ByReference auto_values) {
                                    activeUrbs = auto_values.activeUrbs.intValue();
                                    packetsPerRequest = auto_values.packetsPerRequest.intValue();
                                    camStreamingAltSetting = auto_values.altsetting.intValue();
                                    maxPacketSize = auto_values.maxPacketSize.intValue();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                                            tv.setText("Automatic Transfer Sucessful!");
                                            tv.setTextColor(Color.BLACK);
                                        }
                                    });
                                    return true;
                                }
                            });
                            JNA_I_LibUsb.INSTANCE.automaticDetection(new Pointer(mNativePtr));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));
                            log("Streaming stopped");
                            ProgressBar progressBar = findViewById(R.id.progressBar);
                            progressBar.setVisibility(View.INVISIBLE);

                        }
                    }
                });








            } else {

                CFAlertDialog alertDialog;
                CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this);
                LayoutInflater li = LayoutInflater.from(this);
                View setup_auto_manual_view = li.inflate(R.layout.set_up_the_device_manual_automatic, null);
                builder.setHeaderView(setup_auto_manual_view);
                builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
                alertDialog = builder.show();
                CFPushButton automatic = setup_auto_manual_view.findViewById(R.id.automatic);
                automatic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //if (convertedMaxPacketSize == null) listDevice(camDevice);
                        if (!bulkMode) {
                            //displayMessage("Please select the manual Method");
                            //return;
                        }
                        log("Automatic Button Pressed");
                        automaticStart = true;
                        if (convertedMaxPacketSize == null) listDevice(camDevice);
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
                CFPushButton manual = setup_auto_manual_view.findViewById(R.id.manual);
                manual.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        log("Manual Button Pressed");
                        // Set up from UVC manually
                        if (convertedMaxPacketSize == null) listDevice(camDevice);
                        log("running stf.setUvcSettingsMethod");
                        final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info = JNA_I_LibUsb.INSTANCE.listDeviceUvc(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
                        stf.setUpWithUvcValues_libusb(uvc_device_info, convertedMaxPacketSize, false);
                        alertDialog.dismiss();
                    }
                });
                alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        progress = "1% done";
                        if (automaticStart) {
                            // Automatic UVC Detection
                            final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info = JNA_I_LibUsb.INSTANCE.listDeviceUvc(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
                            stf.setUpWithUvcValues_libusb(uvc_device_info, convertedMaxPacketSize, true);
                            if (bcdUVC_short == 0) listDevice(camDevice);
                            bcdUVC = new byte[2];
                            bcdUVC[0] = (byte)(bcdUVC_short & 0xff);
                            bcdUVC[1] = (byte)((bcdUVC_short >> 8) & 0xff);
                            int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                            if (mUsbFs == null) mUsbFs = getUSBFSName(camDevice);
                            int framesReceive = 1;
                            if (fiveFrames) framesReceive = 5;
                            int lowAndroid = 0;
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                lowAndroid = 1;
                            }
                            JNA_I_LibUsb.INSTANCE.set_the_native_Values(new Pointer(mNativePtr), fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                    camFrameIndex, camFrameInterval, imageWidth, imageHeight, camStreamingEndpointAdress, 1, videoformat, framesReceive, bcdUVC_int, lowAndroid);
                            JNA_I_LibUsb.INSTANCE.setCallbackAuto(new JNA_I_LibUsb.eventCallbackAuto() {
                                public boolean callback(JNA_I_LibUsb.auto_detect_struct.ByReference auto_values) {
                                    activeUrbs = auto_values.activeUrbs.intValue();
                                    packetsPerRequest = auto_values.packetsPerRequest.intValue();
                                    camStreamingAltSetting = auto_values.altsetting.intValue();
                                    maxPacketSize = auto_values.maxPacketSize.intValue();
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                                            tv.setText("Automatic Transfer Sucessful!");
                                            tv.setTextColor(Color.BLACK);
                                        }
                                    });
                                    return true;
                                }
                            });
                            JNA_I_LibUsb.INSTANCE.automaticDetection(new Pointer(mNativePtr));
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));
                            log("Streaming stopped");
                            ProgressBar progressBar = findViewById(R.id.progressBar);
                            progressBar.setVisibility(View.INVISIBLE);

                        }
                    }
                });

            }
        }
    }

    private void moveToNativeSetUpTheValues() {

        byte[] a = camDeviceConnection.getRawDescriptors();
        ByteBuffer uvcData = ByteBuffer.wrap(a);
        uvc_descriptor = new UVC_Descriptor(uvcData);
        CFAlertDialog alertDialog;
        CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this);
        LayoutInflater li = LayoutInflater.from(this);
        View setup_auto_manual_view = li.inflate(R.layout.set_up_the_device_move_to_native, null);
        builder.setHeaderView(setup_auto_manual_view);
        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
        libUsb = true;
        alertDialog = builder.show();
        CFPushButton manual = setup_auto_manual_view.findViewById(R.id.native_Only_Manual);
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
            log("failed to get USBFS path, try to use default path:" + name);
            result = DEFAULT_USBFS;
        }
        return result;
    }


    public int getBus(String myString) {
        if (myString.length() > 3)
            return parseInt(myString.substring(myString.length() - 7, myString.length() - 4));
        else
            return 0;
    }

    public int getDevice(String myString) {
        if (myString.length() > 3)
            return parseInt(myString.substring(myString.length() - 3));
        else
            return 0;
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
        int[] array = new int[3];
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

    private int videoFormatToInt() {
        if (videoformat.equals("MJPEG")) return 1;
        else if (videoformat.equals("YUY2")) return 0;
        else return 0;
    }

    private void isoRead1Frame() {
        if (!camIsOpen) {
            return;
        }
        log("getOneFrameUVC - Setting Callback");
        JNA_I_LibUsb.INSTANCE.setCallback(new JNA_I_LibUsb.eventCallback() {
            public boolean callback(Pointer videoFrame, int frameSize) {
                log("frame received (JavaMsg)");
                sframeCnt++;
                log("Event Callback called:\nFrameLength = " + frameSize);
                stringBuilder = new StringBuilder();
                stringBuilder.append("Received one Frame with LibUsb:\n\n");
                stringBuilder.append("Length = " + frameSize + "\n");
                if (frameSize == (imageWidth * imageHeight * 2))
                    stringBuilder.append("\nThe Frame length matches it's expected size.\nThis are the first 20 bytes of the frame:");
                stringBuilder.append("\ndata = " + hexDump(videoFrame.getByteArray(0, 50), Math.min(32, 50)));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText(stringBuilder.toString());
                        tv.setTextColor(Color.BLACK);
                    }
                });
                JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));
                return true;
            }
        });
        log("getOneFrameUVC - native call");
        JNA_I_LibUsb.INSTANCE.getOneFrameUVC(new Pointer(mNativePtr), ControlValues);
        //log("getOneFrameUVC - stopStreaming");
        //JNA_I_LibUsb.INSTANCE.stopStreaming();
        log("getOneFrameUVC - complete (JavaMsg)");
    }

    private void isoRead5sec() {
        if (!camIsOpen) {
            displayMessage("run the control transfer first !");
            return;
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
                while ((time0 + time) > System.currentTimeMillis()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                            tv.setVisibility(View.GONE);
                            ConstraintLayout fadingTextView = (ConstraintLayout) findViewById(R.id.fadingTextViewLayout);
                            fadingTextView.setVisibility(View.VISIBLE);
                            FadingTextView FTV = (FadingTextView) findViewById(R.id.fadingTextView);
                            FTV.setTimeout(500, MILLISECONDS);
                            FTV.setVisibility(View.VISIBLE);
                            String[] texts = {"seconds counting","...."};
                            FTV.setTexts(texts);
                            FTV.forceRefresh();
                        }
                    });
                    try {
                        Thread.sleep(100);

                        /////////////////////////////////////////////////////////////////////////////////////////
                        if (!started) {
                            int result = -1;
                            result = PreviewPrepareTest(mNativePtr, new IFrameCallback() {
                                        @Override
                                        public void onFrame(final byte[] frame) {
                                            log("frame received " + sframeCnt);
                                            sframeCnt++;
                                            byte[] data = Arrays.copyOf(frame, 50);

                                            myVar.add(frame.length);
                                            logArray.add("bytes // data = " + hexDump(data, Math.min(32, data.length)));
                                            //myVar.add(frame.limit());
                                            //logArray.add("bytes // data = " + hexDump(data, Math.min(32, data.length)));
                                        }
                                    }
                            );
                            if (result == 0) {
                                result = PreviewStartTest(mNativePtr);
                                if (result == 0) {

                                    libusb_is_initialized = true;
                                    log("Test started  ... ");
                                    started = true;
                                } else {
                                    displayMessage("Test failed;  Result = " + result);
                                    log ("Test failed;  Result = " + result);
                                }

                            } else {
                                log("Test 5 sec failed;  Result = " + result);
                                displayMessage("Test 5 sec failed;  Result = " + result);
                            }


                        }
                        Thread.sleep(900);

                        /////////////////////////////////////////////////////////////////////////////////////////////


                        /*
                        if (!started) {
                            JNA_I_LibUsb.INSTANCE.setCallback(new JNA_I_LibUsb.eventCallback() {
                                public boolean callback(Pointer videoFrame, int frameSize) {
                                    log("frame received " + sframeCnt);
                                    sframeCnt++;
                                    myVar.add(frameSize);
                                    logArray.add("bytes // data = " + hexDump(videoFrame.getByteArray(0, 50), Math.min(32, 50)));
                                    return true;
                                }
                            });
                            JNA_I_LibUsb.INSTANCE.getFramesOverLibUsb5sec(new Pointer(mNativePtr), ControlValues);
                                    //mService.altSettingControl();
                            started = true;
                        }
                        */
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                PreviewStopTest(mNativePtr);


                //JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setVisibility(View.VISIBLE);
                        stringBuilder.append(String.format("Counted Frames in a Time of %d seconds:\n", (time / 1000)));
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
                if (a >= 10) break;
            }
        }
        if (libusb_is_initialized) log("\nLibusb is initialized\n");
        if (!libusb_is_initialized) {
            try {
                if (camDeviceConnection == null) {
                    log("No Camera Device Connection");
                    return;
                    /*
                    log("camDeviceConnection == null");
                    findCam();
                    log("openCameraDevice(true)");
                    openCameraDevice(true);
                    */
                }
                if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                if (productID == 0) productID = camDevice.getProductId();
                if (vendorID == 0) vendorID = camDevice.getVendorId();
                if (adress == null) adress = camDevice.getDeviceName();
                if (bcdUVC_short == 0) listDevice(camDevice);
                bcdUVC = new byte[2];
                bcdUVC[0] = (byte)(bcdUVC_short & 0xff);
                bcdUVC[1] = (byte)((bcdUVC_short >> 8) & 0xff);
                int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
                if (mUsbFs == null) mUsbFs = getUSBFSName(camDevice);
                int framesReceive = 1;
                if (fiveFrames) framesReceive = 5;
                int lowAndroid = 0;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    lowAndroid = 1;
                }
                log("JNA_I_LibUsb.INSTANCE.set_the_native_Values");
                JNA_I_LibUsb.INSTANCE.set_the_native_Values(new Pointer(mNativePtr), fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                        camFrameIndex, camFrameInterval, imageWidth, imageHeight, camStreamingEndpointAdress, 1, videoformat, framesReceive, bcdUVC_int, lowAndroid);
                libusb_is_initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int one = -1, two = -1, three = -1, four = -1;
        camIsOpen = false;
        initStreamingParmsResult = "Camera Controltransfer Failed \n\n";
        log("Starting with the Control Transfers");
        JNA_I_LibUsb.uvc_stream_ctrl.ByValue probeSet = JNA_I_LibUsb.INSTANCE.probeSetCur_TransferUVC(new Pointer(mNativePtr));
        if (probeSet.bInterfaceNumber < 0) one = probeSet.bInterfaceNumber;
        else { one = 0;
            initStreamingParms = dumpStreamingParmsStructure(probeSet);
            JNA_I_LibUsb.uvc_stream_ctrl.ByValue probeGet = JNA_I_LibUsb.INSTANCE.probeGetCur_TransferUVC(new Pointer(mNativePtr), probeSet);
            if (probeGet.bInterfaceNumber < 0) two = probeGet.bInterfaceNumber;
            else {
                two = 0;
                probedStreamingParms = dumpStreamingParmsStructure(probeGet);
                JNA_I_LibUsb.uvc_stream_ctrl.ByValue commitSet = JNA_I_LibUsb.INSTANCE.CommitSetCur_TransferUVC(new Pointer(mNativePtr), probeGet);
                if (commitSet.bInterfaceNumber < 0) three = commitSet.bInterfaceNumber;
                else {
                    three = 0;
                    camIsOpen = true;
                    ControlValues = commitSet;
                    initStreamingParmsResult = "Camera Controltransfer Sucessful !\n\nThe returned Values from the Camera Controltransfer fits to your entered Values\nYou can proceed starting a test run!";
                    finalStreamingParms_first = dumpStreamingParmsStructure(commitSet);
                    JNA_I_LibUsb.uvc_stream_ctrl.ByValue commitGet = JNA_I_LibUsb.INSTANCE.CommitGetCur_TransferUVC(new Pointer(mNativePtr), commitSet);
                    if (commitGet.bInterfaceNumber < 0) four = commitGet.bInterfaceNumber;
                    else {
                        four = 0;
                        ControlValues = commitGet;
                        finalStreamingParms = dumpStreamingParmsStructure(commitGet);
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n");
        if (one != 0 )
            sb.append("FAILED - The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "");
        else
            sb.append("The first Probe Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "");
        if (two != 0)
            sb.append("\n\nFAILED - The second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "");
        else
            sb.append("\n\nThe second Probe Controltransfer for receiving the values from the camera:\n" + probedStreamingParms + "");
        if (three != 0)
            sb.append("\n\nFAILED - The third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first);
        else
            sb.append("\n\nThe third Controltransfer for sending the final commit Values to the Camera: \n" + finalStreamingParms_first);
        if (four != 0)
            sb.append("\n\nFAILED - The Last Commit Controltransfer for receiving the final Camera Values:\n" + finalStreamingParms);
        else
            sb.append("\n\nThe Last Commit Controltransfer for receiving the final Camera Values:\n" + finalStreamingParms);
        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
        tv.setText(sb.toString());
        tv.setTextColor(Color.BLACK);
        log("Control probeCommitControl End");
    }

    private String dumpStreamingParmsStructure(JNA_I_LibUsb.uvc_stream_ctrl.ByValue value) {
        StringBuilder s = new StringBuilder(128);
        s.append("[ hint=0x" + value.bmHint);
        s.append(" / format=" + value.bFormatIndex);
        s.append(" / frame=" + value.bFrameIndex);
        s.append(" / frameInterval=" + value.dwFrameInterval);
        s.append(" / maxVideoFrameSize=" + value.dwMaxVideoFrameSize);
        s.append(" / maxPayloadTransferSize=" + value.dwMaxPayloadTransferSize);
        s.append(" ]");
        return s.toString();
    }

    private int libusb_openCam(boolean init) {
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
            /*
            mService.libusb_wrapped = true;
            mService.libusb_InterfacesClaimed = true;
            mService.altSettingControl();
            mService.ctl_to_camera_sent = true;
             */
            return JNA_I_LibUsb.INSTANCE.initStreamingParms(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
        }
        return 0;
    }

    private void libusb_openCameraDevice() {
        fd = camDeviceConnection.getFileDescriptor();
        if (moveToNative) {
            if (camStreamingEndpointAdress == 0) {
                //camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(new Pointer(mNativePtr), camDeviceConnection.getFileDescriptor());
                //mService.libusb_wrapped = true;
                //mService.libusb_InterfacesClaimed = true;
            }
        } else if (camStreamingEndpointAdress == 0)
            camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
        int framesReceive = 1;
        if (fiveFrames) framesReceive = 5;
        int bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
        int lowAndroid = 0;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            lowAndroid = 1;
        }
        int valuesSet = 0;
        if (moveToNative)
            valuesSet = JNA_I_LibUsb.INSTANCE.set_the_native_Values(new Pointer(mNativePtr), fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                    camFrameIndex, camFrameInterval, imageWidth, imageHeight, camStreamingEndpointAdress, 1, videoformat, framesReceive, bcdUVC_int, lowAndroid);
        else
            valuesSet = JNA_I_LibUsb.INSTANCE.set_the_native_Values(new Pointer(mNativePtr), fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                    camFrameIndex, camFrameInterval, imageWidth, imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat, framesReceive, bcdUVC_int, lowAndroid);
        log("set_the_native_Values returned = " + valuesSet);
        libusb_is_initialized = true;
        camDeviceIsClosed = false;
    }

    //////////////////////////////////  General Methods    //////////////////////////////////

    private static void packIntBrightness(int i, byte[] buf) {
        buf[0] = (byte) (i & 0xFF);
        buf[0 + 1] = (byte) ((i >>> 8) & 0xFF);
    }

    private static int unpackIntBrightness(byte[] buf) {
        return (((buf[1]) << 8) | (buf[0] & 0xFF));
    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SetUpTheUsbDeviceUvc.this, msg, Toast.LENGTH_LONG).show();
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

    private void fetchTheValues() {
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bun");
        if (bundle.getBoolean("edit") == true) {
            camStreamingAltSetting = bundle.getInt("camStreamingAltSetting", 0);
            videoformat = bundle.getString("videoformat");
            camFormatIndex = bundle.getInt("camFormatIndex", 0);
            imageWidth = bundle.getInt("imageWidth", 0);
            imageHeight = bundle.getInt("imageHeight", 0);
            camFrameIndex = bundle.getInt("camFrameIndex", 0);
            camFrameInterval = bundle.getInt("camFrameInterval", 0);
            packetsPerRequest = bundle.getInt("packetsPerRequest", 0);
            maxPacketSize = bundle.getInt("maxPacketSize", 0);
            activeUrbs = bundle.getInt("activeUrbs", 0);
            deviceName = bundle.getString("deviceName");
            bUnitID = bundle.getByte("bUnitID", (byte) 0);
            bTerminalID = bundle.getByte("bTerminalID", (byte) 0);
            bNumControlTerminal = bundle.getByteArray("bNumControlTerminal");
            bNumControlUnit = bundle.getByteArray("bNumControlUnit");
            bcdUVC = bundle.getByteArray("bcdUVC");
            bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte) 0);
            libUsb = bundle.getBoolean("libUsb");
            moveToNative = bundle.getBoolean("moveToNative");
            mNativePtr = bundle.getLong("mNativePtr");
            connected_to_camera = bundle.getInt("connected_to_camera", 0);
        } else {
            stf.restoreValuesFromFile();
            mPermissionIntent = null;
            unregisterReceiver(mUsbReceiver);
            unregisterReceiver(mUsbDeviceReceiver);
            writeTheValues();
        }
    }

    private void writeTheValues() {
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

        resultIntent.putExtra("mNativePtr", mNativePtr);
        resultIntent.putExtra("connected_to_camera", connected_to_camera);


        setResult(Activity.RESULT_OK, resultIntent);

        /*
        if (camDeviceConnection != null) {
            if (camControlInterface != null)
                camDeviceConnection.releaseInterface(camControlInterface);
            if (camStreamingInterface != null)
                camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
        }
        if (libUsb) {
            if (libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming(new Pointer(mNativePtr));
            }
        }
        */

        finish();
    }

    public void beenden() {
        /*
        if (camIsOpen) {
            try {
                closeCameraDevice();
            } catch (Exception e) {
                displayErrorMessage(e);
                return;
            }
        } else if (camDeviceConnection != null) {
            if (moveToNative) {
                camDeviceConnection = null;
                finish();
            } else {
                if (camControlInterface != null)
                    camDeviceConnection.releaseInterface(camControlInterface);
                if (camStreamingInterface != null)
                    camDeviceConnection.releaseInterface(camStreamingInterface);
                camDeviceConnection.close();
            }
        }

         */
        //if(mService != null) mService.streamCanBeResumed = false;
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

    public static int darker(int color, float factor) {
        int a = Color.alpha(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        return Color.argb(a,
                Math.max((int) (r * factor), 0),
                Math.max((int) (g * factor), 0),
                Math.max((int) (b * factor), 0));
    }

    // Methods for Automatic Setup

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ProgressBar progressBar = findViewById(R.id.progressBar);
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


    public static boolean isEmpty(final byte[] data) {
        int hits = 0;
        for (byte b : data) {
            if (b != 0) {
                hits++;
            }
        }
        return (hits == 0);
    }

/*
    private final IFrameCallback mIFrameCallback5Sec = new IFrameCallback() {
        @Override
        public void onFrame(final byte[] frame) {
            log("frame received " + sframeCnt);
            sframeCnt++;
            byte[] data = Arrays.copyOf(frame, 50);

            //myVar.add(frame.length);
            //logArray.add("bytes // data = " + hexDump(data, Math.min(32, data.length)));
            //myVar.add(frame.limit());
            //logArray.add("bytes // data = " + hexDump(data, Math.min(32, data.length)));
        }
    };
    */

/*
    final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {

            log("frame received " + sframeCnt);
            sframeCnt++;
            frame.clear();
            byte[] data = new byte[50];
            if (frame.capacity() >= 50) frame.get(data, 0, 50);
            else data = frame.array();
            myVar.add(frame.limit());
            logArray.add("bytes // data = " + hexDump(data, Math.min(32, data.length)));
        }
    };

   */

}
