#ifndef libusb_support_h__
#define libusb_support_h__

#include </home/peter/Android/Sdk/ndk/21.1.6352462/sysroot/usr/include/android/native_window.h>
#include </home/peter/Android/Sdk/ndk/21.1.6352462/sysroot/usr/include/jni.h>



extern int init (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                 int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpointAdress, int camStreamingInterfaceNumber,
                 const char* frameformat);





extern int libUsb_open_def_fd(int vid, int pid, const char *serial, int fd, int busnum, int devaddr);

extern void getFramesOverLibUsb(int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                                int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int yuvFrameIsZero, int stream );


extern int awaitFrame () ;

typedef int ( *eventCallback)(unsigned char *videoframe, int value);
extern void setCallback(eventCallback evnHnd);

typedef int ( *logPrint)(const char *log_msg);
extern void setLogPrint(logPrint LogPrint);



extern void stopStreaming();

extern void exit();

extern void closeLibUsb();

extern unsigned char * probeCommitControl();

extern int eheckEventHandling();


extern unsigned char * probeCommitControl(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva);

extern void probeCommitControl_cleanup();

extern void sendCtlForConnection(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva);

extern int setPreviewDisplay(ANativeWindow *preview_window);

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivitySurface
        (JNIEnv *, jobject, jobject, jint, jint);

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivity
        (JNIEnv *, jobject, jint, jint);

JNIEXPORT void JNICALL Java_humer_UvcCamera_SetUpTheUsbDevice_JniIsoStreamActivity
        (JNIEnv *, jobject, jobject, jint, jint);

JNIEXPORT unsigned char * JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniProbeCommitControl
        (JNIEnv *, jobject, jint, jint, jint, jint);

JNIEXPORT void JNICALL Java_com_example_androidthings_videortc_UsbCapturer_JniWebRtc
        (JNIEnv *, jobject, jint, jint);


#endif  // iso_h__



