package com.example.androidthings.videortc;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import androidx.annotation.ColorInt;

import android.text.TextUtils;
import android.util.Log;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import humer.UvcCamera.LibUsb.I_LibUsb;
import humer.UvcCamera.StartIsoStreamActivity;
import humer.UvcCamera.UsbIso64.USBIso;
import humer.UvcCamera.UsbIso64.usbdevice_fs_util;

import static java.lang.Integer.parseInt;


public class UsbCapturer implements VideoCapturer {

    private static String saveFilePathFolder = "UVC_Camera/yuvImage";
    private int value = 0;
    private CapturerObserver capturerObserver;
    private volatile UsbCapturer.IsochronousStream runningStream;
    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";

    // USB codes:
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
    private static final int VS_STILL_PROBE_CONTROL = 0x03;
    private static final int VS_STILL_COMMIT_CONTROL = 0x04;
    private static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    private static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;

    // Camera Values
    public static int camStreamingAltSetting;
    public static int camFormatIndex;
    public int camFrameIndex;
    public static int camFrameInterval;
    public static int packetsPerRequest;
    public static int maxPacketSize;
    public int imageWidth;
    public int imageHeight;
    public static int activeUrbs;
    public static String videoformat;
    public static boolean camIsOpen;
    public static byte bUnitID;
    public static byte bTerminalID;
    public static byte bStillCaptureMethod;
    public static byte[] bNumControlTerminal;
    public static byte[] bNumControlUnit;

    // Android USB Classes
    private UsbManager usbManager;
    private UsbDevice camDevice = null;
    private UsbDeviceConnection camDeviceConnection;
    private UsbInterface camControlInterface;
    private UsbInterface camStreamingInterface;
    private UsbEndpoint camStreamingEndpoint;
    private boolean bulkMode;
    private PendingIntent mPermissionIntent;
    private String controlltransfer;

    // Vales for debuging the camera
    private boolean stopKamera = false;
    private boolean pauseCamera = false;
    private boolean exit = false;
    public StringBuilder stringBuilder;
    private enum Videoformat {yuv, mjpeg, YUY2, YV12, YUV_422_888, YUV_420_888}
    private int [] differentFrameSizes;
    private int [] lastThreeFrames;
    private int whichFrame = 0;

    public static CallActivity callActivity;

    public native void JniWebRtc(int a, int b);


    // NEW LIBUSB VALUES
    public static boolean LIBUSB;
    private static int fd;
    private static int productID;
    private static int vendorID;
    private static String adress;
    private static int camStreamingEndpointAdress;
    private static String mUsbFs;
    private static int busnum;
    private static int devaddr;
    private volatile boolean libusb_is_initialized;
    private static final String DEFAULT_USBFS = "/dev/bus/usb";

    private static boolean isLoaded;
    static {
        if (!isLoaded) {
            System.loadLibrary("usb100");
            System.loadLibrary("jpeg-turbo1500");
            System.loadLibrary("uvc");
            System.loadLibrary("Iso_stream");
            isLoaded = true;
        }
    }

    public UsbCapturer(Context context, SurfaceViewRenderer svVideoRender, CallActivity callActivity) {
        this.callActivity = callActivity;
        fetchTheValues();
        initializeTheStream();
    }

    @Override
    public void initialize(SurfaceTextureHelper surfaceTextureHelper, Context context, CapturerObserver capturerObserver) {
        this.capturerObserver = capturerObserver;
    }

    @Override
    public void startCapture(int i, int i1, int i2) {

    }

    @Override
    public void stopCapture() throws InterruptedException {
        stopTheCameraStream();
    }

    @Override
    public void changeCaptureFormat(int i, int i1, int i2) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean isScreencast() {
        return false;
    }

    public void fetchTheValues(){
        camStreamingAltSetting = callActivity.camStreamingAltSetting;
        videoformat = callActivity.videoformat;
        camFormatIndex = callActivity.camFormatIndex;
        imageWidth = callActivity.imageWidth;
        imageHeight = callActivity.imageHeight;
        camFrameIndex = callActivity.camFrameIndex;
        camFrameInterval = callActivity.camFrameInterval;
        packetsPerRequest = callActivity.packetsPerRequest;
        maxPacketSize = callActivity.maxPacketSize;
        activeUrbs = callActivity.activeUrbs;
        bUnitID = callActivity.bUnitID;
        bTerminalID = callActivity.bTerminalID;
        bNumControlTerminal = callActivity.bNumControlTerminal;
        bNumControlUnit = callActivity.bNumControlUnit;
        bStillCaptureMethod = callActivity.bStillCaptureMethod;
        LIBUSB = callActivity.LIBUSB;
    }

