package humer.UvcCamera.AutomaticDetection;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.material.textfield.TextInputLayout;

import humer.UvcCamera.AutomaticDetection.Jna_AutoDetect;
import humer.UvcCamera.SetUpTheUsbDevice;


public class Jna_AutoDetect_Handler {

    private Context mContext;
    private Activity activity;
    private SetUpTheUsbDevice setUpTheUsbDevice;

    public static int sALT_SETTING;
    public static int smaxPacketSize ;
    public static int scamFormatIndex ;   // MJPEG // YUV // bFormatIndex: 1 = uncompressed
    public static String svideoformat;
    public static int scamFrameIndex ; // bFrameIndex: 1 = 640 x 360;       2 = 176 x 144;     3 =    320 x 240;      4 = 352 x 288;     5 = 640 x 480;
    public static int simageWidth;
    public static int simageHeight;
    public static int scamFrameInterval ; // 333333 YUV = 30 fps // 666666 YUV = 15 fps
    public static int spacketsPerRequest ;
    public static int sactiveUrbs ;
    public static String sdeviceName;
    public static byte bUnitID;
    public static byte bTerminalID;
    public static byte[] bNumControlTerminal;
    public static byte[] bNumControlUnit;
    public static byte bStillCaptureMethod;
    private static boolean libUsb;

    private static int spacketCnt;
    private static int spacket0Cnt ;
    private static int spacket12Cnt;
    private static int spacketDataCnt;
    private static int spacketHdr8Ccnt;
    private static int spacketErrorCnt ;
    private static int sframeCnt ;
    private static int sframeLen ;
    private static int [] sframeLenArray;
    private static int [] [] shighestFramesCube;
    private static int srequestCnt = 0;
    private static int sframeMaximalLen = 0;
    private static boolean fiveFrames;
    // how many transfers completed
    private static int doneTransfers;
    private static boolean highQuality;
    public static boolean maxPacketsPerRequestReached;
    public static boolean maxActiveUrbsReached;
    private static String progress;
    private static boolean submiterror;




    private static boolean max_Framelength_cant_reached;


    public Jna_AutoDetect_Handler(SetUpTheUsbDevice setUpTheUsbDevice, Context mContext) {
        this.setUpTheUsbDevice = setUpTheUsbDevice;
        this.mContext = mContext;
        fetchTheValues();


    }

    //  return -1 == error
    //  return 0 == sucess
    //  return 1 == startAutoDetection
    public int compare() {
        fetchTheValues();
        if (submiterror) {
            setUpTheUsbDevice.transferSucessful = false;
            int i = solveSubmitError();
            writeTheValues();
            return i;
        }
        else {
            setUpTheUsbDevice.transferSucessful = true;
            setUpTheUsbDevice.sucessfulDoneTransfers ++;
        }







        if (is_framelength_as_long_as_expected_maxSize()) {
            if (!fiveFrames) {
                fiveFrames = true;
                writeTheValues();
                return 1;
            } else {
                if (!highQuality) {
                    highQuality = true;
                    writeTheValues();
                    return 1;
                } else {
                    writeTheValues();
                    return 0;
                }
            }
        }
        if (!setUpTheUsbDevice.maxPacketsPerRequestReached) {
            switch (spacketsPerRequest) {
                case 1:
                    setUpTheUsbDevice.progress = "3% done";
                    spacketsPerRequest = 2;
                    writeTheValues();
                    return 1;
                case 2:
                    setUpTheUsbDevice.progress = "5% done";

                    spacketsPerRequest = 4;
                    writeTheValues();
                    return 1;
                case 4:
                    setUpTheUsbDevice.progress = "8% done";
                    spacketsPerRequest = 8;
                    writeTheValues();
                    return 1;
                case 8:
                    setUpTheUsbDevice.progress = "10% done";
                    spacketsPerRequest = 16;
                    writeTheValues();
                    return 1;
                case 16:
                    setUpTheUsbDevice.progress = "12% done";
                    spacketsPerRequest = 32;
                    writeTheValues();
                    return 1;
                case 32:
                    maxPacketsPerRequestReached = true;
                    setUpTheUsbDevice.maxPacketsPerRequestReached = true;
                    break;
            }
        }

        if (!setUpTheUsbDevice.maxActiveUrbsReached) {
            switch (sactiveUrbs) {
                case 1:
                    setUpTheUsbDevice.progress = "20% done";
                    sactiveUrbs = 2;
                    writeTheValues();
                    return 1;
                case 2:
                    setUpTheUsbDevice.progress = "30% done";
                    sactiveUrbs = 4;
                    writeTheValues();
                    return 1;
                case 4:
                    setUpTheUsbDevice.progress = "40% done";
                    sactiveUrbs = 8;
                    writeTheValues();
                    return 1;
                case 8:
                    setUpTheUsbDevice.progress = "50% done";
                    sactiveUrbs = 16;
                    writeTheValues();
                    return 1;
                case 16:
                    setUpTheUsbDevice.progress = "60% done";
                    sactiveUrbs = 32;
                    writeTheValues();
                    return 1;
                case 32:
                    setUpTheUsbDevice.progress = "70% done";
                    maxActiveUrbsReached = true;
                    setUpTheUsbDevice.maxActiveUrbsReached = true;
                    break;
            }
        }
        writeTheValues();
        return -1;
    }

