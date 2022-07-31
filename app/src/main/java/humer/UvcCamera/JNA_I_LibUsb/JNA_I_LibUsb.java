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

package humer.UvcCamera.JNA_I_LibUsb;

import com.sun.jna.Callback;
import com.sun.jna.IntegerType;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;
import com.sun.jna.ptr.IntByReference;

public interface JNA_I_LibUsb extends Library {

    public static final JNA_I_LibUsb INSTANCE = Native.load("Uvc_Support", JNA_I_LibUsb.class);

    public int set_the_native_Values(Pointer uvc_camera, int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                                      int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpoint, int camStreamingInterfaceNumber,
                                      String frameFormat, int numberOfAutoFrames, int bcdUVC_int, int lowAndroid);

    public int initStreamingParms(Pointer uvc_camera, int FD);

    public interface autoStreamComplete extends Callback {
        boolean callback();
    }

    public void setAutoStreamComplete(autoStreamComplete AutoStreamComplete);



    public Libusb_Auto_Values.ByValue get_autotransferStruct();

    public void closeLibUsb();

    //// SetUpTheUsbDevice Method
    public interface eventCallback extends Callback {
        boolean callback(Pointer videoFrame, int frameSize);
    }
    public void setCallback(eventCallback evnHnd);

    //// SetUpTheUsbDevice Automatic Method
    public interface eventCallbackAuto extends Callback {
        boolean callback(auto_detect_struct.ByReference auto_values);
    }
    public void setCallbackAuto(eventCallbackAuto evnHnd);

    public void stopStreaming(Pointer uvc_camera);

    public void stopJavaVM();

    //public void probeCommitControl_cleanup();

    public void probeCommitControlUVC();

    public uvc_stream_ctrl.ByValue probeSetCur_TransferUVC(Pointer uvc_camera);
    public uvc_stream_ctrl.ByValue probeGetCur_TransferUVC(Pointer uvc_camera, uvc_stream_ctrl.ByValue ctrl);
    public uvc_stream_ctrl.ByValue CommitSetCur_TransferUVC(Pointer uvc_camera, uvc_stream_ctrl.ByValue ctrl);
    public uvc_stream_ctrl.ByValue CommitGetCur_TransferUVC(Pointer uvc_camera, uvc_stream_ctrl.ByValue ctrl);

    public void getOneFrameUVC(Pointer uvcCamera, uvc_stream_ctrl.ByValue ctrl);

    public void getFramesOverLibUsb5sec(Pointer uvc_camera, uvc_stream_ctrl.ByValue ctrl);

    public void getFramesOverLibUVC(Pointer uvc_camera, int yuvFrameIsZero, int stream, int whichTestrun);

    public void setRotation(int rot, int horizontalFlip, int verticalFlip);

    // WebRtc Methods
    public void prepairTheStream_WebRtc_Service();

    public void setImageCapture();

    public void startVideoCapture();

    public void stopVideoCapture();

    public void setImageCaptureLongClick();

    public void startVideoCaptureLongClick();

    public void stopVideoCaptureLongClick();

    // move to Native Methods:
    public int fetchTheCamStreamingEndpointAdress(Pointer uvc_camera, int FD);

    // Frame Conversation:
    public Pointer convertUYVYtoJPEG(Pointer UYVY_frame_array, IntByReference jpgLength, int UYVYframeLength, int imageWidth, int imageHeight);

    // Frame Complete Callback Method
    public interface frameComplete extends Callback {
        boolean callback(int bitmap);
    }

    public void setFrameComplete(frameComplete evnHnd);

    // LibUsb Methods
    public int initLibUsb();
    public uvc_device_info.ByReference listDeviceUvc(Pointer uvc_camera, int fd);
    public void automaticDetection(Pointer uvc_camera);

    // c++ Methods






