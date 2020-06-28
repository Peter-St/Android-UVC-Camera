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

import android.hardware.usb.UsbDeviceConnection;
import android.util.Log;

import java.math.BigInteger;

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


    // Processing Unit Control Selectors

    private static final int PU_CONTROL_UNDEFINED = 0x00;
    private static final int PU_BACKLIGHT_COMPENSATION_CONTROL = 0x01;
    private static final int PU_BRIGHTNESS_CONTROL = 0x02;
    private static final int PU_CONTRAST_CONTROL = 0x03;
    private static final int PU_GAIN_CONTROL = 0x04;
    private static final int PU_POWER_LINE_FREQUENCY_CONTROL = 0x05;
    private static final int PU_HUE_CONTROL = 0x06;
    private static final int PU_SATURATION_CONTROL = 0x07;
    private static final int PU_SHARPNESS_CONTROL = 0x08;
    private static final int PU_GAMMA_CONTROL = 0x09;
    private static final int PU_WHITE_BALANCE_TEMPERATURE_CONTROL = 0x0A;
    private static final int PU_WHITE_BALANCE_TEMPERATURE_AUTO_CONTROL = 0x0B;
    private static final int PU_WHITE_BALANCE_COMPONENT_CONTROL = 0x0C;
    private static final int PU_WHITE_BALANCE_COMPONENT_AUTO_CONTROL = 0x0D;
    private static final int PU_DIGITAL_MULTIPLIER_CONTROL = 0x0E;
    private static final int PU_DIGITAL_MULTIPLIER_LIMIT_CONTROL = 0x0F;
    private static final int PU_HUE_AUTO_CONTROL = 0x10;
    private static final int PU_ANALOG_VIDEO_STANDARD_CONTROL = 0x11;
    private static final int PU_ANALOG_LOCK_STATUS_CONTROL = 0x12;

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

    public enum CameraFunction {brightness, contrast, hue, saturation, sharpness, gamma, gain, power_line_frequency, autofocus, auto_exposure_mode};
    public enum CameraFunctionSetting {defaultAdjust, auto, adjust}

    private CameraFunction cameraFunction;

    public static int minValue;
    public static int maxValue;
    public int currentValue;
    private static int defaultValue;
    private static int resolutionValue;
    private static int infoValue;
    public boolean autoEnabled;
    public static byte bUnitID;
    public static byte bTerminalID;

    private static int CONTROL;
    private static byte bID;

    public SetCameraVariables(UsbDeviceConnection camConnection, CameraFunction cameraFunct, boolean auto, byte bUnit, byte bTerminal) {
        this.camDeviceConnection = camConnection;
        this.cameraFunction = cameraFunct;
        this.autoEnabled = auto;
        this.bUnitID = bUnit;
        this.bTerminalID = bTerminal;
        initCameraFunction();
    }

    private void initCameraFunction() {

        int timeout = 500;
        int len;
        byte[] controlParms = null;
        boolean BOOL_GET_CUR = false;
        boolean BOOL_GET_RES = false;
        boolean BOOL_GET_INFO = false;
        boolean BOOL_GET_DEF = false;
        boolean BOOL_GET_MIN = false;
        boolean BOOL_GET_MAX = false;

        switch (cameraFunction) {
            // temporary solution
            case brightness:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_BRIGHTNESS_CONTROL;
                break;

            case contrast:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_CONTRAST_CONTROL;
                break;


            case hue:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_HUE_CONTROL;
                break;

            case saturation:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_SATURATION_CONTROL;
                break;

            case sharpness:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_SHARPNESS_CONTROL;
                break;

            case gamma:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_GAMMA_CONTROL;
                break;

            case gain:
                bID = bUnitID;
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_MAX = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_GAIN_CONTROL;
                break;

            case power_line_frequency:
                bID = bUnitID;
                controlParms = new byte[1];
                BOOL_GET_CUR = true;
                BOOL_GET_MIN = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = PU_POWER_LINE_FREQUENCY_CONTROL;
                break;

            case autofocus:
                bID = bTerminalID;
                controlParms = new byte [1];
                BOOL_GET_CUR = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = CT_FOCUS_AUTO_CONTROL;
                break;

            case auto_exposure_mode:
                bID = bTerminalID;
                controlParms = new byte [1];
                BOOL_GET_CUR = true;
                BOOL_GET_RES = true;
                BOOL_GET_INFO = true;
                BOOL_GET_DEF = true;
                CONTROL = CT_AE_MODE_CONTROL;
                break;

            default:
                throw new AssertionError();
        }


        if (BOOL_GET_MIN) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length) log("Error: Durning CONTROL GET_MIN");

            if(controlParms.length == 1){
                log("Min Value: " + unpackIntOneValues(controlParms));
                minValue = unpackIntOneValues(controlParms);
            }
            else if (controlParms.length == 2) {
                log("Min Value: " + unpackIntTwoValues(controlParms));
                minValue = unpackIntTwoValues(controlParms);
            }
        }
        if (BOOL_GET_MAX) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length) log("Error: Durning CONTROL GET_MAX");

            if(controlParms.length == 1){
                log("Max Value: " + unpackIntOneValues(controlParms));
                maxValue = unpackIntOneValues(controlParms);
            }
            else if (controlParms.length == 2) {
                log("Max Value: " + unpackIntTwoValues(controlParms));
                maxValue = unpackIntTwoValues(controlParms);
            }
        }
        if (BOOL_GET_RES) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_RES, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length) log("Error: Durning CONTROL GET_RES");

            if(controlParms.length == 1){
                log("RES Value: " + unpackIntOneValues(controlParms));
                resolutionValue = unpackIntOneValues(controlParms);
            }
            else if (controlParms.length == 2) {
                log("RES Value: " + unpackIntTwoValues(controlParms));
                resolutionValue = unpackIntTwoValues(controlParms);
            }
        }
        if (BOOL_GET_CUR) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length) log("Error: Durning CONTROL GET_CUR");
            if(controlParms.length == 1){
                log("CUR Value: " + unpackIntOneValues(controlParms));
                currentValue = unpackIntOneValues(controlParms);
            }
            else if (controlParms.length == 2) {
                log("CUR Value: " + unpackIntTwoValues(controlParms));
                currentValue = unpackIntTwoValues(controlParms);
            }
        }
        if (BOOL_GET_DEF) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_DEF, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length)  log("Error: Durning CONTROL GET_DEF");
            if(controlParms.length == 1){
                log("DEF Value: " + unpackIntOneValues(controlParms));
                defaultValue = unpackIntOneValues(controlParms);
            }
            else if (controlParms.length == 2) {
                log("DEF Value: " + unpackIntTwoValues(controlParms));
                defaultValue = unpackIntTwoValues(controlParms);
            }
        }
        if (BOOL_GET_INFO) {
            len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_INFO, CONTROL << 8, bID << 8, controlParms, controlParms.length, timeout);
            if (len != controlParms.length)   log("Error: Durning CONTROL GET_INFO");

            if(controlParms.length == 1){  log("INFO Value: " + unpackIntOneValues(controlParms));  infoValue = unpackIntOneValues(controlParms); }
            else if (controlParms.length == 2) { log("INFO Value: " + unpackIntTwoValues(controlParms)); infoValue = unpackIntTwoValues(controlParms);  }
        }



        switch (cameraFunction) {

            case auto_exposure_mode:
                // D0: Manual Mode – manual Exposure Time, manual Iris
                // D1: Auto Mode – auto Exposure Time, auto Iris
                // D2: Shutter Priority Mode - manual Exposure Time, auto Iris
                // D3: Aperture Priority Mode – auto Exposure Time, manual Iris
                if(BigInteger.valueOf(currentValue).testBit(0)) autoEnabled = false;
                else if(BigInteger.valueOf(currentValue).testBit(1)) autoEnabled = true;
                else if(BigInteger.valueOf(currentValue).testBit(2)) autoEnabled = false;
                else if(BigInteger.valueOf(currentValue).testBit(3)) autoEnabled = true;
                break;
            case autofocus:
                if (currentValue == 0) autoEnabled = false;
                else if (currentValue == 1) autoEnabled = true;
                break;

            default:
                break;
        }

    }

    public void adjustValue(CameraFunctionSetting setting) {
        log("adjust");
        int timeout = 500;
        int len;
        byte[] controlParms = null;
        boolean BOOL_SET_CUR = true;
        boolean BOOL_GET_CUR = false;
        boolean BOOL_GET_RES;
        boolean BOOL_GET_INFO;
        boolean BOOL_GET_DEF;
        boolean BOOL_GET_MIN;
        boolean BOOL_GET_MAX;
        boolean exit = false;

        switch (cameraFunction) {
            case brightness:
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                break;
            case contrast:
                controlParms = new byte[2];
                BOOL_GET_CUR = true;
                break;



            case autofocus:
                controlParms = new byte [1];
                BOOL_GET_CUR = true;
                break;

            case auto_exposure_mode:
                controlParms = new byte [1];
                BOOL_GET_CUR = true;
                exit = true;
                break;

            default:
                throw new AssertionError();
        }

        switch (setting) {

            case defaultAdjust:
                if (controlParms.length == 1) packIntOneValues(defaultValue, controlParms);
                else if (controlParms.length == 2) packIntTwoValues(defaultValue, controlParms);

                if(BOOL_SET_CUR) {
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                    if (len != controlParms.length) {
                        log("Error: Durning CONTROL");
                    }
                    if (controlParms.length == 1) currentValue = unpackIntOneValues(controlParms);
                    else if (controlParms.length == 2) currentValue = unpackIntTwoValues(controlParms);
                }
                break;

            case adjust:
                if (controlParms.length == 1) packIntOneValues(currentValue, controlParms);
                else if (controlParms.length == 2) packIntTwoValues(currentValue, controlParms);
                if(BOOL_SET_CUR) {
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                    if (len != controlParms.length) {
                        log("Error: Durning CONTROL");
                    }
                }
                break;


            case auto:
                if (exit) break;
                if(autoEnabled) {
                    if (controlParms.length == 1) packIntOneValues(0x01, controlParms);
                    else if (controlParms.length == 2) packIntTwoValues(0x01, controlParms);
                    if(BOOL_SET_CUR) {
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                        if (len != controlParms.length) {
                            log("Error: Durning CONTROL - SET_CUR");
                        }
                    }
                    if (BOOL_GET_CUR) {
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                        if (len != controlParms.length) {
                            log("Error: Durning CONTROL - GET_CUR");
                        } else {
                            if (controlParms.length == 1) currentValue = unpackIntOneValues(controlParms);
                            else if (controlParms.length == 2) currentValue = unpackIntTwoValues(controlParms);
                            log( "currentValue: " + currentValue);
                        }
                    }
                } else {
                    if (controlParms.length == 1) packIntOneValues(0x00, controlParms);
                    else if (controlParms.length == 2) packIntTwoValues(0x00, controlParms);
                    if(BOOL_SET_CUR) {
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                        if (len != controlParms.length) {
                            log("Error: Durning CONTROL");
                        }
                    }
                    if (BOOL_GET_CUR) {
                        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                        if (len != controlParms.length) {
                            log("Error: Durning CONTROL");
                        } else {
                            if (controlParms.length == 1) currentValue = unpackIntOneValues(controlParms);
                            else if (controlParms.length == 2) currentValue = unpackIntTwoValues(controlParms);
                            log( "currentValue: " + currentValue);
                        }
                    }
                }
                break;
            default:
                throw new AssertionError();
        }

        switch (cameraFunction) {

            case auto_exposure_mode:
                if (autoEnabled) {
                    packIntOneValues(defaultValue, controlParms);
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                    if (len != controlParms.length) {
                        log("Error: Durning CONTROL - SET_CUR");
                    }
                } else {
                    packIntOneValues(0x01, controlParms);
                    len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, CONTROL << 8, bID <<8, controlParms, controlParms.length, timeout);
                    if (len != controlParms.length) {
                        log("Error: Durning CONTROL - SET_CUR");
                    }
                }
                break;

            default:
                break;
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