    private void initializeTheStream() {
        usbManager = (UsbManager) callActivity.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(callActivity.getApplicationContext(), 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        findCamm();

        if (camDevice == null) {
            callActivity.usbCamera = false;
            displayMessage("No Usb Camera found\nTry to connect to internal camera ...");
            return;
        }
        else {
            if(LIBUSB) {
                try {
                    openCameraDevice(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (camDeviceConnection != null || camStreamingInterface != null) closeCameraDevice();


                if (!libusb_is_initialized) {
                    try {
                        I_LibUsb.INSTANCE.setLogPrint(new I_LibUsb.logPrint(){
                            public boolean callback(String msg) {
                                log(msg);
                                return false;
                            }

                        });
                        if (camDeviceConnection == null) {
                            findCamm();
                            openCameraDevice(true);
                        }
                        if (fd == 0) fd = camDeviceConnection.getFileDescriptor();
                        if(productID == 0) productID = camDevice.getProductId();
                        if(vendorID == 0) vendorID = camDevice.getVendorId();
                        if(adress == null)  adress = camDevice.getDeviceName();
                        if(camStreamingEndpointAdress == 0)  camStreamingEndpointAdress = camStreamingEndpoint.getAddress();
                        if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);
                        I_LibUsb.INSTANCE.init(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                                camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, camStreamingInterface.getId(), videoformat);
                        libusb_is_initialized = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                I_LibUsb.INSTANCE.probeCommitControl(1, camFormatIndex, camFrameIndex,  camFrameInterval);
                I_LibUsb.INSTANCE.probeCommitControl_cleanup();

                JniWebRtc( 1, 1);

                //JniIsoStreamActivitySurface(mPreviewSurface, 1, 1);
                //camera.setPreviewDisplay(holder.getSurface());



            } else {

                try {
                    openCam(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                initStillImageParms();
                if (camIsOpen) {

                    if (runningStream != null) {
                        return;
                    }
                    runningStream = new UsbCapturer.IsochronousStream(callActivity.getApplicationContext());
                    runningStream.start();
                } else {
                    displayMessage("Failed to start the Camera Stream");
                }
            }





        }
    }



    private void findCamm()  {
        camDevice = findCameraDevice();
        if (camDevice == null) {
            log("Camera = null");
            return;
        }
        if (!usbManager.hasPermission(camDevice)) {
            log("Asking for Permissions");
            usbManager.requestPermission(camDevice, mPermissionIntent);
        } else usbManager.requestPermission (camDevice, mPermissionIntent);

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
        }
        return null;
    }

    private void openCam(boolean init) throws Exception {
        openCameraDevice(init);
        if (init) {
            initCamera();
            camIsOpen = true;
        }
        log("Camera opened sucessfully");
    }

    private void openCameraDevice(boolean init) throws Exception {

        // (For transfer buffer sizes > 196608 the kernel file drivers/usb/core/devio.c must be patched.)
        camControlInterface = getVideoControlInterface(camDevice);
        camStreamingInterface = getVideoStreamingInterface(camDevice);
        if (camStreamingInterface.getEndpointCount() < 1) {
            throw new Exception("Streaming interface has no endpoint.");
        }
        camStreamingEndpoint = camStreamingInterface.getEndpoint(0);
        bulkMode = camStreamingEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK;
        camDeviceConnection = usbManager.openDevice(camDevice);
        if (camDeviceConnection == null) {
            log("Failed to open the device");
            throw new Exception("Unable to open camera device connection.");
        }
        if (!LIBUSB) {
            if (!camDeviceConnection.claimInterface(camControlInterface, true)) {
                log("Failed to claim camControlInterface");
                throw new Exception("Unable to claim camera control interface.");
            }
            if (!camDeviceConnection.claimInterface(camStreamingInterface, true)) {
                log("Failed to claim camStreamingInterface");
                throw new Exception("Unable to claim camera streaming interface.");
            }
        }
    }

    private void closeCameraDevice() {
        if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
            camDeviceConnection = null;
        }
        runningStream = null;
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
        //initBrightnessParms();
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
        log("Initial streaming parms: " + dumpStreamingParms(streamingParms));
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
        log("Probed streaming parms: " + dumpStreamingParms(streamingParms));
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
            //throw new Exception("Camera initialization failed. Streaming parms commit get failed.");
        }
        log("Final streaming parms: " + dumpStreamingParms(streamingParms));
        stringBuilder.append("\nFinal streaming parms: \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        controlltransfer = new String(dumpStreamingParms(streamingParms));
    }

    private String dumpStreamingParms(byte[] p) {
        StringBuilder s = new StringBuilder(128);
        s.append("hint=0x" + Integer.toHexString(unpackUsbUInt2(p, 0)));
        s.append(" format=" + (p[2] & 0xf));
        s.append(" frame=" + (p[3] & 0xf));
        s.append(" frameInterval=" + unpackUsbInt(p, 4));
        s.append(" keyFrameRate=" + unpackUsbUInt2(p, 8));
        s.append(" pFrameRate=" + unpackUsbUInt2(p, 10));
        s.append(" compQuality=" + unpackUsbUInt2(p, 12));
        s.append(" compWindowSize=" + unpackUsbUInt2(p, 14));
        s.append(" delay=" + unpackUsbUInt2(p, 16));
        s.append(" maxVideoFrameSize=" + unpackUsbInt(p, 18));
        s.append(" maxPayloadTransferSize=" + unpackUsbInt(p, 22));
        return s.toString();
    }

    private void initStillImageParms() {
        final int timeout = 5000;
        int len;
        byte[] parms = new byte[11];

        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_MIN): " + dumpStillImageParms(parms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_MAX): " + dumpStillImageParms(parms));

        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        //parms[2] = 1;

        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_CUR): " + dumpStillImageParms(parms));
        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms commit set failed.");
        }
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms commit get failed. len=" + len); }
        log("Final still image parms: " + dumpStillImageParms(parms));
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

    private int unpackUsbInt(byte[] buf, int pos) {
        return unpackInt(buf, pos, false);
    }

    private int unpackUsbUInt2(byte[] buf, int pos) {
        return ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
    }

    private void packUsbInt(int i, byte[] buf, int pos) {
        packInt(i, buf, pos, false);
    }

    private void packInt(int i, byte[] buf, int pos, boolean bigEndian) {
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

    private int unpackInt(byte[] buf, int pos, boolean bigEndian) {
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
        usbdevice_fs_util.setInterface(camDeviceConnection.getFileDescriptor(), camStreamingInterface.getId(), altSetting);
    }

    private void sendStillImageTrigger() {
        log("Sending Still Image Trigger");
        byte buf[] = new byte[1];
        buf[0] = 1;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_IMAGE_TRIGGER_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len != 1) {
            displayMessage("Still Image Controltransfer failed !!");
            log("Still Image Controltransfer failed !!");
        }
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

//------------------------------------------------------------------------------

    private String hexDump(byte[] buf, int len) {
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

    public void displayMessage(final String msg) {
        callActivity.displayMessage(msg);
    }

    public void log(String msg) {
        Log.i("UsbCapturer", msg);
    }

    public void logError(String msg) {
        Log.e("UVC_Camera", msg);
    }

    public void displayErrorMessage(Throwable e) {
        Log.e("UVC_Camera", "Error in MainActivity", e);
        displayMessage("Error: " + e);
    }

    // see 10918-1:1994, K.3.3.1 Specification of typical tables for DC difference coding
    private byte[] mjpgHuffmanTable = {
            (byte) 0xff, (byte) 0xc4, (byte) 0x01, (byte) 0xa2, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x05, (byte) 0x01, (byte) 0x01,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
            (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x10, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x02,
            (byte) 0x04, (byte) 0x03, (byte) 0x05, (byte) 0x05, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x7d,
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x11, (byte) 0x05, (byte) 0x12, (byte) 0x21, (byte) 0x31,
            (byte) 0x41, (byte) 0x06, (byte) 0x13, (byte) 0x51, (byte) 0x61, (byte) 0x07, (byte) 0x22, (byte) 0x71, (byte) 0x14, (byte) 0x32,
            (byte) 0x81, (byte) 0x91, (byte) 0xa1, (byte) 0x08, (byte) 0x23, (byte) 0x42, (byte) 0xb1, (byte) 0xc1, (byte) 0x15, (byte) 0x52,
            (byte) 0xd1, (byte) 0xf0, (byte) 0x24, (byte) 0x33, (byte) 0x62, (byte) 0x72, (byte) 0x82, (byte) 0x09, (byte) 0x0a, (byte) 0x16,
            (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a,
            (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x43, (byte) 0x44, (byte) 0x45,
            (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57,
            (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
            (byte) 0x6a, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x83,
            (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a, (byte) 0x92, (byte) 0x93, (byte) 0x94,
            (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5,
            (byte) 0xa6, (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb6,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0xc6, (byte) 0xc7,
            (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4, (byte) 0xd5, (byte) 0xd6, (byte) 0xd7, (byte) 0xd8,
            (byte) 0xd9, (byte) 0xda, (byte) 0xe1, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4, (byte) 0xe5, (byte) 0xe6, (byte) 0xe7, (byte) 0xe8,
            (byte) 0xe9, (byte) 0xea, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0xf8,
            (byte) 0xf9, (byte) 0xfa, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a,
            (byte) 0x0b, (byte) 0x11, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x03, (byte) 0x04,
            (byte) 0x07, (byte) 0x05, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x77, (byte) 0x00, (byte) 0x01,
            (byte) 0x02, (byte) 0x03, (byte) 0x11, (byte) 0x04, (byte) 0x05, (byte) 0x21, (byte) 0x31, (byte) 0x06, (byte) 0x12, (byte) 0x41,
            (byte) 0x51, (byte) 0x07, (byte) 0x61, (byte) 0x71, (byte) 0x13, (byte) 0x22, (byte) 0x32, (byte) 0x81, (byte) 0x08, (byte) 0x14,
            (byte) 0x42, (byte) 0x91, (byte) 0xa1, (byte) 0xb1, (byte) 0xc1, (byte) 0x09, (byte) 0x23, (byte) 0x33, (byte) 0x52, (byte) 0xf0,
            (byte) 0x15, (byte) 0x62, (byte) 0x72, (byte) 0xd1, (byte) 0x0a, (byte) 0x16, (byte) 0x24, (byte) 0x34, (byte) 0xe1, (byte) 0x25,
            (byte) 0xf1, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a,
            (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46,
            (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57, (byte) 0x58,
            (byte) 0x59, (byte) 0x5a, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6a,
            (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x82, (byte) 0x83,
            (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a, (byte) 0x92, (byte) 0x93, (byte) 0x94,
            (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5,
            (byte) 0xa6, (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb6,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0xc6, (byte) 0xc7,
            (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4, (byte) 0xd5, (byte) 0xd6, (byte) 0xd7, (byte) 0xd8,
            (byte) 0xd9, (byte) 0xda, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4, (byte) 0xe5, (byte) 0xe6, (byte) 0xe7, (byte) 0xe8, (byte) 0xe9,
            (byte) 0xea, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0xf8, (byte) 0xf9, (byte) 0xfa};


    public class IsochronousStream extends Thread {

        private boolean reapTheLastFrames;
        private int lastReapedFrames = 0;
        public IsochronousStream(Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
        }

        public void run() {
            try {
                log("Running Stream started");
                USBIso usbIso64 = new USBIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize, (byte) camStreamingEndpoint.getAddress());
                usbIso64.preallocateRequests(activeUrbs);
                ByteArrayOutputStream frameData = new ByteArrayOutputStream(0x20000);
                int skipFrames = 0;

                byte[] data = new byte[maxPacketSize];
                enableStreaming(true);
                usbIso64.submitUrbs();

                while (true) {
                    if (pauseCamera) {
                        Thread.sleep(200);
                    } else {
                        USBIso.Request req = usbIso64.reapRequest(true);
                        for (int packetNo = 0; packetNo < req.getNumberOfPackets(); packetNo++) {

                            int packetStatus = req.getPacketStatus(packetNo);
                            try {if (packetStatus != 0) {
                                skipFrames = 1;}
                            } catch (Exception e){
                                log("Camera read error, packet status=" + packetStatus);
                            }
                            int packetLen = req.getPacketActualLength(packetNo);

                            if (packetLen == 0) {
                                continue;
                            }

                            if (packetLen > maxPacketSize) {
                                throw new Exception("packetLen > maxPacketSize");
                            }
                            req.getPacketData(packetNo, data, packetLen);
                            int headerLen = data[0] & 0xff;

                            try { if (headerLen < 2 || headerLen > packetLen) {
                                skipFrames = 1;
                            }
                            } catch (Exception e) {
                                log("Invalid payload header length.");
                            }
                            int headerFlags = data[1] & 0xff;
                            int dataLen = packetLen - headerLen;

                            boolean error = (headerFlags & 0x40) != 0;
                            if (error && skipFrames == 0) skipFrames = 1;
                            if (dataLen > 0 && skipFrames == 0) frameData.write(data, headerLen, dataLen);


                            if ((headerFlags & 2) != 0) {

                                log("Frame Complete");
                                log("frameLen = " + frameData.size());
                                if(frameData.size() < imageWidth * imageHeight * 2) {
                                    if (videoformat.equals("mjpeg")) ;
                                    else skipFrames =1;
                                }
                                //if(frameData.size() < 20000) skipFrames =1;
                                // check Frame Size
                                if (checkFrameSize(frameData.size())) {
                                    skipFrames = 1;
                                    log("Checking Frame --> Skip Retruned");
                                }

                                if (skipFrames > 0) {
                                    log("Skipping frame, len= " + frameData.size());
                                    frameData.reset();
                                    skipFrames--;
                                }  else {
                                    if ((headerFlags & 0x20) != 0) {
                                        log("Still Image Bit set.\nSetting saveStillImage");
                                        //saveStillImage = true;

                                    }
                                    frameData.write(data, headerLen, dataLen);
                                    if (videoformat.equals("mjpeg") ) {
                                        try {
                                            log("frame, len= " + frameData.size());

                                            processReceivedMJpegVideoFrameKamera(frameData.toByteArray());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }else if (videoformat.equals("yuv")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), UsbCapturer.Videoformat.yuv);
                                    }else if (videoformat.equals("YUY2")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), UsbCapturer.Videoformat.YUY2);
                                    }else if (videoformat.equals("YUY2")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), UsbCapturer.Videoformat.YV12);
                                    }else if (videoformat.equals("YUV_420_888")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), UsbCapturer.Videoformat.YUV_420_888);
                                    }else if (videoformat.equals("YUV_422_888")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), UsbCapturer.Videoformat.YUV_422_888);
                                    }
                                    frameData.reset();
                                }
                            }
                        }
                        if (reapTheLastFrames) {
                            if (++ lastReapedFrames == activeUrbs) break;
                        } else {
                            req.initialize();
                            req.submit();
                        }
                        if (stopKamera == true) {
                            reapTheLastFrames = true;
                            break;
                        }
                    }
                }
                log("OK");
            } catch (Exception e) {
                e.printStackTrace();
            }
            runningStream = null;
        }
    }

    private boolean checkFrameSize(int size) {
        if(size < 10000) return true;
        if (differentFrameSizes == null) differentFrameSizes = new int [5];
        if (lastThreeFrames == null) lastThreeFrames = new int [4];
        lastThreeFrames[3] = lastThreeFrames[2];
        lastThreeFrames[2] = lastThreeFrames[1];
        lastThreeFrames[1] = lastThreeFrames[0];
        lastThreeFrames[0] = size;
        if (++whichFrame == 5) whichFrame = 0;
        if(differentFrameSizes[whichFrame] == 0) {
            differentFrameSizes[whichFrame] = size;
            return false;
        }
        if (differentFrameSizes[whichFrame] < size) {
            differentFrameSizes[whichFrame] = size;
            return false;
        }

        int averageSize = 0;
        for (int j = 1; j < lastThreeFrames.length;j++) {
            averageSize = averageSize + lastThreeFrames[j];
        }
        if ((averageSize / 3 - 1000) < size) {
            return false;
        }
        if ((averageSize / 3 /1.3) < size) {
            log ("averageSize = " + (averageSize / 3 /1.3));
            return false;
        }
        return true;
    }

    private void processReceivedVideoFrameYuv(byte[] frameData, UsbCapturer.Videoformat videoFromat) throws IOException {
        Long imageTime = System.currentTimeMillis();
        log("YUV Progress");
        YuvImage yuvImage ;
        if (videoFromat == UsbCapturer.Videoformat.YUY2) yuvImage = new YuvImage(frameData, ImageFormat.YUY2, imageWidth, imageHeight, null);
        else if (videoFromat == UsbCapturer.Videoformat.YV12) yuvImage = new YuvImage(frameData, ImageFormat.YV12, imageWidth, imageHeight, null);
        else if (videoFromat == UsbCapturer.Videoformat.YUV_420_888) yuvImage = new YuvImage(frameData, ImageFormat.YUV_420_888, imageWidth, imageHeight, null);
        else if (videoFromat == UsbCapturer.Videoformat.YUV_422_888) yuvImage = new YuvImage(frameData, ImageFormat.YUV_422_888, imageWidth, imageHeight, null);
        else yuvImage = new YuvImage(frameData, ImageFormat.YUY2, imageWidth, imageHeight, null);

        if (exit == false) {
            if(yuvImage.getYuvFormat() == ImageFormat.NV21) {
                capturerObserver.onByteBufferFrameCaptured(yuvImage.getYuvData(), imageWidth, imageHeight, 0, imageTime);
            } else {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, os);
                byte[] jpegByteArray = os.toByteArray();
                final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                final byte [] yuvimage;
                if (callActivity.horizontalFlip || callActivity.verticalFlip || callActivity.rotate != 0) {
                    yuvimage = getNV21(imageWidth, imageHeight, flipImage(bitmap));
                    log("before flip Capturer Observer");
                } else {
                    yuvimage = getNV21(imageWidth, imageHeight, bitmap);
                    log("before Capturer Observer");
                }
                capturerObserver.onByteBufferFrameCaptured(yuvimage, imageWidth, imageHeight, 0, imageTime);
            }
        }
    }

    public Bitmap flipImage(Bitmap src) {
        // create new matrix for transformation
        Matrix matrix = new Matrix();
        if (callActivity.horizontalFlip) matrix.preScale(1.0f, -1.0f);
        if (callActivity.verticalFlip) matrix.preScale(-1.0f, 1.0f);
        if (callActivity.rotate != 0) matrix.postRotate(callActivity.rotate);
        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    private byte [] getNV21 (int inputWidth, int inputHeight, Bitmap scaled) {

        int [] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte [] yuv = new byte[inputWidth*inputHeight*3/2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

        scaled.recycle();

        return yuv;
    }

    void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;

                // well known RGB to YUV algorithm
                Y = ( (  66 * R + 129 * G +  25 * B + 128) >> 8) +  16;
                U = ( ( -38 * R -  74 * G + 112 * B + 128) >> 8) + 128;
                V = ( ( 112 * R -  94 * G -  18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte)((V<0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte)((U<0) ? 0 : ((U > 255) ? 255 : U));
                }

                index ++;
            }
        }
    }


    private byte[] rgbValuesFromBitmap(Bitmap bitmap)
    {
        ColorMatrix colorMatrix = new ColorMatrix();
        ColorFilter colorFilter = new ColorMatrixColorFilter(
                colorMatrix);
        Bitmap argbBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(argbBitmap);

        Paint paint = new Paint();

        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int componentsPerPixel = 3;
        int totalPixels = width * height;
        int totalBytes = totalPixels * componentsPerPixel;

        byte[] rgbValues = new byte[totalBytes];
        @ColorInt int[] argbPixels = new int[totalPixels];
        argbBitmap.getPixels(argbPixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < totalPixels; i++) {
            @ColorInt int argbPixel = argbPixels[i];
            int red = Color.red(argbPixel);
            int green = Color.green(argbPixel);
            int blue = Color.blue(argbPixel);
            rgbValues[i * componentsPerPixel + 0] = (byte) red;
            rgbValues[i * componentsPerPixel + 1] = (byte) green;
            rgbValues[i * componentsPerPixel + 2] = (byte) blue;
        }

        return rgbValues;
    }

    //  RGB(ARGB8888) to YUV420 Semi-Planar(NV21) conversion
    public void rgbToYuv(byte[] rgb, int width, int height, byte[] yuv) {
        int rgbIndex = 0;
        int yIndex = 0;
        int uvIndex = width * height;
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                final int r = rgb[rgbIndex] & 0xFF;
                final int g = rgb[rgbIndex + 1] & 0xFF;
                final int b = rgb[rgbIndex + 2] & 0xFF;

                final int y = (int) (0.257 * r + 0.504 * g + 0.098 * b + 16);
                final int u = (int) (-0.148 * r - 0.291 * g + 0.439 * b + 128);
                final int v = (int) (0.439 * r - 0.368 * g - 0.071 * b + 128);

                yuv[yIndex++] = (byte) Math.max(0, Math.min(255, y));
                if ((i & 0x01) == 0 && (j & 0x01) == 0) {
                    yuv[uvIndex++] = (byte) Math.max(0, Math.min(255, v));
                    yuv[uvIndex++] = (byte) Math.max(0, Math.min(255, u));
                }

                rgbIndex += 3;
            }
        }
    }





    public void processReceivedMJpegVideoFrameKamera(byte[] mjpegFrameData) throws Exception {

        byte[] jpegFrameData = convertMjpegFrameToJpegKamera(mjpegFrameData);
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length);
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];

        if (callActivity.horizontalFlip || callActivity.verticalFlip || callActivity.rotate != 0) {
            rgbToYuv(rgbValuesFromBitmap(flipImage(bitmap)), imageWidth, imageHeight, yuv);
        } else rgbToYuv(rgbValuesFromBitmap(bitmap), imageWidth, imageHeight, yuv);

        //rgbToNV21(jpegFrameData, bitmap.getWidth(), bitmap.getHeight(), yuv);
        //encodeYUV420SP(yuv, rgbValuesFromBitmap(bitmap), imageWidth, imageHeight);

        final String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        final File file = new File(rootPath, "/" + saveFilePathFolder);
        if (!file.exists()) {
            log("creating directory");
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
            }
            file.mkdirs();
        }
        log("Path: " + rootPath.toString());
        String rootdirStr = file.toString();
        rootdirStr += "/";
        rootdirStr += value;
        rootdirStr += ".Yuv_nv21";
        if (value < 5) {
            try (FileOutputStream fos = new FileOutputStream(rootdirStr)) {
                fos.write(yuv);
                value++;
                //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
            }
        }
        if (exit == false) {
            Long imageTime = System.currentTimeMillis();
            capturerObserver.onByteBufferFrameCaptured(yuv, imageWidth, imageHeight, 0, imageTime);
        }
    }

    public void processReceivedVideoFrameYuvFromJni(byte[] frameData) {
        Videoformat videoFromat;
        if (videoformat.equals("yuv")){
            videoFromat = Videoformat.yuv;
        }else if (videoformat.equals("YUY2")){
            videoFromat = Videoformat.YUY2;
        }else if (videoformat.equals("YUY2")){
            videoFromat = Videoformat.YV12;
        }else if (videoformat.equals("YUV_420_888")){
            videoFromat = Videoformat.YUV_420_888;
        }else if (videoformat.equals("YUV_422_888")){
            videoFromat = Videoformat.YUV_422_888;
        } else videoFromat = Videoformat.mjpeg;

        try {
            processReceivedVideoFrameYuv(frameData, videoFromat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // see USB video class standard, USB_Video_Payload_MJPEG_1.5.pdf
    private byte[] convertMjpegFrameToJpegKamera(byte[] frameData) throws Exception {
        int frameLen = frameData.length;
        while (frameLen > 0 && frameData[frameLen - 1] == 0) {
            frameLen--;
        }
        if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9) {
            logError("Invalid MJPEG frame structure, length= " + frameData.length);
        }
        boolean hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
        exit = false;
        if (hasHuffmanTable) {
            if (frameData.length == frameLen) {
                return frameData;
            }
            return Arrays.copyOf(frameData, frameLen);
        } else {
            int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);

            try {if (segmentDaPos == -1)   exit = true;
            } catch (Exception e) {
                logError("Segment 0xDA not found in MJPEG frame data.");}
            //          throw new Exception("Segment 0xDA not found in MJPEG frame data.");
            if (exit ==false) {
                byte[] a = new byte[frameLen + mjpgHuffmanTable.length];
                System.arraycopy(frameData, 0, a, 0, segmentDaPos);
                System.arraycopy(mjpgHuffmanTable, 0, a, segmentDaPos, mjpgHuffmanTable.length);
                System.arraycopy(frameData, segmentDaPos, a, segmentDaPos + mjpgHuffmanTable.length, frameLen - segmentDaPos);
                return a;
            } else
                return null;


        }
    }

    private int findJpegSegment(byte[] a, int dataLen, int segmentType) {
        int p = 2;
        while (p <= dataLen - 6) {
            if ((a[p] & 0xff) != 0xff) {
                log("Unexpected JPEG data structure (marker expected).");
                break;
            }
            int markerCode = a[p + 1] & 0xff;
            if (markerCode == segmentType) {
                return p;
            }
            if (markerCode >= 0xD0 && markerCode <= 0xDA) {       // stop when scan data begins
                break;
            }
            int len = ((a[p + 2] & 0xff) << 8) + (a[p + 3] & 0xff);
            p += len + 2;
        }
        return -1;
    }

    public void stopTheCameraStream() {

        stopKamera = true;
        if (LIBUSB) {
            I_LibUsb.INSTANCE.stopStreaming();
        } else {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                enableStreaming(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
            displayMessage("Stopped ");
            log("Stopped");
            runningStream = null;
        }
        closeCameraDevice();
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

}

/*
 if (FFmpeg.getInstance(callActivity.getApplicationContext()).isSupported()) {
            //versionFFmpeg();
            String resolution = new String();
            resolution += imageWidth;
            resolution += "x";
            resolution += imageHeight;

            String[] cmd = {"-i", "jpegFrameData", "output.mov", "-s", resolution , "-pix_fmt" , "nv21", "test-yuv420p.yuv"};


            // ffmpeg -i test-640x480.jpg -s 640x480 -pix_fmt yuv420p test-yuv420p.yuv

            FFmpeg ffmpeg = FFmpeg.getInstance(callActivity.getApplicationContext());
            ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart() {Timber.d( "on start");}

                @Override
                public void onProgress(String message) {Timber.d(message);}

                @Override
                public void onFailure(String message) {Timber.d(message);}

                @Override
                public void onSuccess(String message) {Timber.d(message);}

                @Override
                public void onFinish() {Timber.d("on finish");}

            });

        } else {
            // ffmpeg is not supported
        }
        //byte[] jpegFrameData = convertMjpegFrameToJpegKamera(mjpegFrameData);

 */
/*
    private void versionFFmpeg() {
        FFmpeg.getInstance(callActivity.getApplicationContext()).execute(new String[]{"-version"}, new ExecuteBinaryResponseHandler() {
            @Override
            public void onSuccess(String message) {
                Timber.d(message);
            }

            @Override
            public void onProgress(String message) {
                Timber.d(message);
            }
        });

    }
*/

/*

    /** Convert YV12 (YYYYYYYY:UU:VV) to NV21 (YYYYYYYYY:VUVU) */  /*
public byte[] YV12toNV21(final byte[] input, byte[] output, final int width, final int height) {
    if (output == null) {
        output = new byte[input.length];
    }
    final int size = width * height;
    final int quarter = size / 4;
    final int u0 = size + quarter;

    System.arraycopy(input, 0, output, 0, size); // Y is same

    for (int v = size, u = u0, o = size; v < u0; u++, v++, o += 2) {
        output[o] = input[v]; // For NV21, V first
        output[o + 1] = input[u]; // For NV21, U second
    }
    return output;
}
 */

    /*
    if (!OpenCVLoader.initDebug())
            Log.e("OpenCv", "Unable to load OpenCV");
        else
            Log.d("OpenCv", "OpenCV loaded");
     */

     /*
        Mat rgbaMat = new Mat(imageWidth, imageHeight, CvType.CV_8UC3);
        Utils.bitmapToMat(bitmap, rgbaMat);
        // convert to NV21 ...
        Mat nv12Mat = new Mat(imageWidth, imageHeight, CvType.CV_8UC2);
        Imgproc.cvtColor( rgbaMat, nv12Mat, Imgproc.COLOR_BGR2YUV_I420 );
        byte[] nv12buffer = new byte[(int) (nv12Mat.total() * nv12Mat.channels())];
        nv12Mat.get(0, 0, nv12buffer);
        byte [] nv21Array = YV12toNV21 (nv12buffer, null , imageWidth, imageHeight);

        if (exit == false) {
            Long imageTime = System.currentTimeMillis();
            capturerObserver.onByteBufferFrameCaptured(nv21Array, imageWidth, imageHeight, 0, imageTime);
        }
        //*/