    ///////////////////   UVC Enums
    /**
     * Input terminal type (B.2)
     */
    public enum uvc_it_type {
        UVC_ITT_VENDOR_SPECIFIC (0x0200),
        UVC_ITT_CAMERA  (0x0201),
        UVC_ITT_MEDIA_TRANSPORT_INPUT (0x0202),;
        private final int value;
        uvc_it_type(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }


    /**
     * VideoStreaming interface descriptor subtype (A.6)
     */
    public enum uvc_vs_desc_subtype {
        UVC_VS_UNDEFINED (0x00),
        UVC_VS_INPUT_HEADER (0x01),
        UVC_VS_OUTPUT_HEADER (0x02),
        UVC_VS_STILL_IMAGE_FRAME  (0x03),
        UVC_VS_FORMAT_UNCOMPRESSED  (0x04),
        UVC_VS_FRAME_UNCOMPRESSED  (0x05),
        UVC_VS_FORMAT_MJPEG  (0x06),
        UVC_VS_FRAME_MJPEG  (0x07),
        UVC_VS_FORMAT_MPEG2TS  (0x0a),
        UVC_VS_FORMAT_DV  (0x0c),
        UVC_VS_COLORFORMAT  (0x0d),
        UVC_VS_FORMAT_FRAME_BASED  (0x10),
        UVC_VS_FRAME_FRAME_BASED  (0x11),
        UVC_VS_FORMAT_STREAM_BASED  (0x12),;
        private final int value;
        uvc_vs_desc_subtype(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }

    /**
     * Output terminal type (B.3)
     */
    public enum uvc_ot_type {
        UVC_OTT_VENDOR_SPECIFIC  (0x0300),
        UVC_OTT_DISPLAY  (0x0301),
        UVC_OTT_MEDIA_TRANSPORT_OUTPUT  (0x0302),;
        private final int value;
        uvc_ot_type(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }




    ///////////////////  UVC Structures


    //Global Uvc_Camera Struct
    @FieldOrder({"preview_pointer", "uvc_context", "uvc_device_handle", "uvc_device", "uvc_ctrl"  })
    public static class uvc_camera extends Structure {
        public static class ByReference extends uvc_camera implements Structure.ByReference{}

        public long preview_pointer;

        public Pointer uvc_context;
        public Pointer uvc_device_handle;
        public Pointer uvc_device;
        public Pointer uvc_ctrl;

    }

    @FieldOrder({"spacketCnt", "spacket0Cnt", "spacket12Cnt", "spacketDataCnt", "spacketHdr8Ccnt", "spacketErrorCnt", "sframeCnt", "sframeLen", "requestCnt", "sframeLenArray"})
    public static class Libusb_Auto_Values extends Structure {
        public static class ByValue extends Libusb_Auto_Values implements Structure.ByValue {
        }

        public int spacketCnt;
        public int spacket0Cnt;
        public int spacket12Cnt;
        public int spacketDataCnt;
        public int spacketHdr8Ccnt;
        public int spacketErrorCnt;
        public int sframeCnt;
        public int sframeLen;
        public int requestCnt;
        public int[] sframeLenArray = new int[5];
    }


    @FieldOrder({"config", "ctrl_if", "stream_ifs"})
    public static class uvc_device_info extends Structure {
        public static class ByReference extends uvc_device_info implements Structure.ByReference{}
        public static class ByValue extends uvc_device_info implements Structure.ByValue{}
        public libusb_config_descriptor.ByReference config;
        public uvc_control_interface.ByValue ctrl_if;
        public uvc_streaming_interface.ByReference stream_ifs;
    }

    @FieldOrder({"bLength", "bDescriptorType", "wTotalLength", "bNumInterfaces", "bConfigurationValue", "iConfiguration",
            "bmAttributes", "MaxPower", "interFace", "extra", "extra_length"})
    public static class libusb_config_descriptor extends Structure {
        public static class ByReference extends libusb_config_descriptor implements Structure.ByReference { }
        public static class ByValue extends libusb_config_descriptor implements Structure.ByValue { }
        public byte bLength;
        public byte bDescriptorType;
        public short wTotalLength;
        /** Number of interfaces supported by this configuration */
        public byte bNumInterfaces;
        public byte bConfigurationValue;
        public byte iConfiguration;
        public byte bmAttributes;
        public byte MaxPower;
        /** Array of interfaces supported by this configuration. The length of
         * this array is determined by the bNumInterfaces field. */
        public libusb_interface.ByReference interFace;
        public String extra;
        public int extra_length;
    }

