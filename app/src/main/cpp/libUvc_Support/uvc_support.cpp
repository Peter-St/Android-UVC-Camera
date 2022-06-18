//
// Created by peter on 16.06.22.
//

#include "uvc_support.h"


long preview_pointer;


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



JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_JniIsoStreamActivitySurface
        (JNIEnv *env, jobject obj, jobject jSurface, jint stream, jint frameIndex) {


    /*
    preview_pointer = create_UVCPreview(uvcDeviceHandle_global);
    int result;
    if(set_preview_size_random(preview_pointer,  imageWidth, imageHeight, 0, 30, 0,
                               activeUrbs, packetsPerRequest, camStreamingAltSetting, maxPacketSize,
                               camFormatIndex, camFrameIndex, camFrameInterval) == 0) {
        if(set_preview_display(preview_pointer, mCaptureWindow) == 0 ) {
            startPreview(preview_pointer);
        }
    }
*/

}

