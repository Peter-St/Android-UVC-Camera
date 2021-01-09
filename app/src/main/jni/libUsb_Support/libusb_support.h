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

#ifndef libusb_support_h__
#define libusb_support_h__
#include </home/peter/Android/Sdk/ndk/21.1.6352462/sysroot/usr/include/android/native_window.h>
#include </home/peter/Android/Sdk/ndk/21.1.6352462/sysroot/usr/include/jni.h>
//////////////// Auto Detect Methods ////////////////////
struct AutotransferStruct; /* Forward declaration */
typedef struct AutotransferStruct {
    int packetCnt;
    int packet0Cnt;
    int packet12Cnt;
    int packetDataCnt;
    int packetHdr8Ccnt;
    int packetErrorCnt;
    int frameCnt;
    int frameLen;
    int requestCnt;
    int sframeLenArray[5];
} AutotransferStruct;

extern AutotransferStruct get_autotransferStruct();

////////////////////// TEST 1 FRAME METHODS //////////////////////////

struct ManualTestFrameStruct; /* Forward declaration */
typedef struct ManualTestFrameStruct {
    int packetCnt;
    int packet0Cnt;
    int packet12Cnt;
    int packetDataCnt;
    int packetHdr8Ccnt;
    int packetErrorCnt;
    int frameCnt;
    int frameLen;
    int requestCnt;
    int sframeLenArray[5];
} ManualTestFrameStruct;

extern ManualTestFrameStruct get_ManualTestFrameStruct();

//////////////// Global Methods ////////////////////

//   Camera Initialization Methods
extern int set_the_native_Values (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                 int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpointAdress, int camStreamingInterfaceNumber,
                 const char* frameformat, int numberOfAutoFrame, int bcdUVC_int);
extern int initStreamingParms(int FD);
extern void stopStreaming();
extern void stopJavaVM();
extern void exit();
extern void closeLibUsb();
extern int libUsb_open_def_fd(int vid, int pid, const char *serial, int fd, int busnum, int devaddr);
extern unsigned char * probeCommitControl();



// Camera Set up Methods
extern void startAutoDetection ();
typedef int ( *autoStreamComplete)();
extern void setAutoStreamComplete(autoStreamComplete autoStream);
extern void getFramesOverLibUsb(int yuvFrameIsZero, int stream, int whichTestrun);
extern int awaitFrame () ;
typedef int ( *eventCallback)(unsigned char *videoframe, int value);
extern void setCallback(eventCallback evnHnd);
typedef int ( *jnaFrameCallback)(void  *videoframe, int value);
extern void setJnaFrameCallback(jnaFrameCallback evnHnd);
extern int eheckEventHandling();
extern unsigned char * probeCommitControl(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva, int FD);
extern void probeCommitControl_cleanup();
extern void sendCtlForConnection(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva);




////////// Activity Strteam
extern int setPreviewDisplay(ANativeWindow *preview_window);
// horizontalFlip == 1) horizontalFlip = true;
extern void setRotation(int rot, int horizontalFl, int verticalFl);
extern int fetchTheCamStreamingEndpointAdress (int FD);
extern void setImageCapture();
extern void setImageCaptureLongClick();
extern void startVideoCapture();
extern void stopVideoCapture();
extern void startVideoCaptureLongClick() ;
extern void stopVideoCaptureLongClick() ;

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivitySurface
        (JNIEnv *, jobject, jobject, jint, jint);
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivity
        (JNIEnv *, jobject, jint, jint);
// Streaming Method
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetSurfaceView
        (JNIEnv *, jobject, jobject);
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetSurfaceYuv
        (JNIEnv *, jobject, jobject);

///////////////   Stream Service
JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniServiceOverSurface
        (JNIEnv *, jobject);
JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniGetAnotherFrame
        (JNIEnv *, jobject);
JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniPrepairForStreamingfromService
        (JNIEnv *, jobject);



////////    SetUpTheDevice
JNIEXPORT void JNICALL Java_humer_UvcCamera_SetUpTheUsbDevice_JniIsoStreamActivity
        (JNIEnv *, jobject, jobject, jint, jint);


////////// WebRTC
extern void prepairTheStream_WebRtc_Service();
extern void lunchTheStream_WebRtc_Service();
JNIEXPORT void JNICALL Java_com_example_androidthings_videortc_UsbCapturer_JniWebRtcJavaMethods
        (JNIEnv *, jobject);