    @FieldOrder({"altsetting", "num_altsetting"})
    public static class libusb_interface extends Structure {
        public static class ByReference extends libusb_interface implements Structure.ByReference {  }
        public static class ByValue extends libusb_interface implements Structure.ByValue {}
        public libusb_interface_descriptor.ByReference altsetting;
        public int num_altsetting;
    }

    @FieldOrder({"bLength", "bDescriptorType", "bInterfaceNumber", "bAlternateSetting", "bNumEndpoints", "bInterfaceClass", "bInterfaceSubClass",
            "bInterfaceProtocol", "iInterface", "endpoint", "extra", "extra_length"})
    public static class libusb_interface_descriptor extends Structure {
        public static class ByReference extends libusb_interface_descriptor implements Structure.ByReference { }
        public static class ByValue extends libusb_interface_descriptor implements Structure.ByValue {}
        public byte bLength;
        public byte bDescriptorType;
        public byte bInterfaceNumber;
        public byte bAlternateSetting;
        public byte bNumEndpoints;
        public byte bInterfaceClass;
        public byte bInterfaceSubClass;
        public byte bInterfaceProtocol;
        public byte iInterface;
        public libusb_endpoint_descriptor.ByReference endpoint;
        public String extra;
        public int extra_length;
    }

    @FieldOrder({"bLength", "bDescriptorType", "bEndpointAddress", "bmAttributes", "wMaxPacketSize", "bInterval",
            "bRefresh", "bSynchAddress", "extra", "extra_length"})
    public static class libusb_endpoint_descriptor extends Structure {
        public static class ByReference extends libusb_endpoint_descriptor implements Structure.ByReference {        }
        public byte bLength;
        public byte bDescriptorType;
        public byte bEndpointAddress;
        public byte bmAttributes;
        public short wMaxPacketSize;
        public byte bInterval;
        public byte bRefresh;
        public byte bSynchAddress;
        public String extra;
        public int extra_length;
    }

    /**
     * VideoControl interface
     */
    @FieldOrder({"parent", "input_term_descs", "output_term_descs", "processing_unit_descs", "extension_unit_descs",
            "bcdUVC", "bEndpointAddress", "bInterfaceNumber"})
    public static class uvc_control_interface extends Structure {
        public static class ByReference extends uvc_control_interface implements Structure.ByReference {        }
        public static class ByValue extends uvc_control_interface implements Structure.ByValue {        }
        public uvc_device_info.ByReference parent;
        public uvc_input_terminal.ByReference input_term_descs;
        public uvc_output_terminal.ByReference output_term_descs;
        public uvc_processing_unit.ByReference processing_unit_descs;
        public uvc_extension_unit.ByReference extension_unit_descs;
        public short bcdUVC;
        public byte bEndpointAddress;
        /**
         * Interface number
         */
        public byte bInterfaceNumber;
    }

