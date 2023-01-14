/*
 * UVCCamera
 * library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 * File name: UVCPreview.cpp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 * Files in the jni/libjpeg, jni/libusb, jin/libuvc, jni/rapidjson folder may have a different license, see the respective files.
*/

#include <stdlib.h>
#include <linux/time.h>
#include <unistd.h>


#if 1	// set 1 if you don't need debug log
	#ifndef LOG_NDEBUG
		#define	LOG_NDEBUG		// w/o LOGV/LOGD/MARK
	#endif
	#undef USE_LOGALL
#else
	#define USE_LOGALL
	#undef LOG_NDEBUG
//	#undef NDEBUG
#endif

#include "utilbase.h"
#include "UVCPreview.h"
#include "libuvc_internal.h"
#include "../uvc_support.h"

#define	LOCAL_DEBUG 0
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME + 2

JavaVM* g_VM;

UVCPreview::UVCPreview(uvc_device_handle_t *devh):
        mPreviewWindow(NULL),
        mCaptureWindow(NULL),
        mDeviceHandle(devh),
        requestWidth(DEFAULT_PREVIEW_WIDTH),
        requestHeight(DEFAULT_PREVIEW_HEIGHT),
        requestMinFps(DEFAULT_PREVIEW_FPS_MIN),
        requestMaxFps(DEFAULT_PREVIEW_FPS_MAX),
        requestMode(DEFAULT_PREVIEW_MODE),
        requestBandwidth(DEFAULT_BANDWIDTH),
        frameWidth(DEFAULT_PREVIEW_WIDTH),
        frameHeight(DEFAULT_PREVIEW_HEIGHT),
        frameBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * 2),	// YUYV
        frameMode(0),
        previewBytes(DEFAULT_PREVIEW_WIDTH * DEFAULT_PREVIEW_HEIGHT * PREVIEW_PIXEL_BYTES),
        previewFormat(WINDOW_FORMAT_RGBA_8888),
        mIsRunning(false),
        mIsCapturing(false),
        mPictureCapturing(false),
        captureQueu(NULL),
        mFrameCallbackObj(NULL),
        mFrameCallbackFunc(NULL),
        callbackPixelBytes(2) {

    ENTER();
    pthread_cond_init(&preview_sync, NULL);
    pthread_mutex_init(&preview_mutex, NULL);
//
    pthread_cond_init(&capture_sync, NULL);
    pthread_mutex_init(&capture_mutex, NULL);
//
    pthread_mutex_init(&pool_mutex, NULL);
    EXIT();
}

UVCPreview::~UVCPreview() {

	ENTER();
	if (mPreviewWindow)
		ANativeWindow_release(mPreviewWindow);
	mPreviewWindow = NULL;
	if (mCaptureWindow)
		ANativeWindow_release(mCaptureWindow);
	mCaptureWindow = NULL;
	clearPreviewFrame();
	clearCaptureFrame();
	clear_pool();
	pthread_mutex_destroy(&preview_mutex);
	pthread_cond_destroy(&preview_sync);
	pthread_mutex_destroy(&capture_mutex);
	pthread_cond_destroy(&capture_sync);
	pthread_mutex_destroy(&pool_mutex);
	EXIT();
}

/**
 * get uvc_frame_t from frame pool
 * if pool is empty, create new frame
 * this function does not confirm the frame size
 * and you may need to confirm the size
 */
uvc_frame_t *UVCPreview::get_frame(size_t data_bytes) {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&pool_mutex);
	{
		if (!mFramePool.isEmpty()) {
			frame = mFramePool.last();
		}
	}
	pthread_mutex_unlock(&pool_mutex);
	if UNLIKELY(!frame) {
		LOGW("allocate new frame");
		frame = uvc_allocate_frame(data_bytes);
	}
	return frame;
}

void UVCPreview::recycle_frame(uvc_frame_t *frame) {
	pthread_mutex_lock(&pool_mutex);
	if (LIKELY(mFramePool.size() < FRAME_POOL_SZ)) {
		mFramePool.put(frame);
		frame = NULL;
	}
	pthread_mutex_unlock(&pool_mutex);
	if (UNLIKELY(frame)) {
		uvc_free_frame(frame);
	}
}


void UVCPreview::init_pool(size_t data_bytes) {
	ENTER();

	clear_pool();
	pthread_mutex_lock(&pool_mutex);
	{
		for (int i = 0; i < FRAME_POOL_SZ; i++) {
			mFramePool.put(uvc_allocate_frame(data_bytes));
		}
	}
	pthread_mutex_unlock(&pool_mutex);

	EXIT();
}

void UVCPreview::clear_pool() {
	ENTER();

	pthread_mutex_lock(&pool_mutex);
	{
		const int n = mFramePool.size();
		for (int i = 0; i < n; i++) {
			uvc_free_frame(mFramePool[i]);
		}
		mFramePool.clear();
	}
	pthread_mutex_unlock(&pool_mutex);
	EXIT();
}

inline const bool UVCPreview::isRunning() const {return mIsRunning; }