////// YUV Methods
#define align_buffer_64(var, size)                                           \
  uint8_t* var##_mem = (uint8_t*)(malloc((size) + 63));         /* NOLINT */ \
  uint8_t* var = (uint8_t*)(((intptr_t)(var##_mem) + 63) & ~63) /* NOLINT */

#define free_aligned_buffer_64(var) \
  free(var##_mem);                  \
  var = 0


// USB codes:
// Request types (bmRequestType):

#define  RT_STANDARD_INTERFACE_SET = 0x01
#define  RT_CLASS_INTERFACE_SET     0x21
#define  RT_CLASS_INTERFACE_GET     0xA1
// Video interface subclass codes:
#define  SC_VIDEOCONTROL            0x01
#define  SC_VIDEOSTREAMING          0x02
#define CLASS_VIDEO                 0x14
// Standard request codes:
#define  SET_INTERFACE              0x0b
// Video class-specific request codes:
#define  SET_CUR                     0x01
#define  GET_CUR                     0x81
// VideoControl interface control selectors (CS):
#define  VC_REQUEST_ERROR_CODE_CONTROL  0x02
// VideoStreaming interface control selectors (CS):
#define  VS_PROBE_CONTROL              0x01
#define  VS_COMMIT_CONTROL             0x02
#define  VS_STILL_PROBE_CONTROL        0x03
#define  VS_STILL_COMMIT_CONTROL       0x04
#define  VS_STREAM_ERROR_CODE_CONTROL  0x06
#define  VS_STILL_IMAGE_TRIGGER_CONTROL  0x05

#define PIXEL_RGB565		2
#define PIXEL_UYVY			2
#define PIXEL_YUYV			2
#define PIXEL_RGB			3
#define PIXEL_BGR			3
#define PIXEL_RGBX			4

#define PIXEL8_YUYV			PIXEL_YUYV * 8
#define PIXEL8_RGBX			PIXEL_RGBX * 8


#define PIXEL2_RGB565		PIXEL_RGB565 * 2
#define PIXEL2_UYVY			PIXEL_UYVY * 2
#define PIXEL2_YUYV			PIXEL_YUYV * 2
#define PIXEL2_RGB			PIXEL_RGB * 2
#define PIXEL2_BGR			PIXEL_BGR * 2
#define PIXEL2_RGBX			PIXEL_RGBX * 2

#define PIXEL4_RGB565		PIXEL_RGB565 * 4
#define PIXEL4_UYVY			PIXEL_UYVY * 4
#define PIXEL4_YUYV			PIXEL_YUYV * 4
#define PIXEL4_RGB			PIXEL_RGB * 4
#define PIXEL4_BGR			PIXEL_BGR * 4
#define PIXEL4_RGBX			PIXEL_RGBX * 4

#define PIXEL8_RGB565		PIXEL_RGB565 * 8
#define PIXEL8_UYVY			PIXEL_UYVY * 8
#define PIXEL8_YUYV			PIXEL_YUYV * 8
#define PIXEL8_RGB			PIXEL_RGB * 8
#define PIXEL8_BGR			PIXEL_BGR * 8
#define PIXEL8_RGBX			PIXEL_RGBX * 8

#define PIXEL16_RGB565		PIXEL_RGB565 * 16
#define PIXEL16_UYVY		PIXEL_UYVY * 16
#define PIXEL16_YUYV		PIXEL_YUYV * 16
#define PIXEL16_RGB			PIXEL_RGB * 16
#define PIXEL16_BGR			PIXEL_BGR * 16
#define PIXEL16_RGBX		PIXEL_RGBX * 16

#define	LOCAL_DEBUG 0
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME + 2

#define  LOG_TAG    "From Native"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define UVC_STREAM_EOH (1 << 7)
#define UVC_STREAM_ERR (1 << 6)
#define UVC_STREAM_STI (1 << 5)
#define UVC_STREAM_RES (1 << 4)
#define UVC_STREAM_SCR (1 << 3)
#define UVC_STREAM_PTS (1 << 2)
#define UVC_STREAM_EOF (1 << 1)
#define UVC_STREAM_FID (1 << 0)
#define TAG "LibUsb"


#define nullptr ((void*)0)

  #endif  // iso_h__