    @FieldOrder({"prev", "next", "bTerminalID", "wTerminalType", "wObjectiveFocalLengthMin",
            "wObjectiveFocalLengthMax", "wOcularFocalLength", "bmControls", "request"})
    public static class uvc_input_terminal extends Structure {
        public static class ByReference extends uvc_input_terminal implements Structure.ByReference {        }
        public uvc_input_terminal.ByReference prev, next;
        /**
         * Index of the terminal within the device
         */
        public byte bTerminalID;
        /**
         * Type of terminal (e.g., camera)
         */
        public int wTerminalType;
        public short wObjectiveFocalLengthMin;
        public short wObjectiveFocalLengthMax;
        public short wOcularFocalLength;
        /**
         * Camera controls (meaning of bits given in {uvc_ct_ctrl_selector})
         */
        public long bmControls;
        /**
         * request code(wIndex)
         */
        public short request;
    }
    /**
     * Representation of the interface that brings data into the UVC device
     */
    @FieldOrder({"prev", "next", "bTerminalID", "wTerminalType",
            "bAssocTerminal", "bSourceID", "iTerminal", "request"})
    public static class uvc_output_terminal extends Structure {
        public static class ByReference extends uvc_output_terminal implements Structure.ByReference {        }
        uvc_output_terminal.ByReference prev, next;
        /** @todo */
        /**
         * Index of the terminal within the device
         */
        public byte bTerminalID;
        /**
         * Type of terminal (e.g., camera)
         */
        public int wTerminalType;
        public short bAssocTerminal;
        public byte bSourceID;
        public byte iTerminal;
        /**
         * request code(wIndex)
         */
        public short request;
    }

    /**
     * Representation of the interface that brings data into the UVC device
     */
    @FieldOrder({"prev", "next", "bUnitID", "bSourceID", "bmControls", "request"})
    public static class uvc_processing_unit extends Structure {
        public static class ByReference extends uvc_processing_unit implements Structure.ByReference {        }
        public uvc_processing_unit.ByReference prev, next;
        /**
         * Index of the processing unit within the device
         */
        public byte bUnitID;
        /**
         * Index of the terminal from which the device accepts images
         */
        public byte bSourceID;
        /**
         * Processing controls (meaning of bits given in {uvc_pu_ctrl_selector})
         */
        public long bmControls;
        /**
         * request code(wIndex)
         */
        public short request;
    }

    /**
     * Representation of the interface that brings data into the UVC device
     */
    @FieldOrder({"prev", "next", "bUnitID", "guidExtensionCode", "bmControls", "request"})
    public static class uvc_extension_unit extends Structure {
        public static class ByReference extends uvc_extension_unit implements Structure.ByReference {        }
        public uvc_extension_unit.ByReference prev, next;
        /**
         * Index of the extension unit within the device
         */
        public byte bUnitID;
        /**
         * GUID identifying the extension unit
         */
        public byte[] guidExtensionCode = new byte[16];
        /**
         * Bitmap of available controls (manufacturer-dependent)
         */
        public long bmControls;
        /**
         * request code(wIndex)
         */
        public short request;
    }

    @FieldOrder({"parent", "prev", "next", "bInterfaceNumber", "format_descs",
            "bEndpointAddress", "bTerminalLink", "bmInfo", "bStillCaptureMethod", "bTriggerSupport", "bTriggerUsage", "bmaControls"})
    public static class uvc_streaming_interface extends Structure {
        public static class ByReference extends uvc_streaming_interface implements Structure.ByReference {        }
        public static class ByValue extends uvc_streaming_interface implements Structure.ByValue {        }
        public uvc_device_info.ByReference parent;
        public uvc_streaming_interface.ByReference prev, next;
        /**
         * Interface number
         */
        public byte bInterfaceNumber;
        /**
         * Video formats that this interface provides
         */
        public uvc_format_desc.ByReference format_descs;
        /**
         * USB endpoint to use when communicating with this interface
         */
        public byte bEndpointAddress;
        public byte bTerminalLink;
        public byte bmInfo;    // XXX
        public byte bStillCaptureMethod;    // XXX
        public byte bTriggerSupport;    // XXX
        public byte bTriggerUsage;    // XXX
        public long bmaControls;    // XXX
    }

    @FieldOrder({"parent", "prev", "next", "bDescriptorSubtype", "bFormatIndex", "bNumFrameDescriptors",
            "formatSpecifier", "formatSpecificData",
            "bDefaultFrameIndex", "bAspectRatioX", "bAspectRatioY",
            "bmInterlaceFlags", "bCopyProtect", "bVariableSize", "frame_descs"})
    public static class uvc_format_desc extends Structure {
        public static class ByReference extends uvc_format_desc implements Structure.ByReference {        }
        public static class ByValue extends uvc_format_desc implements Structure.ByValue {        }

