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

extern int set_the_native_Values (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                 int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpointAdress, int camStreamingInterfaceNumber,
                 const char* frameformat, int numberOfAutoFrame, int bcdUVC_int);

extern int initStreamingParms(int FD);

extern void startAutoDetection ();

typedef int ( *autoStreamComplete)();
extern void setAutoStreamComplete(autoStreamComplete autoStream);




extern int libUsb_open_def_fd(int vid, int pid, const char *serial, int fd, int busnum, int devaddr);

extern void getFramesOverLibUsb(int yuvFrameIsZero, int stream, int whichTestrun);

extern int awaitFrame () ;

typedef int ( *eventCallback)(unsigned char *videoframe, int value);
extern void setCallback(eventCallback evnHnd);

typedef int ( *jnaFrameCallback)(void  *videoframe, int value);
extern void setJnaFrameCallback(jnaFrameCallback evnHnd);

extern void stopStreaming();

extern void stopJavaVM();

extern void exit();

extern void closeLibUsb();

extern unsigned char * probeCommitControl();

extern int eheckEventHandling();

extern unsigned char * probeCommitControl(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva, int FD);

extern void probeCommitControl_cleanup();

extern void sendCtlForConnection(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva);

extern int setPreviewDisplay(ANativeWindow *preview_window);

// horizontalFlip == 1) horizontalFlip = true;
extern void setRotation(int rot, int horizontalFl, int verticalFl);



//////////////// JNI Methods ////////////////////



////////// Activity Strteam
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
JNIEXPORT unsigned char * JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniProbeCommitControl
        (JNIEnv *, jobject, jint, jint, jint, jint);
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniPrepairForStreamingfromService
        (JNIEnv *, jobject);
// MJPEG
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetTheMethodsStreamActivityMJPEG
        (JNIEnv *, jobject);
// YUV
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



#endif  // iso_h__


