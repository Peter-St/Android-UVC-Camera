package humer.UvcCamera.AutomaticDetection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.crowdfire.cfalertdialog.CFAlertDialog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import humer.UvcCamera.R;
import humer.UvcCamera.SaveToFile;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;
import humer.UvcCamera.UsbIso64.Libc;
import humer.UvcCamera.UsbIso64.USBIso;
import humer.UvcCamera.UsbIso64.usbdevice_fs_util;

public class Jna_AutoDetect extends AppCompatActivity {

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
    public static byte[] bcdUVC;
    public static byte bStillCaptureMethod;
    public boolean libUsb;

    private volatile boolean running = false;

    // Request types (bmRequestType):
    private static final int RT_STANDARD_INTERFACE_SET = 0x01;
    private static final int RT_CLASS_INTERFACE_SET = 0x21;
    private static final int RT_CLASS_INTERFACE_GET = 0xA1;

    // Video class-specific request codes:
    private static final int SET_CUR = 0x01;
    private static final int GET_CUR = 0x81;
    private static final int GET_MIN = 0x82;
    private static final int GET_MAX = 0x83;
    private static final int GET_RES = 0x84;

    // VideoControl interface control selectors (CS):
    private static final int VC_REQUEST_ERROR_CODE_CONTROL = 0x02;

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

    // Android USB Classes
    private UsbManager usbManager;
    private UsbDevice camDevice = null;
    private UsbDeviceConnection camDeviceConnection;
    private UsbInterface camControlInterface;
    private UsbInterface camStreamingInterface;
    private UsbEndpoint camControlEndpoint;
    private UsbEndpoint camStreamingEndpoint;
    private PendingIntent mPermissionIntent;

    // VideoStreaming interface control selectors (CS):
    private static final int VS_PROBE_CONTROL = 0x01;
    private static final int VS_COMMIT_CONTROL = 0x02;
    private static final int PU_BRIGHTNESS_CONTROL = 0x02;
    private static final int VS_STILL_PROBE_CONTROL = 0x03;
    private static final int VS_STILL_COMMIT_CONTROL = 0x04;
    private static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    private static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;

    // Video interface subclass codes:
    private static final int SC_VIDEOCONTROL = 0x01;
    private static final int SC_VIDEOSTREAMING = 0x02;

    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";

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

    private SaveToFile stf;

    // Debug Camera Variables
    private CountDownLatch latch;
    private boolean automaticStart ;
    private boolean highQualityStreamSucessful;
    private CFAlertDialog percentageBuilder;
    private CFAlertDialog percentageBuilder2;
    private int number = 0;
    private boolean thorthCTLfailed;
    private boolean l1ibusbAutoRunning;