        public uvc_streaming_interface.ByReference parent;
        public uvc_format_desc.ByReference prev;
        public uvc_format_desc.ByReference next;
        public int bDescriptorSubtype;
        public byte bFormatIndex;
        public byte bNumFrameDescriptors;

        public FormatSpecifier formatSpecifier;
        public FormatSpecificData formatSpecificData;

        public byte bDefaultFrameIndex;
        public byte bAspectRatioX;
        public byte bAspectRatioY;
        public byte bmInterlaceFlags;
        public byte bCopyProtect;
        public byte bVariableSize;

        public uvc_frame_desc.ByReference frame_descs;
    }

    public static class FormatSpecifier extends Union {
        public byte[] guidFormat = new byte[16];
        public byte[] fourccFormat = new byte[4];
    }

    /**
     * Format-specific data
     */

    public static  class FormatSpecificData extends Union {
        /**
         * BPP for uncompressed stream
         */
        public byte bBitsPerPixel;
        /**
         * Flags for JPEG stream
         */
        public byte bmFlags;
    }

    @FieldOrder({"parent", "prev", "next", "bDescriptorSubtype", "bFrameIndex", "bmCapabilities",
            "wWidth", "wHeight", "dwMinBitRate", "dwMaxBitRate", "dwMaxVideoFrameBufferSize", "dwDefaultFrameInterval",
            "dwMinFrameInterval", "dwMaxFrameInterval", "dwFrameIntervalStep", "bFrameIntervalType", "dwBytesPerLine", "intervals"})
    public static class uvc_frame_desc extends Structure {
        public static class ByReference extends uvc_frame_desc implements Structure.ByReference {        }
        public static class ByValue extends uvc_frame_desc implements Structure.ByValue {        }
        public uvc_format_desc.ByReference parent;
        public uvc_frame_desc.ByReference prev, next;
        /**
         * Type of frame, such as JPEG frame or uncompressed frme
         */
        public int bDescriptorSubtype;
        /**
         * Index of the frame within the list of specs available for this format
         */
        public byte bFrameIndex;
        public byte bmCapabilities;
        /**
         * Image width
         */
        public short wWidth;
        /**
         * Image height
         */
        public short wHeight;
        /**
         * Bitrate of corresponding stream at minimal frame rate
         */
        public int dwMinBitRate;
        /**
         * Bitrate of corresponding stream at maximal frame rate
         */
        public int dwMaxBitRate;
        /**
         * Maximum number of bytes for a video frame
         */
        public int dwMaxVideoFrameBufferSize;
        /**
         * Default frame interval (in 100ns units)
         */
        public int dwDefaultFrameInterval;
        /**
         * Minimum frame interval for continuous mode (100ns units)
         */
        public int dwMinFrameInterval;
        /**
         * Maximum frame interval for continuous mode (100ns units)
         */
        public int dwMaxFrameInterval;
        /**
         * Granularity of frame interval range for continuous mode (100ns)
         */
        public int dwFrameIntervalStep;
        /**
         * Frame intervals
         */
        public byte bFrameIntervalType;
        /**
         * number of bytes per line
         */
        public int dwBytesPerLine;
        /**
         * Available frame rates, zero-terminated (in 100ns units)
         */
        public Pointer intervals;
    }

