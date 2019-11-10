package humer.uvc_camera;

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

public class SetCameraVariables {


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
    private static final int GET_LEN = 0x85;
    private static final int GET_INFO = 0x86;
    private static final int GET_DEF = 0x87;


    // VideoControl interface control selectors (CS):
    private static final int VC_REQUEST_ERROR_CODE_CONTROL = 0x02;


    // VideoStreaming interface control selectors (CS):
    private static final int VS_PROBE_CONTROL = 0x01;
    private static final int VS_COMMIT_CONTROL = 0x02;
    private static final int PU_BRIGHTNESS_CONTROL = 0x02;


    // Camera Terminal Control Selectors

    private static final int   CT_CONTROL_UNDEFINED = 0x00;
    private static final int   CT_SCANNING_MODE_CONTROL = 0x01;
    private static final int   CT_AE_MODE_CONTROL = 0x02;
    private static final int   CT_AE_PRIORITY_CONTROL = 0x03;
    private static final int   CT_EXPOSURE_TIME_ABSOLUTE_CONTROL = 0x04;
    private static final int   CT_EXPOSURE_TIME_RELATIVE_CONTROL = 0x05;
    private static final int   CT_FOCUS_ABSOLUTE_CONTROL = 0x06;
    private static final int   CT_FOCUS_RELATIVE_CONTROL = 0x07;
    private static final int   CT_FOCUS_AUTO_CONTROL = 0x08;
    private static final int   CT_IRIS_ABSOLUTE_CONTROL = 0x09;
    private static final int   CT_IRIS_RELATIVE_CONTROL = 0x0a;
    private static final int   CT_ZOOM_ABSOLUTE_CONTROL = 0x0b;
    private static final int   CT_ZOOM_RELATIVE_CONTROL = 0x0c;
    private static final int   CT_PANTILT_ABSOLUTE_CONTROL = 0x0d;
    private static final int   CT_PANTILT_RELATIVE_CONTROL = 0x0e;
    private static final int   CT_ROLL_ABSOLUTE_CONTROL = 0x0f;
    private static final int   CT_ROLL_RELATIVE_CONTROL = 0x10;
    private static final int   CT_PRIVACY_CONTROL = 0x11;
    private static final int   CT_FOCUS_SIMPLE_CONTROL = 0x12;
    private static final int   CT_DIGITAL_WINDOW_CONTROL = 0x13;
    private static final int   CT_REGION_OF_INTEREST_CONTROL = 0x14;




    private static final int VS_STILL_PROBE_CONTROL = 0x03;
    private static final int VS_STILL_COMMIT_CONTROL = 0x04;
    private static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    private static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;



    private UsbDeviceConnection camDeviceConnection;

    public enum CameraFunction {brightness, autofocus};
    public enum CameraFunctionSetting {defaultValue, auto, adjust}

    private CameraFunction cameraFunction;

    public static int minValue;
    public static int maxValue;
    public int currentValue;
    private int defaultValue;
    private static int resolutionValue;
    private static int infoValue;
    public boolean autoEnabled;
    public static byte bUnitID;
    public static byte bTerminalID;

    public SetCameraVariables(UsbDeviceConnection camConnection, CameraFunction cameraFunct, boolean auto, byte bUnit, byte bTerminal) {
        this.camDeviceConnection = camConnection;
        this.cameraFunction = cameraFunct;
        this.autoEnabled = auto;
        this.bUnitID = bUnit;
        this.bTerminalID = bTerminal;
        initCameraFunction(cameraFunction);
    }

