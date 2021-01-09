/*
Copyright 2020 Peter Stoiber

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

package humer.UvcCamera.LibUsb;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public interface JNA_I_LibUsb extends Library {

    public static final JNA_I_LibUsb INSTANCE = Native.load("Usb_Support", JNA_I_LibUsb.class);

    public void set_the_native_Values (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
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

    public void stopJavaVM();

    public void exit();

    public Pointer probeCommitControl(int bmHint, int camFormatInde,
                                      int camFrameInde, int camFrameInterva, int FD);

    //public void probeCommitControl_cleanup();

    public void getFramesOverLibUsb(int yuvFrameIsZero, int stream, int whichTestrun);

    public void setRotation(int rot, int horizontalFlip, int verticalFlip);

    // WebRtc Methods
    public void prepairTheStream_WebRtc_Service();
    public void lunchTheStream_WebRtc_Service();

    // Stream Activity
    public void setImageCapture();
    public void startVideoCapture();
    public void stopVideoCapture();
    public void setImageCaptureLongClick();
    public void startVideoCaptureLongClick() ;
    public void stopVideoCaptureLongClick() ;

    // move to Native Methods:
    public int fetchTheCamStreamingEndpointAdress (int FD);


}