    @FieldOrder({"bmHint", "bFormatIndex", "bFrameIndex", "dwFrameInterval", "wKeyFrameRate", "wPFrameRate",
            "wCompQuality", "wCompWindowSize", "wDelay", "dwMaxVideoFrameSize", "dwMaxPayloadTransferSize", "dwClockFrequency",
            "bmFramingInfo", "bPreferedVersion", "bMinVersion", "bMaxVersion",
            "bUsage", "bBitDepthLuma", "bmSettings", "bMaxNumberOfRefFramesPlus1", "bmRateControlModes", "bmLayoutPerStream", "bInterfaceNumber"})
    public static class uvc_stream_ctrl extends Structure {
        public static class ByReference extends uvc_stream_ctrl implements Structure.ByReference {        }
        public static class ByValue extends uvc_stream_ctrl implements Structure.ByValue {        }
        public short bmHint;
        public byte bFormatIndex;
        public byte bFrameIndex;
        public int dwFrameInterval;
        public short wKeyFrameRate;
        public short wPFrameRate;
        public short wCompQuality;
        public short wCompWindowSize;
        public short wDelay;
        public int dwMaxVideoFrameSize;
        public int dwMaxPayloadTransferSize;
        /** XXX add UVC 1.1 parameters */
        public int dwClockFrequency;
        public byte bmFramingInfo;
        public byte bPreferedVersion;
        public byte bMinVersion;
        public byte bMaxVersion;
        /** XXX add UVC 1.5 parameters */
        public byte bUsage;
        public byte bBitDepthLuma;
        public byte bmSettings;
        public byte bMaxNumberOfRefFramesPlus1;
        public short bmRateControlModes;
        public long bmLayoutPerStream;
        //
        public byte bInterfaceNumber;
    }

    public static class size_t extends IntegerType {
        public static final size_t ZERO = new size_t();
        private static final long serialVersionUID = 1L;
        public size_t() { this(0); }
        public size_t(long value) { super(Native.SIZE_T_SIZE, value, true); }
    }

    /** An image frame received from the UVC device
     * @ingroup streaming
     */
    @FieldOrder({"data", "data_bytes", "actual_bytes", "width", "height", "frame_format",
            "step", "sequence", "capture_time", "source", "library_owns_data"})
    public static class uvc_frame extends Structure {
        public static class ByReference extends uvc_frame implements Structure.ByReference {        }
        public static class ByValue extends uvc_frame implements Structure.ByValue {        }
        public Pointer data;
        public size_t data_bytes;
        public size_t actual_bytes;
        public int width;
        public int height;
        public int frame_format;

        public size_t step;
        public int sequence;
        public timeval capture_time;
        /** Handle on the device that produced the image.
         * @warning You must not call any uvc_* functions during a callback. */
        public Pointer source;
        public byte library_owns_data;
    }
    @FieldOrder({"tv_sec", "tv_usec"})
    public static class timeval extends Structure {
        /**
         * Seconds.<br>
         * C type : __time_t
         */
        public int tv_sec;
        /**
         * Microseconds.<br>
         * C type : __suseconds_t
         */
        public int tv_usec;
        public timeval() {
            super();
        }
        public timeval(int tv_sec, int tv_usec) {
            super();
            this.tv_sec = tv_sec;
            this.tv_usec = tv_usec;
        }
        protected ByReference newByReference() {
            ByReference s = new ByReference();
            s.useMemory(getPointer());
            write();
            s.read();
            return s;
        }
        protected ByValue newByValue() {
            ByValue s = new ByValue();
            s.useMemory(getPointer());
            write();
            s.read();
            return s;
        }
        protected timeval newInstance() {
            timeval s = new timeval();
            s.useMemory(getPointer());
            write();
            s.read();
            return s;
        }
        public static class ByReference extends timeval implements com.sun.jna.Structure.ByReference {}
        public static class ByValue extends timeval implements com.sun.jna.Structure.ByValue {}
    }

    @FieldOrder({"maxPacketSize", "altsetting", "packetsPerRequest", "activeUrbs"})
    public static class auto_detect_struct extends Structure {
        public static class ByReference extends auto_detect_struct implements com.sun.jna.Structure.ByReference {}
        public size_t maxPacketSize;
        public size_t altsetting;
        public size_t packetsPerRequest;
        public size_t activeUrbs;
    }

}