package humer.UvcCamera.LibUsb;


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface I_LibUsb extends Library {

    public static final I_LibUsb INSTANCE = Native.load("Usb_Support", I_LibUsb.class);

    public void getFramesOverLibUsb(int packetsPerRequest, int maxPacketSize, int activeUrbs, int camStreamingAltSetting, int camFormatIndex,
                             int camFrameIndex, int camFrameInterval, int imageWidth, int imageHeight, int yuvFrameIsZero, int stream );

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

    public void closeLibUsb();

    public Pointer probeCommitControl(int bmHint, int camFormatInde,
                                      int camFrameInde, int camFrameInterva);

    public void sendCtlForConnection(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva);

    public void probeCommitControl_cleanup();

    public void init (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                      int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpoint, int camStreamingInterfaceNumber,
                      String frameFormat);

}