int UVCPreview::setPreviewSize(int width, int height, int min_fps, int max_fps, int mode, float bandwidth) {
	ENTER();
	
	int result = 0;
	if ((requestWidth != width) || (requestHeight != height) || (requestMode != mode)) {
		requestWidth = width;
		requestHeight = height;
		requestMinFps = min_fps;
		requestMaxFps = max_fps;
		requestMode = mode;
		requestBandwidth = bandwidth;

		uvc_stream_ctrl_t ctrl;
		result = uvc_get_stream_ctrl_format_size_fps(mDeviceHandle, &ctrl,
			!requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG,
			requestWidth, requestHeight, requestMinFps, requestMaxFps);
	}
	
	RETURN(result, int);
}

/* @Parameter test: 0 = stream | 1 = testrun */
int UVCPreview::setPreviewSize_random(int width, int height, int min_fps, int max_fps, float bandwidth,
									  int prev_activeUrbs, int prev_packetsPerRequest, int prev_altset, int prev_maxPacketSize,
									  int camFormatInde, int camFrameInde, int camFrameInterva, const char* frameForma,
									  int imageWidth, int imageHeight, int tes) {
    ENTER();

    int result = 0;
	packetsPerRequest = prev_packetsPerRequest;
	maxPacketSize = prev_maxPacketSize;
	activeUrbs = prev_activeUrbs;
	camStreamingAltSetting = prev_altset;
	camFormatIndex = camFormatInde;
	camFrameIndex = camFrameInde;
	camFrameInterval = camFrameInterva;
	frameFormat = frameForma;
	frameWidth = imageWidth;
	frameHeight = imageHeight;
	/* @Parameter test: 0 = stream | 1 = testrun */
	test = tes;

	requestWidth = width;
	requestHeight = height;
	requestMinFps = min_fps;
	requestMaxFps = max_fps;
	requestMode = 0;
	requestBandwidth = bandwidth;

    RETURN(result, int);
}

int UVCPreview::setPreviewDisplay(ANativeWindow *preview_window) {
	ENTER();
	pthread_mutex_lock(&preview_mutex);
	{
		if (mPreviewWindow != preview_window) {
			if (mPreviewWindow)
				ANativeWindow_release(mPreviewWindow);
			mPreviewWindow = preview_window;
			if (LIKELY(mPreviewWindow)) {
				ANativeWindow_setBuffersGeometry(mPreviewWindow,
					frameWidth, frameHeight, previewFormat);
			}
		}
	}
	pthread_mutex_unlock(&preview_mutex);
	RETURN(0, int);
}

