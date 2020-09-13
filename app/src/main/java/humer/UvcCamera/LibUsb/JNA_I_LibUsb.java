package humer.UvcCamera.LibUsb;


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface JNA_I_LibUsb extends Library {

    public static final JNA_I_LibUsb INSTANCE = Native.load("Usb_Support", JNA_I_LibUsb.class);

    public void init (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                      int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpoint, int camStreamingInterfaceNumber,
                      String frameFormat, int numberOfAutoFrames, int bcdUVC_int);

    public int initStreamingParms(int FD);

    public void startAutoDetection ();

    public interface autoStreamComplete extends Callback {
        boolean callback();
    }
    public void setAutoStreamComplete(autoStreamComplete AutoStreamComplete);

    @Structure.FieldOrder({"spacketCnt", "spacket0Cnt", "spacket12Cnt", "spacketDataCnt", "spacketHdr8Ccnt", "spacketErrorCnt", "sframeCnt", "sframeLen", "requestCnt", "sframeLenArray"})
    public static class Libusb_Auto_Values extends Structure {
        public static class ByValue extends Libusb_Auto_Values implements Structure.ByValue {}
        public int spacketCnt;
        public int spacket0Cnt;
        public int spacket12Cnt;
        public int spacketDataCnt;
        public int spacketHdr8Ccnt;
        public int spacketErrorCnt;
        public int sframeCnt;
        public int sframeLen;
        public int requestCnt;
        public int[] sframeLenArray = new int [5];
    }
    public Libusb_Auto_Values.ByValue get_autotransferStruct();

    public void closeLibUsb();

    public interface eventCallback extends Callback {
        boolean callback(Pointer videoFrame, int frameSize);
    }
    public void setCallback(eventCallback evnHnd);


    public interface logPrint extends Callback {
        boolean callback(String msg);
    }
    public void setLogPrint(logPrint evnHnd);

    public void stopStreaming();

    public void exit();

    public Pointer probeCommitControl(int bmHint, int camFormatInde,
                                      int camFrameInde, int camFrameInterva);

    public void probeCommitControl_cleanup();

    public void getFramesOverLibUsb(int packetsPerRequest, int maxPacketSize, int activeUrbs, int camStreamingAltSetting, int camFormatIndex,
                                    int camFrameIndex, int camFrameInterval, int imageWidth, int imageHeight, int yuvFrameIsZero, int stream );


}
