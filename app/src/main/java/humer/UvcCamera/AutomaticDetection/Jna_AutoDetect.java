package humer.UvcCamera.AutomaticDetection;

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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import humer.UvcCamera.LibUsb.I_LibUsb;
import humer.UvcCamera.R;
import humer.UvcCamera.SaveToFile;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;

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
    public static byte bStillCaptureMethod;
    public boolean libUsb;

    private volatile boolean running = false;

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

    private static int fd;
    private static int productID;
    private static int vendorID;
    private static String adress;
    private static int camStreamingEndpointAdress;
    private static String mUsbFs;
    private static int busnum;
    private static int devaddr;
    private volatile boolean libusb_is_initialized;
    private volatile boolean exit = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View layout = getLayoutInflater().inflate(R.layout.auto_detect_layout, null);
        setContentView(layout);

        if (savedInstanceState == null) {
            Fragment fragment = AutoDetect_Fragment.newInstance();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.layout_fragment_container, fragment);
            transaction.commit();
        }


        stf = new SaveToFile(this, this);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        fetchTheValues();
        start();
    }

    @Override
    public void onBackPressed()
    {
        if (exit == true) finish();
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
            sframeLen = 0;
            number = 0;
            try {
                activeUrbs = 2;
                packetsPerRequest = 32;
                if (!libusb_is_initialized) {
                    try {
                        if (camDeviceConnection == null) {
                            findCam();
                            openCameraDevice(true);
                        }
                        if (fd == 0) fd = camDeviceConnection.getFileDescriptor();

                        if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                        I_LibUsb.INSTANCE.init(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat);
                        libusb_is_initialized = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                log("Stream complete!");
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
        bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
        libUsb = bundle.getBoolean("libUsb" );
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
        if (!libUsb) {
            if(libusb_is_initialized) {
                I_LibUsb.INSTANCE.stopStreaming();
                I_LibUsb.INSTANCE.closeLibUsb();
                I_LibUsb.INSTANCE.exit();
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
        }
    }

    private int videoFormatToInt () {
        if(videoformat.equals("mjpeg")) return 1;
        else if (videoformat.equals("YUY2")) return 0;
        else return 0;
    }
}