// PIXEL_FORMAT defined in UVCPreview.h   0 = raw (yuy2) | 1 = yuv (yuy2) | 2-3 = rgb | 5 = NV21
int UVCPreview::setFrameCallback(JNIEnv *env, jobject frame_callback_obj, int pixel_format) {
	
	ENTER();
	pthread_mutex_lock(&capture_mutex);
	{
		if (isRunning() && isCapturing()) {
			mIsCapturing = false;
			if (mFrameCallbackObj) {
				pthread_cond_signal(&capture_sync);
				pthread_cond_wait(&capture_sync, &capture_mutex);	// wait finishing capturing
			}
		}
		if (!env->IsSameObject(mFrameCallbackObj, frame_callback_obj))	{
			iframecallback_fields.onFrame = NULL;
			if (mFrameCallbackObj) {
				env->DeleteGlobalRef(mFrameCallbackObj);
			}
			mFrameCallbackObj = frame_callback_obj;
			if (frame_callback_obj) {
				// get method IDs of Java object for callback
				jclass clazz = env->GetObjectClass(frame_callback_obj);
				if (LIKELY(clazz)) {
                    LOGD_P("Calling Callback");
					iframecallback_fields.onFrame = env->GetMethodID(clazz,
						"onFrame",	"([B)V");
				} else {
					LOGW("failed to get object class");
				}
				env->ExceptionClear();
				if (!iframecallback_fields.onFrame) {
					LOGE("Can't find IFrameCallback#onFrame");
					env->DeleteGlobalRef(frame_callback_obj);
					mFrameCallbackObj = frame_callback_obj = NULL;
				}
			}
		}
		/* @Parameter test: 0 = stream | 1 = testrun */
		if (!test) {
            if (frame_callback_obj) {
                mPixelFormat = pixel_format;
                callbackPixelFormatChanged();
            } else {
                // PIXEL_FORMAT defined in UVCPreview.h   0 = raw (yuy2) | 1 = yuv (yuy2) | 2-3 = rgb | 5 = NV21  | 6 = MJPEG
                if (strcmp(frameFormat, "MJPEG") == 0) pixel_format = 6;
                else if (strcmp(frameFormat, "YUY2") == 0 ||   strcmp(frameFormat, "UYVY") == 0 ) pixel_format = 1;
                else if (strcmp(frameFormat, "NV21") == 0) pixel_format = 5;
                else pixel_format = 0;
                mPixelFormat = pixel_format;
                callbackPixelFormatChanged();
            }
        } else {
            mPixelFormat = pixel_format;
            /*
            if (mPixelFormat) callbackPixelFormatChanged();
            else {
                if (strcmp(frameFormat, "MJPEG") == 0) pixel_format = 6;
                else if (strcmp(frameFormat, "YUY2") == 0 ||   strcmp(frameFormat, "UYVY") == 0 ) pixel_format = 1;
                else if (strcmp(frameFormat, "NV21") == 0) pixel_format = 5;
                else pixel_format = 0;
                mPixelFormat = pixel_format;
                callbackPixelFormatChanged();
            }
             */
        }
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

void UVCPreview::callbackPixelFormatChanged() {
    mFrameCallbackFunc = NULL;
    const size_t sz = requestWidth * requestHeight;
    switch (mPixelFormat) {
        case PIXEL_FORMAT_RAW:
            LOGI("PIXEL_FORMAT_RAW:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_YUV:
            LOGI("PIXEL_FORMAT_YUV:");
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGB565:
            LOGI("PIXEL_FORMAT_RGB565:");
            mFrameCallbackFunc = uvc_any2rgb565;
            callbackPixelBytes = sz * 2;
            break;
        case PIXEL_FORMAT_RGBX:
            LOGI("PIXEL_FORMAT_RGBX:");
            mFrameCallbackFunc = uvc_any2rgbx;
            callbackPixelBytes = sz * 4;
            break;
        case PIXEL_FORMAT_YUV20SP:
            LOGI("PIXEL_FORMAT_YUV20SP:");
            mFrameCallbackFunc = uvc_yuyv2iyuv420SP;
            callbackPixelBytes = (sz * 3) / 2;
            break;
        case PIXEL_FORMAT_NV21:
            LOGI("PIXEL_FORMAT_NV21:");
            mFrameCallbackFunc = uvc_yuyv2yuv420SP;
            callbackPixelBytes = (sz * 3) / 2;
            break;
        case PIXEL_FORMAT_MJPEG:
            LOGI("PIXEL_FORMAT_MJPEG:");
            callbackPixelBytes = sz * 3;
            break;
	}
}

void UVCPreview::clearDisplay() {
	ENTER();

	ANativeWindow_Buffer buffer;
	pthread_mutex_lock(&capture_mutex);
	{
		if (LIKELY(mCaptureWindow)) {
			if (LIKELY(ANativeWindow_lock(mCaptureWindow, &buffer, NULL) == 0)) {
				uint8_t *dest = (uint8_t *)buffer.bits;
				const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
				const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
				for (int i = 0; i < buffer.height; i++) {
					memset(dest, 0, bytes);
					dest += stride;
				}
				ANativeWindow_unlockAndPost(mCaptureWindow);
			}
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	pthread_mutex_lock(&preview_mutex);
	{
		if (LIKELY(mPreviewWindow)) {
			if (LIKELY(ANativeWindow_lock(mPreviewWindow, &buffer, NULL) == 0)) {
				uint8_t *dest = (uint8_t *)buffer.bits;
				const size_t bytes = buffer.width * PREVIEW_PIXEL_BYTES;
				const int stride = buffer.stride * PREVIEW_PIXEL_BYTES;
				for (int i = 0; i < buffer.height; i++) {
					memset(dest, 0, bytes);
					dest += stride;
				}
				ANativeWindow_unlockAndPost(mPreviewWindow);
			}
		}
	}
	pthread_mutex_unlock(&preview_mutex);

	EXIT();
}

int UVCPreview::startPreview() {
	ENTER();
    LOGD_P("startPreview");
    LOGD_P("test = %d", test);
	int result = EXIT_FAILURE;
	if (!isRunning()) {
		mIsRunning = true;
		pthread_mutex_lock(&preview_mutex);
		{
            LOGD_P("check test");

			if (!test) {
				// Stream Mode
				if (LIKELY(mPreviewWindow)) {
                    LOGD_P("preview_thread_func");
					result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *)this);
                    LOGD_P("preview_thread_func result = %d", result);
				}
			} else {
				// Test Mode
                LOGD_P("preview_thread Test Mode");
				result = pthread_create(&preview_thread, NULL, preview_thread_func, (void *)this);
			}

		}
		pthread_mutex_unlock(&preview_mutex);
		if (UNLIKELY(result != EXIT_SUCCESS)) {
			LOGW("UVCCamera::window does not exist/already running/could not create thread etc.");
			mIsRunning = false;
			pthread_mutex_lock(&preview_mutex);
			{
                LOGD_P("pthread_cond_signal");
				pthread_cond_signal(&preview_sync);
			}
			pthread_mutex_unlock(&preview_mutex);
		}
	}
    LOGD_P("startPreview finished");

	RETURN(result, int);
}

int UVCPreview::stopPreview() {
	ENTER();
	bool b = isRunning();
	if (LIKELY(b)) {
		mIsRunning = false;
		pthread_cond_signal(&preview_sync);
		pthread_cond_signal(&capture_sync);
		if (pthread_join(capture_thread, NULL) != EXIT_SUCCESS) {
			LOGW("UVCPreview::terminate capture thread: pthread_join failed");
		}
		if (pthread_join(preview_thread, NULL) != EXIT_SUCCESS) {
			LOGW("UVCPreview::terminate preview thread: pthread_join failed");
		}
		clearDisplay();
	}
	clearPreviewFrame();
	clearCaptureFrame();
	pthread_mutex_lock(&preview_mutex);
	if (mPreviewWindow) {
		ANativeWindow_release(mPreviewWindow);
		mPreviewWindow = NULL;
	}
	pthread_mutex_unlock(&preview_mutex);
	pthread_mutex_lock(&capture_mutex);
	if (mCaptureWindow) {
		ANativeWindow_release(mCaptureWindow);
		mCaptureWindow = NULL;
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

//**********************************************************************
//
//**********************************************************************
void UVCPreview::uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args) {
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if UNLIKELY(!preview->isRunning() || !frame || !frame->frame_format || !frame->data || !frame->data_bytes) return;
	if (UNLIKELY(
		((frame->frame_format != UVC_FRAME_FORMAT_MJPEG) && (frame->actual_bytes < preview->frameBytes))
		|| (frame->width != preview->frameWidth) || (frame->height != preview->frameHeight) )) {
#if LOCAL_DEBUG
        LOGD_P("broken frame!:format=%d,actual_bytes=%d/%d(%d,%d/%d,%d)",
			frame->frame_format, frame->actual_bytes, preview->frameBytes,
			frame->width, frame->height, preview->frameWidth, preview->frameHeight);
#endif
		return;
	}
	if (LIKELY(preview->isRunning())) {
		uvc_frame_t *copy = preview->get_frame(frame->data_bytes);
		if (UNLIKELY(!copy)) {
			LOGE_P("uvc_callback:unable to allocate duplicate frame!");

#if LOCAL_DEBUG
			LOGE_P("uvc_callback:unable to allocate duplicate frame!");
#endif
			return;
		}
		uvc_error_t ret = uvc_duplicate_frame(frame, copy);
		if (UNLIKELY(ret)) {
			preview->recycle_frame(copy);
			return;
		}
		preview->addPreviewFrame(copy);
	}
}

void UVCPreview::addPreviewFrame(uvc_frame_t *frame) {

	pthread_mutex_lock(&preview_mutex);
	if (isRunning() && (previewFrames.size() < MAX_FRAME)) {
		previewFrames.put(frame);
		frame = NULL;
		pthread_cond_signal(&preview_sync);
	}
	pthread_mutex_unlock(&preview_mutex);
	if (frame) {
		recycle_frame(frame);
	}
}

uvc_frame_t *UVCPreview::waitPreviewFrame() {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&preview_mutex);
	{
		if (!previewFrames.size()) {
			pthread_cond_wait(&preview_sync, &preview_mutex);
		}
		if (LIKELY(isRunning() && previewFrames.size() > 0)) {
			frame = previewFrames.remove(0);
		}
	}
	pthread_mutex_unlock(&preview_mutex);
	return frame;
}

void UVCPreview::clearPreviewFrame() {
	pthread_mutex_lock(&preview_mutex);
	{
		for (int i = 0; i < previewFrames.size(); i++)
			recycle_frame(previewFrames[i]);
		previewFrames.clear();
	}
	pthread_mutex_unlock(&preview_mutex);
}

void *UVCPreview::preview_thread_func(void *vptr_args) {
	int result;

	//ENTER();
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if (LIKELY(preview)) {
		uvc_stream_ctrl_t ctrl;
		memset(&ctrl, 0, sizeof(uvc_stream_ctrl_t));
		uvc_stream_handle_t strmh;
        LOGD_P("prepare_preview(&ctrl, preview)");

		result = preview->prepare_preview(&ctrl, preview);
        LOGD_P("prepare_preview returned %d", result);

		if (LIKELY(!result)) {
            LOGD_P("do_preview(&ctrl)");
			preview->do_preview(&ctrl);
		}
	}
	PRE_EXIT();
	pthread_exit(NULL);
}

int UVCPreview::prepare_preview(uvc_stream_ctrl_t *ctrl, UVCPreview *preview) {
	uvc_error_t result;
    LOGD_P("prepare_preview");

	ENTER();
    LOGD_P("ctrl->bInterfaceNumber");

	ctrl->bInterfaceNumber = preview->mDeviceHandle->info->stream_ifs->bInterfaceNumber;
    LOGD_P("bmHint");
	ctrl->bmHint = 1;
	ctrl->bFormatIndex = camFormatIndex;
	ctrl->bFrameIndex = camFrameIndex;
	ctrl->dwFrameInterval = camFrameInterval;

    LOGD_P("control_TransferUVC");
	result = control_TransferUVC(ctrl, preview->mDeviceHandle);
	if (UNLIKELY(result)) {
		LOGE("control_TransferUVC failed!!\ncontrol_TransferUVC:err=%d", result);
		return -1;
	} else	LOGD_P("control_TransferUVC = UVC_SUCCESS");

	if(ctrl->bInterfaceNumber < 0) return -1;

    LOGD_P("ctrl.bmHint = %d, ctrl.bFormatIndex = %d, ctrl.dwMaxPayloadTransferSize = %d", ctrl->bmHint, ctrl->bFormatIndex, ctrl->dwMaxPayloadTransferSize);
	int overload_buffer_value = packetsPerRequest*activeUrbs*maxPacketSize;
	size_t frame_size;
	if (strcmp(frameFormat, "MJPEG") == 0) {
		requestMode = 1;
		frame_size = preview->frameWidth * preview->frameHeight * 3 + overload_buffer_value;
	} else if (strcmp(frameFormat, "YUY2") == 0 ||   strcmp(frameFormat, "UYVY") == 0  ) {
		requestMode = 0;
		frame_size = preview->frameWidth * preview->frameHeight  * 2 + overload_buffer_value;
	} else if (strcmp(frameFormat, "NV21") == 0) {
		frame_size = preview->frameWidth * preview->frameHeight * 3 / 2 + overload_buffer_value;
	} else {
		frame_size = preview->frameWidth * preview->frameHeight * 2 + overload_buffer_value;
	}
	if (LIKELY(!result)) {
#if LOCAL_DEBUG
		uvc_print_stream_ctrl(ctrl, stderr);
#endif
		//uvc_frame_desc_t *frame_desc;
		//result = uvc_get_frame_desc(mDeviceHandle, ctrl, &frame_desc);
		if (LIKELY(!result)) {
			//frameWidth = frame_desc->wWidth;
			//frameHeight = frame_desc->wHeight;
			if (!test) {
                LOGD_P("frameSize=(%d,%d)@%s", frameWidth, frameHeight, (!requestMode ? "YUYV" : "MJPEG"));
				pthread_mutex_lock(&preview_mutex);
				if (LIKELY(mPreviewWindow)) {
					ANativeWindow_setBuffersGeometry(mPreviewWindow,
													 frameWidth, frameHeight, previewFormat);
				}
			} else LOGD_P("Skipping ANativeWindow_setBuffersGeometry");
			pthread_mutex_unlock(&preview_mutex);
		} else {
			frameWidth = requestWidth;
			frameHeight = requestHeight;
		}
		frameMode = requestMode;
		frameBytes = frameWidth * frameHeight * (!requestMode ? 2 : 4);
		previewBytes = frameWidth * frameHeight * PREVIEW_PIXEL_BYTES;
	} else {
		LOGE("could not negotiate with camera:err=%d", result);
	}
	RETURN(result, int);
}

void UVCPreview::do_preview(uvc_stream_ctrl_t *ctrl) {
	ENTER();
	uvc_error_t result;
	uvc_frame_t *frame = NULL;
	uvc_frame_t *frame_mjpeg = NULL;
	int overload_buffer_value = packetsPerRequest*activeUrbs*maxPacketSize;
	size_t frame_size;
	if (strcmp(frameFormat, "MJPEG") == 0) {
		requestMode = 1;
		frame_size = frameWidth * frameHeight * 3 + overload_buffer_value;
	} else if (strcmp(frameFormat, "YUY2") == 0 ||   strcmp(frameFormat, "UYVY") == 0  ) {
		requestMode = 0;
		frame_size = frameWidth * frameHeight  * 2 + overload_buffer_value;
	} else if (strcmp(frameFormat, "NV21") == 0) {
		frame_size = frameWidth * frameHeight * 3 / 2 + overload_buffer_value;
	} else {
		frame_size = frameWidth * frameHeight * 2 + overload_buffer_value;
	}
	uvc_stream_handle_t *strmh;
	result = uvc_stream_open_ctrl(mDeviceHandle, &strmh, ctrl, frame_size);
	if (UNLIKELY(result != UVC_SUCCESS)) {
		LOGE("uvc_stream_open_ctrl failed !!; return = %d", result);
		return;
	}


	result = uvc_stream_start_random(strmh, uvc_preview_frame_callback, (void *)this, 0, 0, activeUrbs, packetsPerRequest, camStreamingAltSetting, maxPacketSize );

	//uvc_error_t result = uvc_start_streaming_bandwidth(		mDeviceHandle, ctrl, uvc_preview_frame_callback, (void *)this, requestBandwidth, 0);

	if (LIKELY(!result)) {
		clearPreviewFrame();
        LOGD_P("running capture Thread");
		pthread_create(&capture_thread, NULL, capture_thread_func, (void *)this);

#if LOCAL_DEBUG
		LOGI("Streaming...");
#endif
		if (frameMode) {
			// MJPEG mode
			for ( ; LIKELY(isRunning()) ; ) {
				frame_mjpeg = waitPreviewFrame();
				if (LIKELY(frame_mjpeg)) {
					/* @Parameter test: 0 = stream | 1 = testrun */
					if (!test) {
                        frame = get_frame(frame_mjpeg->width * frame_mjpeg->height * 2);
                        result = uvc_mjpeg2yuyv(frame_mjpeg, frame);
                        //recycle_frame(frame_mjpeg);
						if (LIKELY(!result)) {
							frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
                            recycle_frame(frame);
                            // Only store Capture Frame when needed
							if(mIsCapturing) addCaptureFrame(frame_mjpeg);
                            else recycle_frame(frame_mjpeg);
						} else {
							recycle_frame(frame);
						}
					} else {
                        LOGD_P("addCaptureFrame MJPEG mode");
                        addCaptureFrame(frame_mjpeg);
                    }
				}
			}
		} else {
			// yuyv mode
			for ( ; LIKELY(isRunning()) ; ) {
				frame = waitPreviewFrame();
				if (!test) {
					if (LIKELY(frame)) {
						frame = draw_preview_one(frame, &mPreviewWindow, uvc_any2rgbx, 4);
                        // Only store Capture Frame when needed
                        if(mIsCapturing) addCaptureFrame(frame);
                        else recycle_frame(frame);
					}
				} else {
                    LOGD_P("addCaptureFrame yuyv mode");
					addCaptureFrame(frame);
				}

			}
		}
		pthread_cond_signal(&capture_sync);
#if LOCAL_DEBUG
		LOGI("preview_thread_func:wait for all callbacks complete");
#endif
		uvc_stop_streaming(mDeviceHandle);
#if LOCAL_DEBUG
		LOGI("Streaming finished");
#endif
	} else {
		uvc_perror(result, "failed start_streaming");
	}

	EXIT();
}

static void copyFrame(const uint8_t *src, uint8_t *dest, const int width, int height, const int stride_src, const int stride_dest) {
	const int h8 = height % 8;
	for (int i = 0; i < h8; i++) {
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
	}
	for (int i = 0; i < height; i += 8) {
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
		memcpy(dest, src, width);
		dest += stride_dest; src += stride_src;
	}
}


// transfer specific frame data to the Surface(ANativeWindow)
int copyToSurface(uvc_frame_t *frame, ANativeWindow **window) {
	// ENTER();
	int result = 0;
	if (LIKELY(*window)) {
		ANativeWindow_Buffer buffer;
		if (LIKELY(ANativeWindow_lock(*window, &buffer, NULL) == 0)) {
			// source = frame data
			const uint8_t *src = (uint8_t *)frame->data;
			const int src_w = frame->width * PREVIEW_PIXEL_BYTES;
			const int src_step = frame->width * PREVIEW_PIXEL_BYTES;
			// destination = Surface(ANativeWindow)
			uint8_t *dest = (uint8_t *)buffer.bits;
			const int dest_w = buffer.width * PREVIEW_PIXEL_BYTES;
			const int dest_step = buffer.stride * PREVIEW_PIXEL_BYTES;
			// use lower transfer bytes
			const int w = src_w < dest_w ? src_w : dest_w;
			// use lower height
			const int h = frame->height < buffer.height ? frame->height : buffer.height;
			// transfer from frame data to the Surface
			copyFrame(src, dest, w, h, src_step, dest_step);
			ANativeWindow_unlockAndPost(*window);
		} else {
			result = -1;
		}
	} else {
		result = -1;
	}
	return result; //RETURN(result, int);
}

// changed to return original frame instead of returning converted frame even if convert_func is not null.
uvc_frame_t *UVCPreview::draw_preview_one(uvc_frame_t *frame, ANativeWindow **window, convFunc_t convert_func, int pixcelBytes) {
	// ENTER();

	int b = 0;
	pthread_mutex_lock(&preview_mutex);
	{
		b = *window != NULL;
	}
	pthread_mutex_unlock(&preview_mutex);
	if (LIKELY(b)) {
		uvc_frame_t *converted;
		if (convert_func) {
			converted = get_frame(frame->width * frame->height * pixcelBytes);
			if LIKELY(converted) {
				b = convert_func(frame, converted);
				if (!b) {
					pthread_mutex_lock(&preview_mutex);
					copyToSurface(converted, window);
					pthread_mutex_unlock(&preview_mutex);
				} else {
					LOGE("failed converting");
				}
				recycle_frame(converted);
			}
		} else {
			pthread_mutex_lock(&preview_mutex);
			copyToSurface(frame, window);
			pthread_mutex_unlock(&preview_mutex);
		}
	}
	return frame; //RETURN(frame, uvc_frame_t *);
}

inline const bool UVCPreview::isCapturing() const { return mIsCapturing; }

int UVCPreview::setCaptureDisplay(ANativeWindow *capture_window) {
	ENTER();
	pthread_mutex_lock(&capture_mutex);
	{
		if (isRunning() && isCapturing()) {
			mIsCapturing = false;
			if (mCaptureWindow) {
				pthread_cond_signal(&capture_sync);
				pthread_cond_wait(&capture_sync, &capture_mutex);	// wait finishing capturing
			}
		}
		if (mCaptureWindow != capture_window) {
			// release current Surface if already assigned.
			if (UNLIKELY(mCaptureWindow)) ANativeWindow_release(mCaptureWindow);
			mCaptureWindow = capture_window;
			// if you use Surface came from MediaCodec#createInputSurface
			// you could not change window format at least when you use
			// ANativeWindow_lock / ANativeWindow_unlockAndPost
			// to write frame data to the Surface...
			// So we need check here.
			if (mCaptureWindow) {
				int32_t window_format = ANativeWindow_getFormat(mCaptureWindow);
				if ((window_format != WINDOW_FORMAT_RGB_565)
					&& (previewFormat == WINDOW_FORMAT_RGB_565)) {
					LOGE("window format mismatch, cancelled movie capturing.");
					ANativeWindow_release(mCaptureWindow);
					mCaptureWindow = NULL;
				}
			}
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	RETURN(0, int);
}

void UVCPreview::addCaptureFrame(uvc_frame_t *frame) {

	pthread_mutex_lock(&capture_mutex);
	if (LIKELY(isRunning())) {
		// keep only latest one
		if (captureQueu) {
			recycle_frame(captureQueu);
		}
		captureQueu = frame;
		pthread_cond_broadcast(&capture_sync);
	}
	pthread_mutex_unlock(&capture_mutex);
}

/**
 * get frame data for capturing, if not exist, block and wait
 */
uvc_frame_t *UVCPreview::waitCaptureFrame() {
	uvc_frame_t *frame = NULL;
	pthread_mutex_lock(&capture_mutex);
	{
		if (!captureQueu) {
			pthread_cond_wait(&capture_sync, &capture_mutex);
		}
		if (LIKELY(isRunning() && captureQueu)) {
			frame = captureQueu;
			captureQueu = NULL;
		}
	}
	pthread_mutex_unlock(&capture_mutex);
	return frame;
}

/**
 * clear drame data for capturing
 */
void UVCPreview::clearCaptureFrame() {
	pthread_mutex_lock(&capture_mutex);
	{
		if (captureQueu)
			recycle_frame(captureQueu);
		captureQueu = NULL;
	}
	pthread_mutex_unlock(&capture_mutex);
}

//======================================================================
/*
 * thread function
 * @param vptr_args pointer to UVCPreview instance
 */
// static
void *UVCPreview::capture_thread_func(void *vptr_args) {
	int result;

	//ENTER();
	UVCPreview *preview = reinterpret_cast<UVCPreview *>(vptr_args);
	if (LIKELY(preview)) {

        LOGD_P("AttachCurrentThread");
		JNIEnv *env;
		g_VM->AttachCurrentThread(&env, NULL);
        LOGD_P("do_capture(env)");
		preview->do_capture(env);	// never return until finish previewing
		// detach from JavaVM
		g_VM->DetachCurrentThread();
        LOGD_P("DetachCurrentThread() complete");

		MARK("DetachCurrentThread");

	}
	//PRE_EXIT();
	pthread_exit(NULL);
}

/**
 * the actual function for capturing
 */
void UVCPreview::do_capture(JNIEnv *env) {

	ENTER();

	clearCaptureFrame();
	callbackPixelFormatChanged();
	for (; isRunning() ;) {
        // switch mIsCapturing only when needed for imagecapture and videorecord
		//mIsCapturing = true;
		if (mCaptureWindow) {
			do_capture_surface(env);
		} else {
			do_capture_idle_loop(env);
		}
		pthread_cond_broadcast(&capture_sync);
	}	// end of for (; isRunning() ;)
	EXIT();
}

void UVCPreview::do_capture_idle_loop(JNIEnv *env) {
	for (; isRunning() && isCapturing() ;) {
		do_capture_callback(env, waitCaptureFrame());
        if (mPictureCapturing) {
            mPictureCapturing = false;
            mIsCapturing = false;
        }
	}
}

/**
 * write frame data to Surface for capturing
 */
void UVCPreview::do_capture_surface(JNIEnv *env) {
	//ENTER();

	uvc_frame_t *frame = NULL;
	uvc_frame_t *converted = NULL;
	char *local_picture_path;

	for (; isRunning() && isCapturing() ;) {
		frame = waitCaptureFrame();
		if (LIKELY(frame)) {
			// frame data is always YUYV format.
			if LIKELY(isCapturing()) {
				if (UNLIKELY(!converted)) {
					converted = get_frame(previewBytes);
				}
				if (LIKELY(converted)) {
					int b = uvc_any2rgbx(frame, converted);
					if (!b) {
						if (LIKELY(mCaptureWindow)) {
							copyToSurface(converted, &mCaptureWindow);
						}
					}
				}
			}
			do_capture_callback(env, frame);
		}
	}
	if (converted) {
		recycle_frame(converted);
	}
	if (mCaptureWindow) {
		ANativeWindow_release(mCaptureWindow);
		mCaptureWindow = NULL;
	}

	//EXIT();
}

/**
* call IFrameCallback#onFrame if needs
 */
void UVCPreview::do_capture_callback(JNIEnv *env, uvc_frame_t *frame) {
	//ENTER();

	if (LIKELY(frame)) {
		uvc_frame_t *callback_frame = frame;
		if (mFrameCallbackObj) {
            // --> mFrameCallbackFunc likely not in use (only for Pixel Format NV21)
			if (mFrameCallbackFunc) {
				callback_frame = get_frame(callbackPixelBytes);
				if (LIKELY(callback_frame)) {
					int b = mFrameCallbackFunc(frame, callback_frame);
					recycle_frame(frame);
					if (UNLIKELY(b)) {
						LOGW("failed to convert for callback frame");
						goto SKIP;
					}
				} else {
					LOGW("failed to allocate for callback frame");
					callback_frame = frame;
					goto SKIP;
				}
			}


            /////////////////////////////////////////////////////////
            // Changed by Peter St. 14.01.2023
            // Send arrays with their original length
            ////////////////////////////////////////////////////////

            jbyteArray retArray = env->NewByteArray(callback_frame->actual_bytes);
            if(env->GetArrayLength( retArray) != callback_frame->actual_bytes)
            {
                env->DeleteLocalRef( retArray);
                retArray = env->NewByteArray( callback_frame->actual_bytes);
            }
            env->SetByteArrayRegion(retArray, 0, callback_frame->actual_bytes, (jbyte*) callback_frame->data);
            env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, retArray);
            env->ExceptionClear();
            env->DeleteLocalRef(retArray);
            /*
			jbyteArray retArray = env->NewByteArray(callbackPixelBytes);
			if(env->GetArrayLength( retArray) != callbackPixelBytes)
			{
				env->DeleteLocalRef( retArray);
				retArray = env->NewByteArray( callbackPixelBytes);
			}
			env->SetByteArrayRegion(retArray, 0, callbackPixelBytes, (jbyte*) callback_frame->data);
			env->CallVoidMethod(mFrameCallbackObj, iframecallback_fields.onFrame, retArray);
			env->ExceptionClear();
			env->DeleteLocalRef(retArray);
            */
		}
		SKIP:
		recycle_frame(callback_frame);
	}
	//EXIT();


}


/**
 *  set JavaVM
 *  Disabled by Peter St. because of Errors in higher Android devices
 */
int UVCPreview::setJavaVM(JNIEnv *env, jobject obj) {
	ENTER();
	//g_obj = env->NewGlobalRef(obj);
	//env->GetJavaVM(&g_jvm);
	//g_env = env;
	return UVC_SUCCESS;
}

int UVCPreview::enable_mIsCapturing() {
    ENTER();
    int result = EXIT_SUCCESS;
    mIsCapturing = true;
    RETURN(result, int);
}

int UVCPreview::capture_picture() {
    ENTER();
    int result = EXIT_SUCCESS;
    mIsCapturing = true;
    mPictureCapturing = true;
    RETURN(result, int);
}


long create_UVCPreview(uvc_device_handle_t *devh, long preview_pointer) {
    UVCPreview *uvcPreview = new UVCPreview(devh);
	preview_pointer = reinterpret_cast<long>(uvcPreview);
    return reinterpret_cast<long>(uvcPreview);
}

int set_preview_size(long preview_pointer, int width, int height, int min_fps, int max_fps, int mode, float bandwidth) {
    int result = EXIT_FAILURE;
    UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    if (camera) result = camera->setPreviewSize(width, height, min_fps, max_fps, mode, bandwidth);
    return result;
}

/* @Parameter test: 0 = stream | 1 = testrun */
int set_preview_size_random(long preview_pointer, int width, int height, int min_fps, int max_fps, float bandwidth,
							int activeUrbs, int packetsPerRequest, int altset, int maxPacketSize,
							int camFormatInde, int camFrameInde, int camFrameInterva, const char* frameForma,
							int imageWidth, int imageHeight, int test ) {
    int result = EXIT_FAILURE;
    UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    if (camera) result = camera->setPreviewSize_random(width, height, min_fps, max_fps, bandwidth, activeUrbs, packetsPerRequest,
													   altset, maxPacketSize, camFormatInde, camFrameInde, camFrameInterva, frameForma,
													   imageWidth, imageHeight, test );
    return result;
}

int set_preview_display(long preview_pointer, ANativeWindow *preview_window) {
    int result = EXIT_FAILURE;
    UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    if (camera) result = camera->setPreviewDisplay(preview_window);
    return result;
}

int setFrameCallback(long preview_pointer, JNIEnv *env, jobject frame_callback_obj, int pixel_format) {
	int result = EXIT_FAILURE;
	UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    LOGD_P("camera->setFrameCallback");
	if (camera) result = camera->setFrameCallback(env, frame_callback_obj, pixel_format);
	return result;
}

int startPreview(long preview_pointer) {
	int result = EXIT_FAILURE;
	UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
	if (camera) result = camera->startPreview();
	return result;
}

int stopPreview(long preview_pointer) {
	int result = EXIT_FAILURE;
	UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
	if (camera) result = camera->stopPreview();
	return result;
}

int setJavaVM(long preview_pointer, JNIEnv *env, jobject obj) {
	ENTER();

	int result = EXIT_FAILURE;
	UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
	if (camera) {
		env->GetJavaVM(& g_VM);
		result = UVC_SUCCESS;
	}

	return result;
}

int capturePicture(long preview_pointer) {
    int result = EXIT_FAILURE;
    UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    if (camera) result = camera->capture_picture();
    return result;
}

int enableMIsCapturing(long preview_pointer) {
    int result = EXIT_FAILURE;
    UVCPreview *camera = reinterpret_cast<UVCPreview *>(preview_pointer);
    if (camera) result = camera->enable_mIsCapturing();
    return result;
}