    private int solveSubmitError() {
        if (setUpTheUsbDevice.sucessfulDoneTransfers > 1) {
            if (setUpTheUsbDevice.last_transferSucessful) {
               restoreLastValues();
               return -1;
            }
        }
        return -1;
    }

    private void restoreLastValues() {
        setUpTheUsbDevice.last_camStreamingAltSetting = sALT_SETTING;
        setUpTheUsbDevice.last_camFormatIndex = scamFormatIndex;
        setUpTheUsbDevice.last_camFrameIndex = scamFrameIndex;
        setUpTheUsbDevice.last_camFrameInterval = scamFrameInterval;
        setUpTheUsbDevice.last_packetsPerRequest = spacketsPerRequest;
        setUpTheUsbDevice.last_maxPacketSize = smaxPacketSize;
        setUpTheUsbDevice.last_imageWidth = simageWidth;
        setUpTheUsbDevice.last_imageHeight = simageHeight;
        setUpTheUsbDevice.last_activeUrbs = sactiveUrbs;
        setUpTheUsbDevice.last_videoformat = svideoformat;

    }

    private boolean is_framelength_as_long_as_expected_maxSize() {
        int maxSize = simageWidth * simageWidth *2;
        if (!fiveFrames) {
            if (sframeLen >= maxSize) return true;
            else return false;
        } else {
            if (sframeLenArray == null) return false;
            if ((sframeLenArray[0] >= maxSize & sframeLenArray[1] >= maxSize & sframeLenArray[2] >= maxSize & sframeLenArray[3] >= maxSize & sframeLenArray[4] >= maxSize )) return true;
            else return false;
        }
    }

    public void fetchTheValues() {
        if (setUpTheUsbDevice != null) {
            sALT_SETTING = setUpTheUsbDevice.camStreamingAltSetting;
            svideoformat = setUpTheUsbDevice.videoformat;
            scamFormatIndex = setUpTheUsbDevice.camFormatIndex;
            simageWidth = setUpTheUsbDevice.imageWidth;
            simageHeight = setUpTheUsbDevice.imageHeight;
            scamFrameIndex = setUpTheUsbDevice.camFrameIndex;
            scamFrameInterval = setUpTheUsbDevice.camFrameInterval;
            spacketsPerRequest = setUpTheUsbDevice.packetsPerRequest;
            smaxPacketSize = setUpTheUsbDevice.maxPacketSize;
            sactiveUrbs = setUpTheUsbDevice.activeUrbs;
            sdeviceName = setUpTheUsbDevice.deviceName;
            bUnitID = setUpTheUsbDevice.bUnitID;
            bTerminalID = setUpTheUsbDevice.bTerminalID;
            bNumControlTerminal = setUpTheUsbDevice.bNumControlTerminal;
            bNumControlUnit = setUpTheUsbDevice.bNumControlUnit;
            bStillCaptureMethod = setUpTheUsbDevice.bStillCaptureMethod;
            libUsb = setUpTheUsbDevice.libUsb;
            progress = setUpTheUsbDevice.progress;
            submiterror = setUpTheUsbDevice.submiterror;
            sframeLenArray = setUpTheUsbDevice.sframeLenArray;


            spacketCnt = setUpTheUsbDevice.spacketCnt;
            spacket0Cnt = setUpTheUsbDevice.spacket0Cnt;
            spacket12Cnt = setUpTheUsbDevice.spacket12Cnt;
            spacketDataCnt = setUpTheUsbDevice.spacketDataCnt;
            spacketHdr8Ccnt = setUpTheUsbDevice.spacketHdr8Ccnt;
            spacketErrorCnt = setUpTheUsbDevice.spacketErrorCnt;
            sframeCnt = setUpTheUsbDevice.sframeCnt;
            sframeLen = setUpTheUsbDevice.sframeLen;
            srequestCnt = setUpTheUsbDevice.srequestCnt;
            fiveFrames = setUpTheUsbDevice.fiveFrames;
            doneTransfers = setUpTheUsbDevice.doneTransfers;
            highQuality = setUpTheUsbDevice.highQuality;
            max_Framelength_cant_reached = setUpTheUsbDevice.max_Framelength_cant_reached;
            maxPacketsPerRequestReached = setUpTheUsbDevice.maxPacketsPerRequestReached;
            maxActiveUrbsReached = setUpTheUsbDevice.maxActiveUrbsReached;


        }
    }

