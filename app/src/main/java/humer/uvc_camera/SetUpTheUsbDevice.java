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

package humer.uvc_camera;

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
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import humer.uvc_camera.UsbIso64.USBIso;
import humer.uvc_camera.UsbIso64.usbdevice_fs_util;
import noman.zoomtextview.ZoomTextView;


public class SetUpTheUsbDevice extends Activity {

    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";


    protected ImageView imageView;
    protected Button startStream;
    protected Button settingsButton;
    protected Button menu;
    protected Button stopStreamButton;
    protected ImageButton iB;

    // USB codes:
// Request types (bmRequestType):
    private static final int RT_STANDARD_INTERFACE_SET = 0x01;
    private static final int RT_CLASS_INTERFACE_SET = 0x21;
    private static final int RT_CLASS_INTERFACE_GET = 0xA1;
    // Video interface subclass codes:
    private static final int SC_VIDEOCONTROL = 0x01;
    private static final int SC_VIDEOSTREAMING = 0x02;
    private static final int SC_HUAWEI_SUBCLASS = 0x06;
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
    public static byte bStillCaptureMethod;



    // Vales for debuging the camera

    private String controlltransfer;
    private String initStreamingParmsResult;
    private String initStreamingParms;
    private int[] initStreamingParmsIntArray;
    private String probedStreamingParms;
    private int[] probedStreamingParmsIntArray;
    private String finalStreamingParms;
    private int[] finalStreamingParmsIntArray;
    public StringBuilder stringBuilder;
    int [] convertedMaxPacketSize;
    public static boolean camIsOpen;
    private boolean bulkMode;
    private enum Options { searchTheCamera, testrun, listdevice, showTestRunMenu, setUpWithUvcSettings };


    //Buttons & Views
    protected Button testrun;
    private ZoomTextView tv;

    //  Other Classes as Objects
    private UVC_Descriptor uvc_descriptor;
    private SaveToFile  stf;
    private volatile IsochronousRead runningTransfer;
    private volatile IsochronousRead1Frame runningTransfer1Frame;


    // Brightness Values
    private static int brightnessMax;
    private static int brightnessMin;

    // Huawei 360 Camera

    private boolean huawei;


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


    private final BroadcastReceiver mUsbDeviceReceiver =
            new BroadcastReceiver() {

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
    }