    private volatile boolean exit = false;
    private volatile IsochronousAutomaticClass runningAutoTransfer;
    private volatile IsochronousAutomaticClass5Frames runningAutoTransfer5frames;
    private boolean fiveFrames;
    private static String progress;
    private static boolean submiterror;
    private static boolean stopAutoDetecton;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View layout = getLayoutInflater().inflate(R.layout.auto_detect_layout, null);
        setContentView(layout);
        stf = new SaveToFile(this, this);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        fetchTheValues();
        if (savedInstanceState == null) {
            Fragment fragment = AutoDetect_Fragment.newInstance(progress);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.layout_fragment_container, fragment);
            transaction.commit();
        }
        start();
    }

    @Override
    public void onBackPressed()
    {
        if (exit == true) {
            stopAutoDetecton = true;
            writeTheValues();
        }
        exit = true;
        displayMessage("Back Pressed\nPress again to exit");
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                exit = false;
            }
        };
        Handler myHandler = new Handler();
        final int TIME_TO_WAIT = 1200;
        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);

    }



    public int start() {
        running = true;
        begin();
        return 0;
    }

    private void begin() {
        sframeCnt =0;
        // Automatic UVC DetectionAutomatic UVC Detection
        UVC_Descriptor uvc_descriptor;

        try {
            findCam();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camDevice != null) listDevice(camDevice);
        try {
            openCameraDevice(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camDeviceConnection == null) return;

        byte[] data = camDeviceConnection.getRawDescriptors();
        ByteBuffer uvcData = ByteBuffer.wrap(data);
        uvc_descriptor = new UVC_Descriptor(uvcData);
        int a = uvc_descriptor.phraseUvcData();
        if (a == 0) {
            //  /*
            if (convertedMaxPacketSize == null) listDevice(camDevice);
            stf.setUpWithUvcValues(uvc_descriptor, convertedMaxPacketSize, true);
            try {
                boolean fiveFrames = false;
                try {
                    openCam(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (camIsOpen) {
                    if (!fiveFrames) {
                        if (runningAutoTransfer != null) {
                            return;
                        }
                        runningAutoTransfer = new IsochronousAutomaticClass();
                        runningAutoTransfer.start();
                    } else {
                        if (runningAutoTransfer5frames != null) {
                            return;
                        }
                        runningAutoTransfer5frames = new IsochronousAutomaticClass5Frames();
                        runningAutoTransfer5frames.start();
                    }
                }
                log("Stream JNA autoDetect complete!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
        deviceName=bundle.getString("deviceName");
        bUnitID = bundle.getByte("bUnitID",(byte)0);
        bTerminalID = bundle.getByte("bTerminalID",(byte)0);
        bNumControlTerminal = bundle.getByteArray("bNumControlTerminal");
        bNumControlUnit = bundle.getByteArray("bNumControlUnit");
        bcdUVC = bundle.getByteArray("bcdUVC");
        bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
        libUsb = bundle.getBoolean("libUsb" );
        fiveFrames = bundle.getBoolean("fiveFrames" );
        progress = bundle.getString("progress");
    }

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

        if (!usbManager.hasPermission(camDevice)) usbManager.requestPermission(camDevice, mPermissionIntent);
    }

    public void log(String msg) {
        Log.i("UVC_Camera_Set_Up", msg);
    }

    public void displayErrorMessage(Throwable e) {
        Log.e("UVC_Camera", "Error in MainActivity", e);
        displayMessage("Error: " + e);
    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
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
                    log("There is something wrong with your camera\n\nThere have not been detected enought interfaces from your usb device\n\n" + usbDevice.getInterfaceCount() + " - Interfaces have been found, but there should be at least more than 2");
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
            log(stringBuilder.toString());
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
            log(stringBuilder.toString());
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

    private void openCameraDevice(boolean init) throws Exception {
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
        }
    }

    private int videoFormatToInt () {
        if(videoformat.equals("mjpeg")) return 1;
        else if (videoformat.equals("YUY2")) return 0;
        else return 0;
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

        resultIntent.putExtra("spacketCnt", spacketCnt);
        resultIntent.putExtra("spacket0Cnt", spacket0Cnt);
        resultIntent.putExtra("spacket12Cnt", spacket12Cnt);
        resultIntent.putExtra("spacketDataCnt", spacketDataCnt);
        resultIntent.putExtra("spacketHdr8Ccnt", spacketHdr8Ccnt);
        resultIntent.putExtra("spacketErrorCnt", spacketErrorCnt);
        resultIntent.putExtra("sframeCnt", sframeCnt);
        resultIntent.putExtra("sframeLen", sframeLen);
        resultIntent.putExtra("srequestCnt", srequestCnt);
        resultIntent.putExtra("fiveFrames", fiveFrames);
        resultIntent.putExtra("submiterror", submiterror);
        resultIntent.putExtra("stopAutoDetecton", stopAutoDetecton);


        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private void openCam(boolean init) throws Exception {
        openCameraDevice(init);
        if (init) {
            initCamera();
            if (compareStreamingParmsValues()) camIsOpen = true;
            else camIsOpen = false;
        }
        log("Camera opened sucessfully");
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
            log(s.toString());
            log ("compareStreamingParmsValues returned false");
            return false;
        } else {
            initStreamingParmsResult = "Camera Controltransfer Sucessful !\n\nThe returned Values from the Camera Controltransfer fits to your entered Values\nYou can proceed starting a test run!";
            return true;
        }
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

    class IsochronousAutomaticClass extends Thread {
        private boolean reapTheLastFrames;
        private int lastReapedFrames = 0;
        public IsochronousAutomaticClass() {
            log("IsochronousAutomaticClass");
            setPriority(Thread.MAX_PRIORITY);

        }
        public void run() {
            try {
                reapTheLastFrames = false;
                USBIso usbIso64 = new USBIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize, (byte) camStreamingEndpoint.getAddress());
                usbIso64.preallocateRequests(activeUrbs);
                ArrayList<String> logArray = new ArrayList<>(512);
                int packetCnt = 0;
                int packet0Cnt = 0;
                int packet12Cnt = 0;
                int packetDataCnt = 0;
                int packetHdr8Ccnt = 0;
                int packetErrorCnt = 0;
                int frameCnt = 0;
                int frameLen = 0;
                int requestCnt = 0;
                byte[] data = new byte[maxPacketSize];
                try {
                    enableStreaming(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                usbIso64.submitUrbs();
                while (frameCnt < 1) {
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
                            break;
                        }
                        if (packetLen > 0) {
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
                                reapTheLastFrames = true;
                                frameCnt++;
                            }
                        }
                        logArray.add(logEntry.toString());
                    }
                    if (frameCnt > 0)  reapTheLastFrames = true;
                    else if (packetErrorCnt > 800) break;
                    requestCnt++;
                    if (reapTheLastFrames) {
                        if (++ lastReapedFrames == activeUrbs) break;
                    } else {
                        req.initialize();
                        int rc = req.submit();
                        if (rc != 0) {
                            submiterror = true;
                            log("ERROR SUBMIT URB, rc = " + rc);
                        }
                    }
                }
                try {
                    enableStreaming(false);
                } catch (Exception e) {
                    log("Exception during enableStreaming(false): " + e);
                }
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" + packet12Cnt + ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);
                spacketCnt = packetCnt;
                spacket0Cnt = packet0Cnt;
                spacket12Cnt = packet12Cnt;
                spacketDataCnt = packetDataCnt;
                spacketHdr8Ccnt = packetHdr8Ccnt;
                spacketErrorCnt = packetErrorCnt;
                sframeCnt = frameCnt;
                sframeLen = frameLen;
                srequestCnt = requestCnt;
                log("sframeLen = " + sframeLen);
                log("activeUrbs = " + activeUrbs);
                log("packetsPerRequest = " + packetsPerRequest);

                log("release contol Interface returned = " + usbdevice_fs_util.releaseInterface(camDeviceConnection.getFileDescriptor(), camControlInterface.getId()));
                log("release stream Interface returned = " + usbdevice_fs_util.releaseInterface(camDeviceConnection.getFileDescriptor(), camStreamingInterface.getId()));
                log("closing the FileDescriptor returned = " + Libc.INSTANCE.close(camDeviceConnection.getFileDescriptor()));
                runningAutoTransfer = null;
            } catch (IOException e) {
                e.printStackTrace();

            }
            writeTheValues();
        }
    }

    class IsochronousAutomaticClass5Frames extends Thread {
        private boolean reapTheLastFrames;
        private int lastReapedFrames = 0;
        public IsochronousAutomaticClass5Frames() {
            log("IsochronousAutomaticClass5Frames");
            setPriority(Thread.MAX_PRIORITY);
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
                while (frameCnt < 5) {
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
                                sframeLenArray[frameCnt] = frameLen;
                                frameCnt++;
                                frameLen = 0;
                                if (frameCnt > 4) reapTheLastFrames = true;;
                            }
                        }
                        logArray.add(logEntry.toString());
                    }
                    if (frameCnt > 4)  reapTheLastFrames = true;
                    else if (packetErrorCnt > 800) break;
                    requestCnt++;
                    if (reapTheLastFrames) {
                        if (++ lastReapedFrames == activeUrbs) break;
                    } else {
                        req.initialize();
                        int rc = req.submit();
                        if (rc != 0) {
                            displayMessage("ERROR SUBMIT URB");
                            log("ERROR SUBMIT URB, rc = " + rc);
                        }

                    }
                }
                try {
                    enableStreaming(false);
                } catch (Exception e) {
                    log("Exception during enableStreaming(false): " + e);
                    e.printStackTrace();
                }
                log("requests=" + requestCnt + " packetCnt=" + packetCnt + " packetErrorCnt=" + packetErrorCnt + " packet0Cnt=" + packet0Cnt + ", packet12Cnt=" + packet12Cnt + ", packetDataCnt=" + packetDataCnt + " packetHdr8cCnt=" + packetHdr8Ccnt + " frameCnt=" + frameCnt);
                spacketCnt = packetCnt;
                spacket0Cnt = packet0Cnt;
                spacket12Cnt = packet12Cnt;
                spacketDataCnt = packetDataCnt;
                spacketHdr8Ccnt = packetHdr8Ccnt;
                spacketErrorCnt = packetErrorCnt;
                sframeCnt = frameCnt;
                sframeLen = frameLen;
                srequestCnt = requestCnt;
                log("sframeLenArray[0] = " + sframeLenArray[0] + "  /  sframeLenArray[1] = " + sframeLenArray[1] + "  /  sframeLenArray[2] = " + sframeLenArray[2] + "  /  sframeLenArray[3] = " + sframeLenArray[3] + "  /  sframeLenArray[4] = " + sframeLenArray[4] );
                log("activeUrbs = " + activeUrbs);
                log("packetsPerRequest = " + packetsPerRequest);
                runningAutoTransfer5frames = null;
                writeTheValues();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void makeAnAutomaticTransfer (boolean fiveFrames) {

        closeCameraDevice();

        try {
            openCam(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (camIsOpen) {
            if (!fiveFrames) {
                if (runningAutoTransfer != null) {
                    return;
                }
                runningAutoTransfer = new IsochronousAutomaticClass();
                runningAutoTransfer.start();
            } else {
                if (runningAutoTransfer5frames != null) {
                    return;
                }
                runningAutoTransfer5frames = new IsochronousAutomaticClass5Frames();
                runningAutoTransfer5frames.start();
            }
        }

    }

    public void closeCameraDevice() {

        if (camDeviceConnection != null) {
            camDeviceConnection.close();
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection = null;
        }
    }



}

/*


public void makeAnAutomaticTransfer (boolean fiveFrames, int number, boolean libUsb) {
        int a;
        if (libUsb) {
            if (!l1ibusbAutoRunning) {
                stopLibUsbStreaming();
                closeLibUsb();
            }


            I_LibUsb.INSTANCE.setCallback(new I_LibUsb.eventCallback(){
                public boolean callback(Pointer videoFrame, int frameSize) {
                    sframeCnt ++;
                    log("Event Callback called:\nFrameLength = " + frameSize);
                    if (frameSize > 20) {
                        sframeLen = frameSize;
                        log("Event Callback called:\nFrameLength = " + frameSize);
                    }
                    if (sframeCnt > 10) latch.countDown();
                    else if (frameSize == (imageWidth * imageHeight * 2)) {
                        I_LibUsb.INSTANCE.stopStreaming();
                        I_LibUsb.INSTANCE.closeLibUsb();
                        I_LibUsb.INSTANCE.exit();
                        l1ibusbAutoRunning = false;
                        latch.countDown();
                    }
                    return true;
                }
            });


            try {
                latch = new CountDownLatch(1);

                if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                if(productID == 0) productID = camDevice.getProductId();
                if(vendorID == 0) vendorID = camDevice.getVendorId();
                if(adress == null)  adress = camDevice.getDeviceName();
                if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);

                activeUrbs = 2;
                packetsPerRequest = 32;

                runningLibUsbAutoTransfer = new LibUsbAutomaticClass();
                runningLibUsbAutoTransfer.start();

                I_LibUsb.INSTANCE.getFramesOverLibUsb(packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                        camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, videoFormatToInt(), 0);
                l1ibusbAutoRunning = true;
                latch.await();
                I_LibUsb.INSTANCE.stopStreaming();
                percentageBuilder.dismiss();



                //renewTheProgressbar();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }



        } else {
            //int a;
            if (!usbManager.hasPermission(camDevice)) {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(SetUpTheUsbDevice.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
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
            closeCameraDevice();

            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camIsOpen) {
                if (!fiveFrames) {
                    if (runningAutoTransfer != null) {
                        return;
                    }
                    runningAutoTransfer = new IsochronousAutomaticClass();
                    runningAutoTransfer.start();
                } else {
                    if (runningAutoTransfer5frames != null) {
                        return;
                    }
                    runningAutoTransfer5frames = new IsochronousAutomaticClass5Frames(number);
                    runningAutoTransfer5frames.start();
                }
            }

        }
    }






    private void performAnotherAutomaticTest() {
        try {
            latch = new CountDownLatch(1);
            makeAnAutomaticTransfer(true, 0, libUsb);
            latch.await();

            log("High Quality Stream:");
            log("sframeLenArray[0] = " + sframeLenArray[0] + "  /  sframeLenArray[1] = " + sframeLenArray[1] + "  /  sframeLenArray[2] = " + sframeLenArray[2] + "  /  sframeLenArray[3] = " + sframeLenArray[3] + "  /  sframeLenArray[4] = " + sframeLenArray[4] );
            if ((sframeLenArray[0] >= (imageWidth * imageHeight *2) & sframeLenArray[1] >= (imageWidth * imageHeight *2) & sframeLenArray[2] >= (imageWidth * imageHeight *2) & sframeLenArray[3] >= (imageWidth * imageHeight *2) & sframeLenArray[4] >= (imageWidth * imageHeight *2) )) highQualityStreamSucessful = true;
            log("highQualityStreamSucessful = " + highQualityStreamSucessful);



        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }





    private void raiseTheQuality() {
        log("Method: raiseTheQuality");
        UVC_Descriptor.FormatIndex formatIndex;
        formatIndex = stf.formatIndex;
        UVC_Descriptor.FormatIndex.FrameIndex frameIndex;
        int[] resArray = new int [formatIndex.numberOfFrameDescriptors];
        for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {
            frameIndex = formatIndex.getFrameIndex(j);
            resArray[j] = (frameIndex.wWidth * frameIndex.wHeight);
        }
        // find lowest resolution:
        int maxValue = resArray[0];
        int maxPos = 0;
        for (int i = 1; i < resArray.length; i++) {
            if (resArray[i] > maxValue) {
                maxValue = resArray[i];
                maxPos = i;
            }
        }
        frameIndex = formatIndex.getFrameIndex(maxPos);
        camFrameIndex = frameIndex.frameIndex;
        imageWidth = frameIndex.wWidth;
        imageHeight = frameIndex.wHeight;
        System.out.println("camFrameIndex = " + camFrameIndex);
        System.out.println("imageWidth = " + imageWidth);
        System.out.println("imageHeight = " + imageHeight);

        int[] intervalArray = frameIndex.dwFrameInterval.clone();
        // sorting the array to smalest Value first
        Arrays.sort(intervalArray);
        // Selecting the secound biggest Frame Interval
        if(intervalArray.length == 1) camFrameInterval = frameIndex.dwFrameInterval[(0)];
        else camFrameInterval = frameIndex.dwFrameInterval[(1)];
        lowQuality = false;
        if (libUsb) {
            try {
                latch = new CountDownLatch(1);
                I_LibUsb.INSTANCE.setCallback(new I_LibUsb.eventCallback(){
                    public boolean callback(Pointer videoFrame, int frameSize) {
                        sframeCnt ++;
                        log("Event Callback called:\nFrameLength = " + frameSize);
                        if (frameSize > 20) {
                            sframeLen = frameSize;
                            log("Event Callback called:\nFrameLength = " + frameSize);
                        }
                        if (sframeCnt > 3) latch.countDown();
                        else if (frameSize == (imageWidth * imageHeight * 2)) {
                            if (latch.getCount() == 1) {
                                latch.countDown();
                            }
                        }
                        return true;
                    }
                });
                I_LibUsb.INSTANCE.getFramesOverLibUsb(packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                        camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, videoFormatToInt(), 0);
                latch.await();
                I_LibUsb.INSTANCE.stopStreaming();
                log("Stream complete!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else performAnotherAutomaticTest();
    }







    private void renewTheProgressbar() {
        if(percentageBuilder == null) {
        } else {
            CFAlertDialog.Builder percentageB = new CFAlertDialog.Builder(SetUpTheUsbDevice.this);
            percentageB.setHeaderView(R.layout.dialog_header_layout_20);
            percentageBuilder2 = percentageB.create();
            percentageBuilder2.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    CFAlertDialog.Builder percentageB = new CFAlertDialog.Builder(SetUpTheUsbDevice.this);
                    percentageB.setHeaderView(R.layout.dialog_header_layout_20);
                    percentageBuilder = percentageB.show();
                    dialog.dismiss();
                }
            });
            percentageBuilder2.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    try {
                        if (sframeLen > 0 && sframeCnt > 0) {
                            if (sframeLen > sframeMaximalLen) sframeMaximalLen = sframeLen;
                            if (checkOneFrame()) {
                                latch = new CountDownLatch(1);
                                makeAnAutomaticTransfer(true, number, false);
                                latch.await();
                                if (checkFiveFrames()) {
                                    finalAutoMethod();
                                    return;
                                } else {
                                    activeUrbs = 4;
                                    packetsPerRequest = 4;
                                    latch = new CountDownLatch(1);
                                    makeAnAutomaticTransfer(true, ++number, false);
                                    latch.await();
                                    if (checkFiveFrames()) {
                                        finalAutoMethod();
                                        return;
                                    } else {
                                        activeUrbs = 8;
                                        packetsPerRequest = 8;
                                        latch = new CountDownLatch(1);
                                        makeAnAutomaticTransfer(true, ++number, false);
                                        latch.await();
                                        if (checkFiveFrames()) {
                                            finalAutoMethod();
                                            return;
                                        } else {
                                            activeUrbs = 16;
                                            packetsPerRequest = 16;
                                            latch = new CountDownLatch(1);
                                            makeAnAutomaticTransfer(true, ++number, false);
                                            latch.await();
                                            if (checkFiveFrames()) {
                                                finalAutoMethod();
                                                return;
                                            } else {
                                                activeUrbs = 32;
                                                packetsPerRequest = 32;
                                                latch = new CountDownLatch(1);
                                                makeAnAutomaticTransfer(true, ++number, false);
                                                latch.await();
                                                if (checkFiveFrames()) {
                                                    finalAutoMethod();
                                                    return;
                                                } else {
                                                    ///////////////////////    ????????????????????????    ///////////////////////
                                                    finalAutoMethod();
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                activeUrbs = 4;
                                packetsPerRequest = 4;
                                log("4 / 4");
                                latch = new CountDownLatch(1);
                                makeAnAutomaticTransfer(false, 0, false);
                                latch.await();
                                if (checkOneFrame()) {
                                    latch = new CountDownLatch(1);
                                    makeAnAutomaticTransfer(true, number, false);
                                    latch.await();
                                    if (checkFiveFrames()) {
                                        finalAutoMethod();
                                        return;
                                    } else {
                                        activeUrbs = 8;
                                        packetsPerRequest = 8;
                                        latch = new CountDownLatch(1);
                                        makeAnAutomaticTransfer(true, ++number, false);
                                        latch.await();
                                        if (checkFiveFrames()) {
                                            finalAutoMethod();
                                            return;
                                        } else {
                                            activeUrbs = 16;
                                            packetsPerRequest = 16;
                                            latch = new CountDownLatch(1);
                                            makeAnAutomaticTransfer(true, ++number, false);
                                            latch.await();
                                            if (checkFiveFrames()) {
                                                finalAutoMethod();
                                                return;
                                            } else {
                                                activeUrbs = 32;
                                                packetsPerRequest = 32;
                                                latch = new CountDownLatch(1);
                                                makeAnAutomaticTransfer(true, ++number, false);
                                                latch.await();
                                                if (checkFiveFrames()) {
                                                    finalAutoMethod();
                                                    return;
                                                } else {
                                                    findHighestFrameLengths();
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    activeUrbs = 8;
                                    packetsPerRequest = 8;
                                    latch = new CountDownLatch(1);
                                    makeAnAutomaticTransfer(false, 0, false);
                                    latch.await();
                                    if (checkOneFrame()) {
                                        latch = new CountDownLatch(1);
                                        makeAnAutomaticTransfer(true, number, false);
                                        latch.await();
                                        if (checkFiveFrames()) {
                                            finalAutoMethod();
                                            return;
                                        } else {
                                            activeUrbs = 16;
                                            packetsPerRequest = 16;
                                            latch = new CountDownLatch(1);
                                            makeAnAutomaticTransfer(true, ++number, false);
                                            latch.await();
                                            if (checkFiveFrames()) {
                                                finalAutoMethod();
                                                return;
                                            } else {
                                                activeUrbs = 32;
                                                packetsPerRequest = 32;
                                                latch = new CountDownLatch(1);
                                                makeAnAutomaticTransfer(true, ++number, false);
                                                latch.await();
                                                if (checkFiveFrames()) {
                                                    finalAutoMethod();
                                                    return;
                                                } else {
                                                    ///////////////////////    ????????????????????????    ///////////////////////
                                                    finalAutoMethod();
                                                    return;
                                                }
                                            }
                                        }
                                    } else {
                                        activeUrbs = 16;
                                        packetsPerRequest = 16;
                                        latch = new CountDownLatch(1);
                                        makeAnAutomaticTransfer(false, number, false);
                                        latch.await();
                                        if (checkOneFrame()) {
                                            latch = new CountDownLatch(1);
                                            makeAnAutomaticTransfer(true, ++number, false);
                                            latch.await();
                                            if (checkFiveFrames()) {
                                                finalAutoMethod();
                                                return;
                                            } else {
                                                activeUrbs = 32;
                                                packetsPerRequest = 32;
                                                latch = new CountDownLatch(1);
                                                makeAnAutomaticTransfer(true, ++number, false);
                                                latch.await();
                                                if (checkFiveFrames()) {
                                                    finalAutoMethod();
                                                    return;
                                                } else {
                                                    findHighestFrameLengths();
                                                    finalAutoMethod();
                                                    return;
                                                }
                                            }
                                        } else {
                                            findHighestFrameLengths();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            percentageBuilder2.show();
        }
    }


    //////////// Automatic Transfer Methods ////////////

    private void finalAutoMethod () {
        if (lowQuality) {
            raiseTheQuality();
        }
        if (percentageBuilder != null) {
            percentageBuilder.hide();
            percentageBuilder.dismiss();;
            percentageBuilder = null;
        }
        if (percentageBuilder2 != null) {
            percentageBuilder2.hide();
            percentageBuilder2.dismiss();
            percentageBuilder2 = null;
        }
        runOnUiThread(new Runnable() {
            String msg = "Automatic Setup Completed:";
            @Override
            public void run() {
                tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                if (camFrameInterval == 0) tv.setText(msg + "\n\nYour current Values are:\n\nPackets Per Request = " + packetsPerRequest + "\nActive Urbs = " + activeUrbs +
                        "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                        "Camera FrameIndex = " + camFrameIndex + "\nImage Width = " + imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval (fps)= " + camFrameInterval + "\nLibUsb = " + libUsb);
                else tv.setText(msg + "\n\nYour current Values are:\n\nPackets Per Request = " + packetsPerRequest + "\nActive Urbs = " + activeUrbs +
                        "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                        "Camera FrameIndex = " + camFrameIndex + "\nImage Width = " + imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval (fps) = " + (10000000 / camFrameInterval) + "\nLibUsb = " + libUsb);
                tv.setTextColor(Color.BLACK);
                testrun = findViewById(R.id.testrun);
                testrun.setEnabled(true);
                testrun.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showTestRunMenu(view);
                    }
                });
                Button button = findViewById(R.id.returnToMainScreen);
                button.setEnabled(true);
                button = findViewById(R.id.findTheCamera);
                button.setEnabled(true);
                button = findViewById(R.id.listTheCamera);
                button.setEnabled(true);
                button = findViewById(R.id.setUpWithUVC);
                button.setEnabled(true);
                button = findViewById(R.id.editSaveTheValues);
                button.setEnabled(true);
            }
        });
        automaticStart = false;
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        final String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
                        final String saveFilePathFolder = "UVC_Camera/save";
                        final File file = new File(rootPath, "/" + saveFilePathFolder);
                        if (!file.exists()) {
                            log("creating directory");
                            if (!file.mkdirs()) {
                                Log.e("TravellerLog :: ", "Problem creating Image folder");
                            }
                            file.mkdirs();
                        }
                        String rootdirStr = file.toString();
                        stf.fetchTheValues();
                        rootdirStr += "/";
                        rootdirStr += deviceName;
                        rootdirStr += ".sav";
                        stf.saveValuesToFile(rootdirStr);
                        displayMessage("Saved under: -->  " + deviceName);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Automatic Setup Finished").setMessage("Do you want to save the values to a file?").setPositiveButton("Yes, Save", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }



    private boolean checkOneFrame () {
        if (sframeLen == imageWidth * imageHeight * 2 ) return true;
        else return false;
    }

    private boolean checkFiveFrames () {
        if ((sframeLenArray[0] >= (imageWidth * imageHeight *2) & sframeLenArray[1] >= (imageWidth * imageHeight *2) & sframeLenArray[2] >= (imageWidth * imageHeight *2) & sframeLenArray[3] >= (imageWidth * imageHeight *2) & sframeLenArray[4] >= (imageWidth * imageHeight *2) )) return true;
        else return false;
    }


    private void renewTheProgressbarLibUsb() {
        if(percentageBuilder == null) {
            log("percentageBuilder dismissed");
        } else {

            CFAlertDialog.Builder percentageB = new CFAlertDialog.Builder(SetUpTheUsbDevice.this);
            percentageB.setHeaderView(R.layout.dialog_header_layout_40);
            percentageBuilder2 = percentageB.create();
            percentageBuilder2.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    dialog.dismiss();
                }
            });
            percentageBuilder2.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finalAutoMethod();
                }
            });
            percentageBuilder2.show();
        }
    }
















 */