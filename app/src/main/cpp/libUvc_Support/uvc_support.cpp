//
// Created by peter on 16.06.22.
//

#include "uvc_support.h"
#include "UVC_Camera_Saki/UVCPreview.h"

#include <jni.h>
#include "libusb.h"
#include "../libuvc/include/libuvc/libuvc.h"
//#include "libuvc.h"
#include "../libuvc/include/utilbase.h"
//#include "utilbase.h"


/**
 * set the value into the long field
 * @param env: this param should not be null
 * @param bullet_obj: this param should not be null
 * @param field_name
 * @params val
 */
static jlong setField_long(JNIEnv *env, jobject java_obj, const char *field_name, jlong val) {
#if LOCAL_DEBUG
    LOGV("setField_long:");
#endif

    jclass clazz = env->GetObjectClass(java_obj);
    jfieldID field = env->GetFieldID(clazz, field_name, "J");
    if (LIKELY(field))
        env->SetLongField(java_obj, field, val);
    else {
        LOGE("__setField_long:field '%s' not found", field_name);
    }
#ifdef ANDROID_NDK
    env->DeleteLocalRef(clazz);
#endif
    return val;
}



// write native struct
extern "C" JNIEXPORT long JNICALL Java_humer_UvcCamera_Main_nativeCreate
        (JNIEnv *env, jobject obj, ID_TYPE camera_pointer) {

    uvc_camera_t *camera = new uvc_camera_t();

    setField_long(env, obj, "mNativePtr", reinterpret_cast<ID_TYPE>(camera));

    camera_pointer = reinterpret_cast<ID_TYPE>(camera) ;

    return reinterpret_cast<ID_TYPE>(camera);
}

// read native struct
extern "C" JNIEXPORT void JNICALL Java_humer_UvcCamera_Main_readNativeStruct
        (JNIEnv *env, jobject obj, ID_TYPE camera_pointer) {

    uvc_camera_t *camera = reinterpret_cast<uvc_camera_t *>(camera_pointer);

    LOGD("From C++\ncamera->preview_pointer = %d\n", camera->preview_pointer);

}

/////////////////////////////////////////// Stream ///////////////////////////////7


extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_PreviewPrepareStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr, jobject jSurface, jobject jIFrameCallback) {

    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;


    LOGD("camera_pointer->imageWidth = %d", camera_pointer->imageWidth);


    ANativeWindow *preview_window;
    if (jSurface != NULL ) {
        preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
        // WINDOW_FORMAT_RGBA_8888
        //ANativeWindow_setBuffersGeometry(preview_window, camera_pointer->imageWidth, camera_pointer->imageHeight, WINDOW_FORMAT_RGBA_8888);
        LOGD("mCaptureWindow, JniSetSurfaceView");
    }

    camera_pointer->preview_pointer = create_UVCPreview(camera_pointer->camera_deviceHandle, camera_pointer->preview_pointer);
    int result = -1;
    if(set_preview_size_random(camera_pointer->preview_pointer,  camera_pointer->imageWidth, camera_pointer->imageHeight, 0, 30, 0,
                               camera_pointer->activeUrbs, camera_pointer->packetsPerRequest, camera_pointer->camStreamingAltSetting, camera_pointer->maxPacketSize,
                               camera_pointer->camFormatIndex, camera_pointer->camFrameIndex, camera_pointer->camFrameInterval, camera_pointer->frameFormat,
                               camera_pointer->imageWidth, camera_pointer->imageHeight, 0 /* stream bit set */) == 0) {
        result = set_preview_display(camera_pointer->preview_pointer, preview_window);
    }
    LOGD("Setting Frame Callback");

    jobject frame_callback_obj = env->NewGlobalRef(jIFrameCallback);

    int pixel_format;
    if (strcmp(camera_pointer->frameFormat, "MJPEG") == 0) pixel_format = 6;
    else if (strcmp(camera_pointer->frameFormat, "YUY2") == 0 ||   strcmp(camera_pointer->frameFormat, "UYVY") == 0 ) pixel_format = pixel_format;
    else if (strcmp(camera_pointer->frameFormat, "NV21") == 0) pixel_format = 5;
    else pixel_format = 0;

    LOGD ("Pixel Format = %d", pixel_format);

    // PIXEL_FORMAT defined in UVCPreview.h   0 = raw (yuy2) | 1 = yuv (yuy2) | 2-3 = rgb | 5 = NV21 | 6 = MJPEG
    result = setFrameCallback(camera_pointer->preview_pointer, env, frame_callback_obj, pixel_format);
    LOGD("setFrameCallback returned: %d", result);


    result = setJavaVM(camera_pointer->preview_pointer, env, obj);

    return result;
}


extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_PreviewStartStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = startPreview(camera_pointer->preview_pointer);
    return result;
}

extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_PreviewStopStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = stopPreview(camera_pointer->preview_pointer);
    return result;
}

extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_PreviewCapturePicture
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = capturePicture(camera_pointer->preview_pointer);
    return result;
}

/////////////////////////////////////////// Testrun ///////////////////////////////7

extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_SetUpTheUsbDeviceUvc_PreviewPrepareTest
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr,  jobject jIFrameCallback) {
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    LOGD("camera_pointer->imageWidth = %d", camera_pointer->imageWidth);
    camera_pointer->preview_pointer = create_UVCPreview(camera_pointer->camera_deviceHandle, camera_pointer->preview_pointer);
    int result = -1;
    if(set_preview_size_random(camera_pointer->preview_pointer,  camera_pointer->imageWidth, camera_pointer->imageHeight, 0,
                               30, 0,camera_pointer->activeUrbs, camera_pointer->packetsPerRequest,
                               camera_pointer->camStreamingAltSetting, camera_pointer->maxPacketSize,camera_pointer->camFormatIndex,
                               camera_pointer->camFrameIndex, camera_pointer->camFrameInterval, camera_pointer->frameFormat,
                               camera_pointer->imageWidth, camera_pointer->imageHeight, 1 /* test bit set */) == 0) {
        //result = set_preview_display(camera_pointer->preview_pointer, preview_window);
        LOGD("Setting Frame Callback");
        jobject frame_callback_obj = env->NewGlobalRef(jIFrameCallback);

        int pixel_format;
        if (strcmp(camera_pointer->frameFormat, "MJPEG") == 0) pixel_format = 6;
        else if (strcmp(camera_pointer->frameFormat, "YUY2") == 0 ||   strcmp(camera_pointer->frameFormat, "UYVY") == 0 ) pixel_format = 1;
        else if (strcmp(camera_pointer->frameFormat, "NV21") == 0) pixel_format = 5;
        else pixel_format = 0;
        LOGD ("Pixel Format = %d", pixel_format);

        // PIXEL_FORMAT defined in UVCPreview.h   0 = raw (yuy2) | 1 = yuv (yuy2) | 2-3 = rgb | 5 = NV21 | 6 = MJPEG
        result = setFrameCallback(camera_pointer->preview_pointer, env, frame_callback_obj, pixel_format);
        LOGD("setFrameCallback returned: %d", result);

        result = setJavaVM(camera_pointer->preview_pointer, env, obj);
        LOGD("setJavaVM returned: %d", result);
    }
    return result;
}
extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_SetUpTheUsbDeviceUvc_PreviewStartTest
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = startPreview(camera_pointer->preview_pointer);
    if (result == 0) enableMIsCapturing(camera_pointer->preview_pointer);
    return result;
}

extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_SetUpTheUsbDeviceUvc_PreviewStopTest
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    LOGD("stopping the preview");
    result = stopPreview(camera_pointer->preview_pointer);
    return result;
}

/////////////////////////////////////////// WebRTC ///////////////////////////////



extern "C" JNIEXPORT jint JNICALL Java_com_example_androidthings_videortc_UsbCapturer_NativePrepareStreamWebRTC
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr, jobject jIFrameCallback) {

    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;

    LOGD("camera_pointer->imageWidth = %d", camera_pointer->imageWidth);


    camera_pointer->preview_pointer = create_UVCPreview(camera_pointer->camera_deviceHandle, camera_pointer->preview_pointer);
    int result = -1;
    if(set_preview_size_random(camera_pointer->preview_pointer,  camera_pointer->imageWidth, camera_pointer->imageHeight, 0, 30, 0,
                               camera_pointer->activeUrbs, camera_pointer->packetsPerRequest, camera_pointer->camStreamingAltSetting, camera_pointer->maxPacketSize,
                               camera_pointer->camFormatIndex, camera_pointer->camFrameIndex, camera_pointer->camFrameInterval, camera_pointer->frameFormat,
                               camera_pointer->imageWidth, camera_pointer->imageHeight, 0 /* stream bit set */) == 0) {
    }
    LOGD("Setting Frame Callback");

    jobject frame_callback_obj = env->NewGlobalRef(jIFrameCallback);


    //////////////////////////////////////////////

    //   FORCE PIXEL NV21                      //

    /////////////////////////////////////////////

    int pixel_format;
    /*
    if (strcmp(camera_pointer->frameFormat, "MJPEG") == 0) pixel_format = 6;
    else if (strcmp(camera_pointer->frameFormat, "YUY2") == 0 ||   strcmp(camera_pointer->frameFormat, "UYVY") == 0 ) pixel_format = pixel_format;
    else if (strcmp(camera_pointer->frameFormat, "NV21") == 0) pixel_format = 5;
    else pixel_format = 0;
    */
    pixel_format = 5;

    LOGD ("Pixel Format = %d", pixel_format);

    // PIXEL_FORMAT defined in UVCPreview.h   0 = raw (yuy2) | 1 = yuv (yuy2) | 2-3 = rgb | 5 = NV21 | 6 = MJPEG
    result = setFrameCallback(camera_pointer->preview_pointer, env, frame_callback_obj, pixel_format);
    LOGD("setFrameCallback returned: %d", result);


    result = setJavaVM(camera_pointer->preview_pointer, env, obj);

    return result;
}


extern "C" JNIEXPORT jint JNICALL Java_com_example_androidthings_videortc_UsbCapturer_NativeStartStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = startPreview(camera_pointer->preview_pointer);
    if (result == 0) enableMIsCapturing(camera_pointer->preview_pointer);
    return result;
}

extern "C" JNIEXPORT jint JNICALL Java_com_example_androidthings_videortc_UsbCapturer_NativeStopStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr) {
    int result = -1;
    uvc_camera_t *camera_pointer = reinterpret_cast<uvc_camera_t *>(mNativePtr) ;
    result = stopPreview(camera_pointer->preview_pointer);
    return result;
}