    private void initCameraFunction(CameraFunction cameraFunct) {
        switch (cameraFunct) {
            case brightness:

                int timeout = 5000;
                int len;
                byte[] brightnessParms = new byte[2];
                // PU_BRIGHTNESS_CONTROL(0x02), GET_MIN(0x82) [UVC1.5, p. 160, 158, 96]
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                if (len != brightnessParms.length) {
                    log("Error: Durning PU_BRIGHTNESS_CONTROL");
                }

                log("brightness min: " + unpackIntTwoValues(brightnessParms));
                minValue = unpackIntTwoValues(brightnessParms);
                // CT_FOCUS_AUTO_CONTROL(0x02), GET_MAX(0x83) [UVC1.5, p. 160, 158, 96]
                camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                log("brightness max: " + unpackIntTwoValues(brightnessParms));
                maxValue = unpackIntTwoValues(brightnessParms);
                // PU_BRIGHTNESS_CONTROL(0x02), GET_RES(0x84) [UVC1.5, p. 160, 158, 96]
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_RES, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                log("brightness res: " + unpackIntTwoValues(brightnessParms));
                // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                log("brightness cur: " + unpackIntTwoValues(brightnessParms));
                currentValue = unpackIntTwoValues(brightnessParms);
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_DEF, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                log("brightness default: " + unpackIntTwoValues(brightnessParms));
                defaultValue = unpackIntTwoValues(brightnessParms);
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_INFO, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                log("brightness info: " + unpackIntTwoValues(brightnessParms));
                infoValue = unpackIntTwoValues(brightnessParms);
                break;




            case autofocus:

                timeout = 500;
                byte[] focusParms = new byte[1];
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                if (len != focusParms.length) {
                    log("Error: Durning CT_FOCUS_AUTO_CONTROL, GET_CUR");
                }
                log("current Focus VAlue: " + unpackIntOneValues(focusParms));
                currentValue = unpackIntOneValues(focusParms);

                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_DEF, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                if (len != focusParms.length) {
                    log("Error: Durning CT_FOCUS_AUTO_CONTROL, GET_DEF");
                }
                log("Focus default: " + unpackIntOneValues(focusParms));
                defaultValue = unpackIntOneValues(focusParms);
                len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_INFO, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID, focusParms, focusParms.length, timeout);
                if (len != focusParms.length) {
                    log("Error: Durning CT_FOCUS_AUTO_CONTROL, GET_INFO");
                }
                log("Focus info: " + unpackIntOneValues(focusParms));
                infoValue = unpackIntOneValues(focusParms);
                break;

            default:
                throw new AssertionError();

        }
    }

    public void adjustValue(CameraFunctionSetting setting) {

        switch (cameraFunction) {
            // temporary solution
            case brightness:
                int timeout = 500;
                int len;
                byte[] brightnessParms = new byte[2];

                switch (setting) {

                    case defaultValue:

                        packIntTwoValues(defaultValue, brightnessParms);
                        // PU_BRIGHTNESS_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                        if (len != brightnessParms.length) {
                            log("Error: Durning PU_BRIGHTNESS_CONTROL");
                        }
                        // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                        if (len != brightnessParms.length) {
                            log("Error: Durning PU_BRIGHTNESS_CONTROL");
                        } else {
                            currentValue = unpackIntTwoValues(brightnessParms);
                            log( "currentBrightness: " + currentValue);
                        }
                        break;

                    case adjust:

                        packIntTwoValues(currentValue, brightnessParms);
                        // PU_BRIGHTNESS_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                        if (len != brightnessParms.length) {
                            log("Error: Durning PU_BRIGHTNESS_CONTROL");
                        }
                        // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, bUnitID <<8, brightnessParms, brightnessParms.length, timeout);
                        if (len != brightnessParms.length) {
                            log("Error: Durning PU_BRIGHTNESS_CONTROL");
                        } else {
                            currentValue = unpackIntTwoValues(brightnessParms);
                            log( "currentBrightness: " + currentValue);
                        }
                        break;
                }

                break;

            case autofocus:

                byte[] focusParms = new byte [1];
                timeout = 500;
                if(autoEnabled) {

                    packIntOneValues(currentValue, focusParms);
                    // CT_FOCUS_AUTO_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                    if (len != focusParms.length) {
                        log("Error: Durning CT_FOCUS_AUTO_CONTROL - SET_CUR");
                    }
                    // CT_FOCUS_AUTO_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                    if (len != focusParms.length) {
                        log("Error: Durning CT_FOCUS_AUTO_CONTROL - GET_CUR");
                    } else {
                        currentValue = unpackIntOneValues(focusParms);
                        log( "currentBrightness: " + currentValue);
                    }

                } else {

                    packIntOneValues(currentValue, focusParms);
                    // CT_FOCUS_AUTO_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                    if (len != focusParms.length) {
                        log("Error: Durning CT_FOCUS_AUTO_CONTROL");
                    }
                    // CT_FOCUS_AUTO_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CT_FOCUS_AUTO_CONTROL << 8, bTerminalID <<8, focusParms, focusParms.length, timeout);
                    if (len != focusParms.length) {
                        log("Error: Durning CT_FOCUS_AUTO_CONTROL");
                    } else {
                        currentValue = unpackIntOneValues(focusParms);
                        log( "currentBrightness: " + currentValue);
                    }

                }

                break;

            default:
                throw new AssertionError();

        }

    }



    private void log(String msg) {
        Log.i("UsbCamTest1", msg);
    }

    private void displayErrorMessage(Throwable e) {
        Log.e("UsbCamTest1", "Error in MainActivity", e);
    }

    private static void packIntOneValues(int i, byte[] buf) {
        buf[0] = (byte) (i & 0xFF);
    }

    private static int unpackIntOneValues(byte[] buf) {
        return ( (buf[0] & 0xFF));

    }

    private static void packIntTwoValues(int i, byte[] buf) {
        buf[0] = (byte) (i & 0xFF);
        buf[0 + 1] = (byte) ((i >>> 8) & 0xFF);
    }

    private static int unpackIntTwoValues(byte[] buf) {
        return (((buf[1] ) << 8) | (buf[0] & 0xFF));

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


}