    @Override
    public void onBackPressed()
    {
        writeTheValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPermissionIntent = null;
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbDeviceReceiver);
        beenden();
    }




    //////////////////////// BUTTONS ///////////////////////////////////////

    public void showTestRunMenu(View v) {
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
                            isoRead();
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
                        tv.setText("No camera found\nSolutions:\n- Connect a camera and try again ...");
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
            displayMessage("OK");
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
        stf.startEditSave();

    }

    public void returnToConfigScreen(View view) {
        writeTheValues();
    }


    ///////////////////////////////////   Camera spezific methods   ////////////////////////////////////////////


    private void findCam() throws Exception {
        camDevice = findCameraDevice();
        if (camDevice == null) {
            throw new Exception("No USB camera device found.");
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


    private UsbDevice findCameraDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (checkDeviceHasVideoStreamingInterface(usbDevice)) {
                return usbDevice;
            }
        }
        return null;
    }

    private boolean checkDeviceHasVideoStreamingInterface(UsbDevice usbDevice) {
        return getVideoStreamingInterface(usbDevice) != null;
    }

    private UsbInterface getVideoControlInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOCONTROL, false);
    }

    private UsbInterface getVideoStreamingInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOSTREAMING, true);
    }

    private UsbInterface findInterface(UsbDevice usbDevice, int interfaceClass, int interfaceSubclass, boolean withEndpoint) {
        int interfaces = usbDevice.getInterfaceCount();
        for (int i = 0; i < interfaces; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == interfaceClass && usbInterface.getInterfaceSubclass() == interfaceSubclass && (!withEndpoint || usbInterface.getEndpointCount() > 0)) {
                return usbInterface;
            }
            else if (usbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC && usbInterface.getInterfaceSubclass() == SC_HUAWEI_SUBCLASS && (!withEndpoint || usbInterface.getEndpointCount() > 0)) {
                huawei = true;
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
                //builderSingle.setMessage("Select the maximal size of the Packets, which where sent to the camera device!! Important for Mediathek Devices !!");

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
                    //setContentView(R.layout.layout_main);
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setSingleLine(false);
                    tv.setText(stringBuilder.toString());
                    tv.setTextColor(Color.BLACK);
                    tv.bringToFront();
                }
            });
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

                //logArray.add(logEntry.toString());

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

    private void closeCameraDevice() throws IOException {
        if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
            camDeviceConnection = null;
        }

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
            if (init) {
                initCamera();
                if (compareStreamingParmsValues()) camIsOpen = true;
                else camIsOpen = false;
            }


            log("Camera opened sucessfully");
        }
    }

    private boolean compareStreamingParmsValues() {


        if ( !Arrays.equals( initStreamingParmsIntArray, probedStreamingParmsIntArray ) || !Arrays.equals( initStreamingParmsIntArray, finalStreamingParmsIntArray )  )  {
            StringBuilder s = new StringBuilder(128);

            if (initStreamingParmsIntArray[0] != finalStreamingParmsIntArray[0]) {
                s.append("The Controltransfer returned differnt Format Index's\n\n");
                s.append("Your entered 'Camera Format Index' Values is: " + initStreamingParmsIntArray[0] + "\n");
                s.append("The 'Camera Format Index' from the Camera Controltransfer is: " + finalStreamingParmsIntArray[0] + "\n");
            }
            if (initStreamingParmsIntArray[1] != finalStreamingParmsIntArray[1]) {
                s.append("The Controltransfer returned differnt Frame Index's\n\n");
                s.append("Your entered 'Camera Frame Index' Values is: " + initStreamingParmsIntArray[1] + "\n");
                s.append("The 'Camera Frame Index' from the Camera Controltransfer is: " + finalStreamingParmsIntArray[1] + "\n");
            }
            if (initStreamingParmsIntArray[2] != finalStreamingParmsIntArray[2]) {
                s.append("The Controltransfer returned differnt FrameIntervall Index's\n\n");
                s.append("Your entered 'Camera FrameIntervall' Values is: " + initStreamingParmsIntArray[2] + "\n");
                s.append("The 'Camera FrameIntervall' Value from the Camera Controltransfer is: " + finalStreamingParmsIntArray[2] + "\n");
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


    private void openCameraDevice(boolean init) throws Exception {
        if (!huawei) {
            // (For transfer buffer sizes > 196608 the kernel file drivers/usb/core/devio.c must be patched.)
            camControlInterface = getVideoControlInterface(camDevice);
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
            if (!init) {
                byte[] a = camDeviceConnection.getRawDescriptors();
                ByteBuffer uvcData = ByteBuffer.wrap(a);
                uvc_descriptor = new UVC_Descriptor(uvcData);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                int a = uvc_descriptor.phraseUvcData();
                                if (a == 0) {
                                    if (convertedMaxPacketSize == null) listDevice(camDevice);
                                    stf.setUpWithUvcValues(uvc_descriptor, convertedMaxPacketSize);
                                }
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:

                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Do you want to set up from UVC values ?").setPositiveButton("Yes, set up from UVC", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        } else if (huawei) {
            log("\nHuawei\n");
            camStreamingInterface = getVideoStreamingInterface(camDevice);
            camDeviceConnection = usbManager.openDevice(camDevice);
            if (camDeviceConnection == null) {
                displayMessage("Failed to open the device - Retry");
                log("Failed to open the device - Retry");
                throw new Exception("Unable to open camera device connection.");
            }
            if (!camDeviceConnection.claimInterface(camStreamingInterface, true)) {
                log("Failed to claim camStreamingInterface");
                displayMessage("Unable to claim camera streaming interface.");
                throw new Exception("Unable to claim camera streaming interface.");
            }
            if (!init) {
                byte[] a = camDeviceConnection.getRawDescriptors();
                log ("bytes = " + print(a));
                ByteBuffer uvcData = ByteBuffer.wrap(a);

            }
        }
    }



    private void initCamera() throws Exception {
        try {
            getVideoControlErrorCode();
        }                // to reset previous error states
        catch (Exception e) {
            log("Warning: getVideoControlErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        enableStreaming(false);
        try {
            getVideoStreamErrorCode();
        }                // to reset previous error states
        catch (Exception e) {
            log("Warning: getVideoStreamErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request

        initStreamingParms();
    }

    private void initStreamingParms() throws Exception {
        stringBuilder = new StringBuilder();
        final int timeout = 5000;
        int usedStreamingParmsLen;
        int len;
        byte[] streamingParms = new byte[26];
        // The e-com module produces errors with 48 bytes (UVC 1.5) instead of 26 bytes (UVC 1.1) streaming parameters! We could use the USB version info to determine the size of the streaming parameters.
        streamingParms[0] = (byte) 0x01;                // (0x01: dwFrameInterval) //D0: dwFrameInterval //D1: wKeyFrameRate // D2: wPFrameRate // D3: wCompQuality // D4: wCompWindowSize
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
            throw new Exception("Camera initialization failed. Streaming parms probe set failed, len=" + len + ".");
        }
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_PROBE_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms probe get failed.");
        }
        probedStreamingParms = dumpStreamingParms(streamingParms);
        probedStreamingParmsIntArray =  getStreamingParmsArray(streamingParms);
        log("Probed streaming parms: " + probedStreamingParms);
        stringBuilder.append("\nProbed streaming parms:  \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        usedStreamingParmsLen = len;
        // log("Streaming parms length: " + usedStreamingParmsLen);
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, usedStreamingParmsLen, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms commit set failed.");
        }
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, usedStreamingParmsLen, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms commit get failed.");
        }
        finalStreamingParms = dumpStreamingParms(streamingParms);
        finalStreamingParmsIntArray = getStreamingParmsArray(streamingParms);
        log("Final streaming parms: " + finalStreamingParms);
        stringBuilder.append("\nFinal streaming parms: \n");
        stringBuilder.append(finalStreamingParms);
        controlltransfer = new String(finalStreamingParms);
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

    private void initStillImageParms() throws Exception {
        final int timeout = 5000;
        int len;
        byte[] parms = new byte[11];
        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        parms[2] = 1;
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            throw new Exception("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms: " + dumpStillImageParms(parms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            throw new Exception("Camera initialization failed. Still image parms commit set failed.");
        }

    }

    private String dumpStillImageParms(byte[] p) {
        StringBuilder s = new StringBuilder(128);
        s.append("bFormatIndex=" + (p[0] & 0xff));
        s.append(" bFrameIndex=" + (p[1] & 0xff));
        s.append(" bCompressionIndex=" + (p[2] & 0xff));
        s.append(" maxVideoFrameSize=" + unpackUsbInt(p, 3));
        s.append(" maxPayloadTransferSize=" + unpackUsbInt(p, 7));
        return s.toString();
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

    private void enableStreaming_direct(boolean enabled) throws Exception {
        if (!enabled) {
            return;
        }
        // Ist unklar, wie man das Streaming disabled. AltSetting muss 0 sein damit die Video-Daten kommen.
        int len = camDeviceConnection.controlTransfer(RT_STANDARD_INTERFACE_SET, SET_INTERFACE, 0, camStreamingInterface.getId(), null, 0, 1000);
        if (len != 0) {
            throw new Exception("SET_INTERFACE (direct) failed, len=" + len + ".");
        }
    }

    private void sendStillImageTrigger() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 1;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_IMAGE_TRIGGER_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len != 1) {
            throw new Exception("VS_STILL_IMAGE_TRIGGER_CONTROL failed, len=" + len + ".");
        }
    }

    // Resets the error code after retrieving it.
// Does not work with the e-con camera module!
    private int getVideoControlErrorCode() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 99;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VC_REQUEST_ERROR_CODE_CONTROL << 8, 0, buf, 1, 1000);
        if (len != 1) {
            throw new Exception("VC_REQUEST_ERROR_CODE_CONTROL failed, len=" + len + ".");
        }
        return buf[0];
    }

    // Does not work with Logitech C310? Always returns 0.
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
                //Thread.sleep(500);
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
                stringBuilder.append(String.format("\n\nCountedFrames in a Time of %d seconds:\n", (time/1000)));

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
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" + packet12Cnt + ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);
                //for (String s : logArray) {
                //    log(s);
                //}

                if (packetErrorCnt > 800) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Your Camera only return Error frames!\nPlease change your camera values\n");
                    stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" + "\npacketCnt= " + packetCnt + " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +  "\npacket0Cnt= " + packet0Cnt + " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= " + packetDataCnt + " (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");

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
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" + packet12Cnt + ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);


                if (packetErrorCnt > 800) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Your Camera only return Error frames!\nPlease change your camera values\n");
                    stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" + "\npacketCnt= " + packetCnt + " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +  "\npacket0Cnt= " + packet0Cnt + " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= " + packetDataCnt + " (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");

                }

                stringBuilder.append("\n\nrequests= " + requestCnt +  "  ( one Request has a max. size of: "+ packetsPerRequest + " x " + maxPacketSize+ " bytes )" + "\npacketCnt= " + packetCnt + " (number of packets from this frame)" + "\npacketErrorCnt= " + packetErrorCnt + " (This packets are Error packets)" +  "\npacket0Cnt= " + packet0Cnt + " (Packets with a size of 0 bytes)" + "\npacket12Cnt= " + packet12Cnt+ " (Packets with a size of 12 bytes)" + "\npacketDataCnt= " + packetDataCnt + " (This packets contain valid data)" + "\npacketHdr8cCnt= " + packetHdr8Ccnt + "\nframeCnt= " + frameCnt + " (The number of the counted frames)" + "\n\n");


                stringBuilder.append("Explaination: The first number is the Requestnumber and the second number is the data packet from this request.\nThe comes the data length of this packet with: 'len='" +
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


    private void isoRead() {

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
        try {
            closeCameraDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        try {
            closeCameraDevice();
        } catch (IOException e) {
            e.printStackTrace();
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


    private void videoProbeCommitTransfer() {

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
        try {
            closeCameraDevice();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    tv.setText(initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" +
                            "The first Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "" +
                            "\n\nThe second Controltransfer for probing the values with the camera:\n" + probedStreamingParms + "" +
                            "\n\nThe Last Controltransfer for receiving the final Camera Values from the Camera: \n" + finalStreamingParms);
                    tv.setTextColor(Color.BLACK);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("Failed to initialise the camera\n\n" + initStreamingParmsResult + "\n\nThe Control Transfers to the Camera has following Results:\n\n" +
                            "The first Controltransfer for sending the Values to the Camera: \n" + initStreamingParms + "" +
                            "\n\nThe second Controltransfer for probing the values with the camera:\n" + probedStreamingParms + "" +
                            "\n\nThe Last Controltransfer for receiving the final Camera Values from the Camera: \n" + finalStreamingParms);
                    tv.setTextColor(darker(Color.RED, 50));
                }
            });
        }
    }

    private void brightnessControlTransfer() throws Exception{

        if (camIsOpen) {
            StringBuilder stringBuilder = new StringBuilder();
            final int timeout = 5000;
            int len;
            byte[] brightnessParms = new byte[2];
            // PU_BRIGHTNESS_CONTROL(0x02), GET_MIN(0x82) [UVC1.5, p. 160, 158, 96]
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            if (len != brightnessParms.length) {
                displayMessage("Error: Durning PU_BRIGHTNESS_CONTROL");
                throw new Exception("Camera PU_BRIGHTNESS_CONTROL GET_MIN failed. len= " + len + ".");
            }
            log( "brightness min: " + unpackIntBrightness(brightnessParms));
            brightnessMin = unpackIntBrightness(brightnessParms);
            stringBuilder.append("\nbrightnessMin= " + brightnessMin);
            // PU_BRIGHTNESS_CONTROL(0x02), GET_MAX(0x83) [UVC1.5, p. 160, 158, 96]
            camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            log( "brightness max: " + unpackIntBrightness(brightnessParms));
            brightnessMax = unpackIntBrightness(brightnessParms);
            stringBuilder.append("\nbrightnessMax= " + brightnessMax);
            // PU_BRIGHTNESS_CONTROL(0x02), GET_RES(0x84) [UVC1.5, p. 160, 158, 96]
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_RES, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            log( "brightness res: " + unpackIntBrightness(brightnessParms));
            // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            log( "brightness cur: " + unpackIntBrightness(brightnessParms));
            stringBuilder.append("\ncurrent Brightness= " + unpackIntBrightness(brightnessParms));


            // change brightness
            int brightness = unpackIntBrightness(brightnessParms);
            brightness -= 30;
            stringBuilder.append("\nchange the current brightness: " + -30);
            packIntBrightness(brightness, brightnessParms);

            // PU_BRIGHTNESS_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
            camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            brightness = (brightnessParms[1]  <<8 ) | ( brightnessParms[0] & 0xff);
            log( "brightness set: " + brightness);

            // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
            camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
            log( "brightness get: " + unpackIntBrightness(brightnessParms));
            stringBuilder.append("\ncurrent changed Brightness= " + unpackIntBrightness(brightnessParms));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("Brightness Control Sucessful\n\n" + stringBuilder.toString());
                    tv.setTextColor(Color.BLACK);
                }
            });

        }else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                    tv.setText("Please Run The Video Probe Commit Control Transfer first!");
                    tv.setTextColor(Color.BLACK);
                }
            });
        }
    }


    private void focusControlTransfer(){


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
        Log.i("UVC_Camera", msg);
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
            bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
        } else {
            //stf = new SaveToFile(this, this);
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
        resultIntent.putExtra("bStillCaptureMethod", bStillCaptureMethod);


        setResult(Activity.RESULT_OK, resultIntent);
        if (camDeviceConnection != null) {
            if (camControlInterface != null)           camDeviceConnection.releaseInterface(camControlInterface);
            if (camStreamingInterface != null)         camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
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
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
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
}