    public void writeTheValues() {
        if (setUpTheUsbDevice != null) {
            setUpTheUsbDevice.packetsPerRequest = spacketsPerRequest;
            setUpTheUsbDevice.activeUrbs = sactiveUrbs;
            setUpTheUsbDevice.fiveFrames = fiveFrames;
            setUpTheUsbDevice.highQuality = highQuality;
            setUpTheUsbDevice.max_Framelength_cant_reached = max_Framelength_cant_reached;
            setUpTheUsbDevice.sframeLenArray = sframeLenArray;


            // other values
            setUpTheUsbDevice.camStreamingAltSetting = sALT_SETTING;
            setUpTheUsbDevice.videoformat = svideoformat;
            setUpTheUsbDevice.camFormatIndex = scamFormatIndex;
            setUpTheUsbDevice.imageWidth = simageWidth;
            setUpTheUsbDevice.imageHeight = simageHeight;
            setUpTheUsbDevice.camFrameIndex = scamFrameIndex;
            setUpTheUsbDevice.camFrameInterval = scamFrameInterval;
            setUpTheUsbDevice.maxPacketSize = smaxPacketSize;
            setUpTheUsbDevice.deviceName = sdeviceName;
            setUpTheUsbDevice.bUnitID = bUnitID;
            setUpTheUsbDevice.bTerminalID = bTerminalID;
            setUpTheUsbDevice.bNumControlTerminal = bNumControlTerminal;
            setUpTheUsbDevice.bNumControlUnit = bNumControlUnit;
            setUpTheUsbDevice.bStillCaptureMethod = bStillCaptureMethod;
            setUpTheUsbDevice.libUsb = libUsb;
            setUpTheUsbDevice.maxPacketsPerRequestReached = maxPacketsPerRequestReached;
            setUpTheUsbDevice.maxActiveUrbsReached = maxActiveUrbsReached;

        }

    }

    private void findHighestFrameLengths() {

        // find the highest Transferlength:
        int[] lengthOne = findHighestLength();



        if (lengthOne[1] == 0) {
            sactiveUrbs = 4;
            spacketsPerRequest = 4;
            log("4 / 4");
        } else if (lengthOne[1] == 0) {
            sactiveUrbs = 16;
            spacketsPerRequest = 16;
            lengthOne = findHighestLength();
        }





        log("lengthOne[0] = " + lengthOne[0]);
        // Test lowest package size ...
        setTheMaxPacketSize(false, true, 0);



        int[] lengthTwo = findHighestLength();


        log("lengthTwo[0] = " + lengthTwo[0]);
        if (lengthOne[0] > lengthTwo[0]) {
            log("lengthOne[0] > lengthTwo[0]  -->  " + lengthOne[0] + " > " + lengthTwo[0]);
            setTheMaxPacketSize(true, false, 0);
            if (lengthOne[1] == 0) {
                sactiveUrbs = 16;
                spacketsPerRequest = 16;
            } else if (lengthOne[1] == 1) {
                sactiveUrbs = 4;
                spacketsPerRequest = 4;
            }
        } else {
            log("lengthOneo[0] < lengthTwo[0]  -->  " + lengthOne[0] + " > " + lengthTwo[0]);
            if (lengthTwo[1] == 0) {
                sactiveUrbs = 16;
                spacketsPerRequest = 16;
            } else if (lengthTwo[1] == 1) {
                sactiveUrbs = 4;
                spacketsPerRequest = 4;
            }
        }

        //finalAutoMethod();

    }

    private void setTheMaxPacketSize (boolean highest, boolean lowest, int value) {

        if (highest) {
            int[] maxPacketsSizeArray = setUpTheUsbDevice.convertedMaxPacketSize.clone();
            int minValue = maxPacketsSizeArray[0];
            int minPos = 0;
            for (int i = 0; i < maxPacketsSizeArray.length; i++) {
                if (maxPacketsSizeArray[i] < minValue) {
                    minValue = maxPacketsSizeArray[i];
                    minPos = i;
                }
            }
            sALT_SETTING = (minPos + 1);
            smaxPacketSize = maxPacketsSizeArray[minPos];
        } else if (lowest) {
            int[] maxPacketsSizeArray = setUpTheUsbDevice.convertedMaxPacketSize.clone();
            int maxValue = maxPacketsSizeArray[0];
            int maxPos = 0;
            for (int i = 0; i < maxPacketsSizeArray.length; i++) {
                if (maxPacketsSizeArray[i] < maxValue) {
                    maxValue = maxPacketsSizeArray[i];
                    maxPos = i;
                }
            }
            sALT_SETTING = (maxPos + 1);
            smaxPacketSize = maxPacketsSizeArray[maxPos];
        } else {
            int[] maxPacketsSizeArray = setUpTheUsbDevice.convertedMaxPacketSize.clone();
            if (maxPacketsSizeArray.length >= value) {
                sALT_SETTING = (value + 1);
                smaxPacketSize = maxPacketsSizeArray[value];
            }
        }
    }

    private int [] findHighestLength () {
        int lenght;
        int highestlength = 0;
        int num = 0;
        for (int i = 0; i < sframeCnt; i++) {
            /*
            lenght

            lenght = shighestFramesCube[i][0] + shighestFramesCube[i][1] + shighestFramesCube[i][2] + shighestFramesCube[i][3] + shighestFramesCube[i][4];
            if(lenght > highestlength) {
                highestlength = lenght;
                num = i;
            }

             */
        }
        int [] ret = new int [2];
        ret[0] = highestlength;
        ret[1] = num;
        return ret;
    }

    private void log(String msg) {
        Log.i("Jna_AutoDetect_Handler", msg);
    }
}
