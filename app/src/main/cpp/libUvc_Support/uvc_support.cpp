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



extern "C" JNIEXPORT jint JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_PreviewPrepareStream
        (JNIEnv *env, jobject obj, ID_TYPE mNativePtr, jobject jSurface) {

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
                               camera_pointer->imageWidth, camera_pointer->imageHeight) == 0) {
        result = set_preview_display(camera_pointer->preview_pointer, preview_window);

        //if(set_preview_display(camera_pointer->preview_pointer, preview_window) == 0 ) {
            //result = startPreview(camera_pointer->preview_pointer);
        //}
    }
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