#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <linux/usbdevice_fs.h>
#include <signal.h>
#include <android/log.h>
#include "libusb_support.h"
#include <sys/wait.h>
#include <stdbool.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <libusb.h>
#include <libyuv/include/libyuv.h>

#include "jpeg8d/jpeglib.h"


#define  LOG_TAG    "From Native"

#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define	LOCAL_DEBUG 0
#define MAX_FRAME 4
#define PREVIEW_PIXEL_BYTES 4	// RGBA/RGBX
#define FRAME_POOL_SZ MAX_FRAME + 2



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

volatile bool write_Ctl_Buffer = false;
int ueberschreitungDerUebertragungslaenge = 0 ;

int verbose = 0;

// Camera Values

static int packetsPerRequest;
static int maxPacketSize;
static int activeUrbs;
static int camStreamingAltSetting;
static int camFormatIndex;
static int camFrameIndex;
static int camFrameInterval;
static int minCamFrameInterval;
static int maxCamFrameInterval;
static int bmHint;
static int imageWidth;
static int imageHeight;
static int camStreamingEndpoint;
static const char *mUsbFs;
static int productID;
static int vendorID;
static int busnum;
static int devaddr;
static uint16_t bcdUVC;
static int laufzeit = 2;
static int fd, i, j, result = -1;
static int camStreamingInterfaceNum;
const char* frameFormat;    //  MJPEG   YUY2
volatile int total = 0;
volatile int totalFrame = 0;
volatile bool initialized = false;
volatile  bool runningStream = false;
volatile bool runningStreamFragment = false;
static uint8_t frameUeberspringen = 0;
static uint8_t numberOfAutoFrames;
static int streamEndPointAdressOverNative;

#define UVC_STREAM_EOH (1 << 7)
#define UVC_STREAM_ERR (1 << 6)
#define UVC_STREAM_STI (1 << 5)
#define UVC_STREAM_RES (1 << 4)
#define UVC_STREAM_SCR (1 << 3)
#define UVC_STREAM_PTS (1 << 2)
#define UVC_STREAM_EOF (1 << 1)
#define UVC_STREAM_FID (1 << 0)
#define TAG "LibUsb"

#define IS_CONTROL_CMD_READ(c) ((c) & 0x80)
#define CONTROL_CMD_SET_READ(c) ((c) | 0x80)
#define CONTROL_CMD_SET_WRITE(c) ((c) & ~0x80)


#define CONTROL_SPECIAL_RESID 0

#define CONTROL_GET_VERSION CONTROL_CMD_SET_READ(0)
#define CONTROL_GET_LAST_COMMAND_STATUS CONTROL_CMD_SET_READ(1)

typedef uint8_t control_version_t;
#define TIMEOUT_MS 100

/** This is the version of control protocol. Used to check compatibility */
#define CONTROL_VERSION 0x10

libusb_context *ctx;
libusb_device_handle *devh = NULL;

AutotransferStruct autoStruct;
ManualTestFrameStruct manualFrameStruct;
clock_t startTime;

int initStreamingParmsIntArray[3];
int probedStreamingParmsIntArray[3];
int finalStreamingParmsIntArray_first[3];
int finalStreamingParmsIntArray[3];

autoStreamComplete autoStreamfinished = NULL;
void setAutoStreamComplete(autoStreamComplete autoStream)
{
    autoStreamfinished = autoStream;
}

AutotransferStruct get_autotransferStruct () {
    return autoStruct;
}

typedef struct _Frame_Data
{
    int FrameSize;
    int  FrameBufferSize;
    unsigned char videoframe[];
} FrameData;
FrameData *videoFrameData;

uint8_t streamControl[48];
uint8_t unpackUsbInt(uint8_t *p, int i);

bool camIsOpen;
bool cameraDevice_is_wraped;

typedef struct _CTL_Data
{
    int  BufferSize;
    unsigned char ctl_transfer_values[];
} CtlData;
CtlData *ctl_transfer_Data;

void getStreamingParmsArray(int *array, uint8_t *buf) {
    array[0] = buf[2] & 0xf;
    array[1] = buf[3] & 0xf;
    uint8_t pos = 4;
    array[2] = (buf[pos + 3] << 24) | ((buf[pos + 2] & 0xFF) << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
}

eventCallback sendReceivedDataToJava = NULL;
void setCallback(eventCallback evnHnd) {
    sendReceivedDataToJava = evnHnd;
}

jnaFrameCallback jnaSendFrameToJava = NULL;
void setJnaFrameCallback(jnaFrameCallback evnHnd) {
    jnaSendFrameToJava = evnHnd;
}

#define		LIKELY(x)					((__builtin_expect(!!(x), 1)))	// x is likely true
#define		UNLIKELY(x)					((__builtin_expect(!!(x), 0)))	// x is likely false


static inline unsigned char sat(int i) {
    return (unsigned char) (i >= 255 ? 255 : (i < 0 ? 0 : i));
}

#define IYUYV2RGBX_2(pyuv, prgbx, ax, bx) { \
		const int d1 = (pyuv)[ax+1]; \
		const int d3 = (pyuv)[ax+3]; \
		const int r = (22987 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
		const int g = (-5636 * (d1/*(pyuv)[ax+1]*/ - 128) - 11698 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
		const int b = (29049 * (d1/*(pyuv)[ax+1]*/ - 128)) >> 14; \
		const int y0 = (pyuv)[ax+0]; \
		(prgbx)[bx+0] = sat(y0 + r); \
		(prgbx)[bx+1] = sat(y0 + g); \
		(prgbx)[bx+2] = sat(y0 + b); \
		(prgbx)[bx+3] = 0xff; \
		const int y2 = (pyuv)[ax+2]; \
		(prgbx)[bx+4] = sat(y2 + r); \
		(prgbx)[bx+5] = sat(y2 + g); \
		(prgbx)[bx+6] = sat(y2 + b); \
		(prgbx)[bx+7] = 0xff; \
    }
#define IYUYV2RGBX_16(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_8(pyuv, prgbx, ax + PIXEL8_YUYV, bx + PIXEL8_RGBX);
#define IYUYV2RGBX_8(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_4(pyuv, prgbx, ax + PIXEL4_YUYV, bx + PIXEL4_RGBX);
#define IYUYV2RGBX_4(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_2(pyuv, prgbx, ax, bx) \
	IYUYV2RGBX_2(pyuv, prgbx, ax + PIXEL2_YUYV, bx + PIXEL2_RGBX);

/** Color coding of stream, transport-independent
 * @ingroup streaming
 */
enum uvc_frame_format {
    UVC_FRAME_FORMAT_UNKNOWN = 0,
    /** Any supported format */
    UVC_FRAME_FORMAT_ANY = 0,
    UVC_FRAME_FORMAT_UNCOMPRESSED,
    UVC_FRAME_FORMAT_COMPRESSED,
    /** YUYV/YUV2/YUV422: YUV encoding with one luminance value per pixel and
     * one UV (chrominance) pair for every two pixels.
     */
    UVC_FRAME_FORMAT_YUYV,
    UVC_FRAME_FORMAT_UYVY,
    /** 16-bits RGB */
    UVC_FRAME_FORMAT_RGB565,	// RGB565
    /** 24-bit RGB */
    UVC_FRAME_FORMAT_RGB,		// RGB888
    UVC_FRAME_FORMAT_BGR,		// BGR888
    /* 32-bits RGB */
    UVC_FRAME_FORMAT_RGBX,		// RGBX8888
    /** Motion-JPEG (or JPEG) encoded images */
    UVC_FRAME_FORMAT_MJPEG,
    UVC_FRAME_FORMAT_GRAY8,
    UVC_FRAME_FORMAT_BY8,
    /** Number of formats understood */
    UVC_FRAME_FORMAT_COUNT,
    // YVU420SemiPlanar
    UVC_FRAME_FORMAT__NV21 = 5,

};


/** UVC error types, based on libusb errors
 * @ingroup diag
 */
typedef enum uvc_error {
    /** Success (no error) */
    UVC_SUCCESS = 0,
    /** Input/output error */
    UVC_ERROR_IO = -1,
    /** Invalid parameter */
    UVC_ERROR_INVALID_PARAM = -2,
    /** Access denied */
    UVC_ERROR_ACCESS = -3,
    /** No such device */
    UVC_ERROR_NO_DEVICE = -4,
    /** Entity not found */
    UVC_ERROR_NOT_FOUND = -5,
    /** Resource busy */
    UVC_ERROR_BUSY = -6,
    /** Operation timed out */
    UVC_ERROR_TIMEOUT = -7,
    /** Overflow */
    UVC_ERROR_OVERFLOW = -8,
    /** Pipe error */
    UVC_ERROR_PIPE = -9,
    /** System call interrupted */
    UVC_ERROR_INTERRUPTED = -10,
    /** Insufficient memory */
    UVC_ERROR_NO_MEM = -11,
    /** Operation not supported */
    UVC_ERROR_NOT_SUPPORTED = -12,
    /** Device is not UVC-compliant */
    UVC_ERROR_INVALID_DEVICE = -50,
    /** Mode not supported */
    UVC_ERROR_INVALID_MODE = -51,
    /** Resource has a callback (can't use polling and async) */
    UVC_ERROR_CALLBACK_EXISTS = -52,
    /** Undefined error */
    UVC_ERROR_OTHER = -99
} uvc_error_t;



/** @brief Print a message explaining an error in the UVC driver
 * @ingroup diag
 *
 * @param err UVC error code
 * @param msg Optional custom message, prepended to output
 */
void uvc_perror(uvc_error_t err, const char *msg) {
    if (msg && *msg) {
        LOGD("%s: (%d)\n", msg, err);
    } else {
        LOGD("(%d)\n", err);
    }
/*
	 if (msg && *msg) {
	 	 fputs(msg, stderr);
	 	 fputs(": ", stderr);
	 }

	 FPRINTF(stderr, "%s (%d)\n", uvc_strerror(err), err);
*/
}



/** Handle on an open UVC device.
 *
 * Get one of these from uvc_open(). Once you uvc_close()
 * it, it's no longer valid.
 */
struct uvc_device_handle;
typedef struct uvc_device_handle uvc_device_handle_t;


/** An image frame received from the UVC device
 * @ingroup streaming
 */
typedef struct uvc_frame {
    /** Image data for this frame */
    void *data;
    /** Size of image data buffer */
    size_t data_bytes;
    /** XXX Size of actual received data to confirm whether the received bytes is same
     * as expected on user function when some microframes dropped */
    size_t actual_bytes;
    /** Width of image in pixels */
    uint32_t width;
    /** Height of image in pixels */
    uint32_t height;
    /** Pixel data format */
    enum uvc_frame_format frame_format;
    /** Number of bytes per horizontal line (undefined for compressed format) */
    size_t step;
    /** Frame number (may skip, but is strictly monotonically increasing) */
    uint32_t sequence;
    /** Estimate of system time when the device started capturing the image */
    struct timeval capture_time;
    /** Handle on the device that produced the image.
     * @warning You must not call any uvc_* functions during a callback. */
    uvc_device_handle_t *source;
    /** Is the data buffer owned by the library?
     * If 1, the data buffer can be arbitrarily reallocated by frame conversion
     * functions.
     * If 0, the data buffer will not be reallocated or freed by the library.
     * Set this field to zero if you are supplying the buffer.
     */
    uint8_t library_owns_data;
} uvc_frame_t;

/** @brief Free a frame structure
 * @ingroup frame
 *
 * @param frame Frame to destroy
 */
void uvc_free_frame(uvc_frame_t *frame) {
    if ((frame->data_bytes > 0) && frame->library_owns_data)
        free(frame->data);

    free(frame);
}

/** @internal */
uvc_error_t uvc_ensure_frame_size(uvc_frame_t *frame, size_t need_bytes) {
    if LIKELY(frame->library_owns_data) {
        if UNLIKELY(!frame->data || frame->data_bytes != need_bytes) {
            frame->actual_bytes = frame->data_bytes = need_bytes;	// XXX
            frame->data = realloc(frame->data, frame->data_bytes);
        }
        if (UNLIKELY(!frame->data || !need_bytes))
            return UVC_ERROR_NO_MEM;
        return UVC_SUCCESS;
    } else {
        if (UNLIKELY(!frame->data || frame->data_bytes < need_bytes))
            return UVC_ERROR_NO_MEM;
        return UVC_SUCCESS;
    }
}


/** @brief Convert a frame from YUYV to RGBX8888
 * @ingroup frame
 * @param ini YUYV frame
 * @param out RGBX8888 frame
 */
uvc_error_t uvc_yuyv2rgbx(uvc_frame_t *out) {
    out->width = imageWidth;
    out->height = imageHeight;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (out->library_owns_data)
        out->step = imageWidth * PIXEL_RGBX;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    uint8_t *pyuv = videoFrameData->videoframe;
    const uint8_t *pyuv_end = pyuv + videoFrameData->FrameBufferSize - PIXEL8_YUYV;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // YUYV => RGBX8888
#if USE_STRIDE
    if (in->step && out->step && (in->step != out->step)) {
		const int hh = in->height < out->height ? in->height : out->height;
		const int ww = in->width < out->width ? in->width : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = in->data + in->step * h;
			prgbx = out->data + out->step * h;
			for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
				IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

				prgbx += PIXEL8_RGBX;
				pyuv += PIXEL8_YUYV;
				w += 8;
			}
		}
	} else {
		// compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
		for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
			IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

			prgbx += PIXEL8_RGBX;
			pyuv += PIXEL8_YUYV;
		}
	}
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
        IYUYV2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}

uvc_error_t uvc_yuyv2yuv420SP(uvc_frame_t *out) {


    out->frame_format = UVC_FRAME_FORMAT__NV21;
    out->width = imageWidth;
    out->height = imageHeight;
    if (out->library_owns_data)
        out->step = imageWidth * PIXEL_RGBX;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    const uint8_t *src = videoFrameData->videoframe;
    uint8_t *dest = out->data;
    const int32_t width = imageWidth;
    const int32_t height = imageHeight;
    const int32_t src_width = imageWidth;
    const int32_t src_height = imageHeight;
    const int32_t dest_width = out->width = out->step = imageWidth;
    const int32_t dest_height = out->height = imageHeight;

    const uint32_t hh = src_height < dest_height ? src_height : dest_height;
    uint8_t *uv = dest + dest_width * dest_height;
    int h, w;
    for (h = 0; h < hh - 1; h += 2) {
        uint8_t *y0 = dest + width * h;
        uint8_t *y1 = y0 + width;
        const uint8_t *yuv = src + src_width * h;
        for (w = 0; w < width; w += 4) {
            *(y0++) = yuv[0];	// y
            *(y0++) = yuv[2];	// y'
            *(y0++) = yuv[4];	// y''
            *(y0++) = yuv[6];	// y'''
            *(uv++) = yuv[1];	// u
            *(uv++) = yuv[3];	// v
            *(uv++) = yuv[5];	// u
            *(uv++) = yuv[7];	// v
            *(y1++) = yuv[src_width+0];	// y on next low
            *(y1++) = yuv[src_width+2];	// y' on next low
            *(y1++) = yuv[src_width+4];	// y''  on next low
            *(y1++) = yuv[src_width+6];	// y'''  on next low
            yuv += 8;	// (1pixel=2bytes)x4pixels=8bytes
        }
    }
    return UVC_SUCCESS;
}


/** @brief Allocate a frame structure
 * @ingroup frame
 *
 * @param data_bytes Number of bytes to allocate, or zero
 * @return New frame, or NULL on error
 */
uvc_frame_t *uvc_allocate_frame(size_t data_bytes) {
    uvc_frame_t *frame = malloc(sizeof(*frame));	// FIXME using buffer pool is better performance(5-30%) than directory use malloc everytime.

    if (UNLIKELY(!frame))
        return NULL;

#ifndef __ANDROID__
    // XXX in many case, it is not neccesary to clear because all fields are set before use
	// therefore we remove this to improve performace, but be care not to forget to set fields before use
	memset(frame, 0, sizeof(*frame));	// bzero(frame, sizeof(*frame)); // bzero is deprecated
#endif
//	frame->library_owns_data = 1;	// XXX moved to lower

    if (LIKELY(data_bytes > 0)) {
        frame->library_owns_data = 1;
        frame->actual_bytes = frame->data_bytes = data_bytes;	// XXX
        frame->data = malloc(data_bytes);

        if (UNLIKELY(!frame->data)) {
            free(frame);
            return NULL ;
        }
    }

    return frame;
}

/// JNI Values
JavaVM* javaVm;
jclass class;
jclass jniHelperClass;
jclass mainActivityObj;
ANativeWindow *mCaptureWindow;

// Stream - Activity
jmethodID javaRetrievedStreamActivityFrameFromLibUsb;
jmethodID javainitializeStreamArray;
// For capturing Images during YUV stream (byte[] is MJPEG)
jmethodID javaPicturCapture;


// SERVICE
jmethodID javaServicePublishResults;
jmethodID javaServiceReturnToStreamActivity;


// WebRtc
jmethodID javaRetrievedFrameFromLibUsb;

int rotation = 0;
volatile bool horizontalFlip = false;
volatile bool verticalFlip = false;
volatile bool imageCapture = false;

void setRotation(int rot, int horizontalFl, int verticalFl) {
    rotation = rot;
    if (horizontalFl == 1) horizontalFlip = true;
    else horizontalFlip = false;
    if (verticalFl == 1) verticalFlip = true;
    else verticalFlip = false;
    LOGD("Rotation SET !!!!! horizontalFlip = %d,    verticalFlip = %d", horizontalFl, verticalFl);
}

void flip_vertically(uvc_frame_t *img)
{
    const size_t bytes_per_pixel = PIXEL_RGBX ;
    // stride = Ausschreitung
    const size_t stride = img->width * bytes_per_pixel;
    unsigned char *row = malloc(stride);
    unsigned char *low = img->data;
    unsigned char *high = &img->data[(img->height - 1) * stride];
    for (; low < high; low += stride, high -= stride) {
        memcpy(row, low, stride);
        memcpy(low, high, stride);
        memcpy(high, row, stride);
    }
    free(row);
}

uvc_frame_t *flip_horizontal(uvc_frame_t *frame) {

    uvc_frame_t *flip_img = uvc_allocate_frame(imageWidth * imageHeight * 4);
    flip_img->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (flip_img->library_owns_data)
        flip_img->step = imageWidth * PIXEL_RGBX;
    flip_img->sequence = 0;
    flip_img->capture_time = frame->capture_time;
    flip_img->source = devh;
    int src_stride_argb = frame->width * 4;
    uint* dst_argb = flip_img->data;
    int width = frame->width;
    int height = frame->height;
    int dst_stride_argb = frame->width * 4;
    flip_img->width = frame->width ;
    flip_img->height = frame->height;

    ARGBMirror(frame->data, src_stride_argb,
    dst_argb, dst_stride_argb,
    width, height);

    uvc_free_frame(frame);

    return flip_img;

    /*
     * ARGBMirror(const uint8* src_argb, int src_stride_argb,
                   uint8* dst_argb, int dst_stride_argb,
                   int width, int height);
     */

}

uvc_frame_t *checkFlip(uvc_frame_t *rot_img) {
    if (!verticalFlip && !horizontalFlip) return rot_img;
    else if (verticalFlip && horizontalFlip) flip_vertically(rot_img);
    if (verticalFlip) { flip_vertically(rot_img); return flip_horizontal(rot_img); }
    if (horizontalFlip) return flip_horizontal(rot_img);
    return rot_img;
}

uvc_frame_t *checkRotation(uvc_frame_t *rgb) {
    if (rotation == 0) return checkFlip(rgb);
    uvc_frame_t *rot_img = uvc_allocate_frame(imageWidth * imageHeight * 4);
    rot_img->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (rot_img->library_owns_data)
        rot_img->step = imageWidth * PIXEL_RGBX;
    rot_img->sequence = 0;
    rot_img->capture_time = rgb->capture_time;
    rot_img->source = devh;
    int src_stride_argb = imageWidth * 4;
    uint* dst_argb = rot_img->data;
    int src_width = imageWidth;
    int src_height = imageHeight;

    if (rotation == 90) {
        rot_img->width = imageHeight ;
        rot_img->height = imageWidth;
        int dst_stride_argb = imageHeight * 4;
        enum RotationMode mode = kRotate90;
        ARGBRotate(rgb->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgb);
        return checkFlip(rot_img);
    } else if (rotation == 180) {
        rot_img->width = imageWidth;
        rot_img->height = imageHeight;
        int dst_stride_argb = imageWidth * 4;
        enum RotationMode mode = kRotate180;
        ARGBRotate(rgb->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgb);
        return checkFlip(rot_img);
    } else if (rotation == 270) {
        rot_img->width = imageHeight ;
        rot_img->height = imageWidth;
        int dst_stride_argb = imageHeight * 4;
        enum RotationMode mode = kRotate270;
        ARGBRotate(rgb->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgb);
        return checkFlip(rot_img);
    }
    return rot_img;
}



void initStreamingParms_controltransfer(libusb_device_handle *handle, bool createPointer) {
    LOGD("bool createPointer = %d", createPointer);
    size_t length;
    if (bcdUVC >= 0x0150)
        length = 48;
    else if (bcdUVC >= 0x0110)
        length = 34;
    else
        length = 26;
    LOGD("length = %d", length);
    if (createPointer == true) {
        ctl_transfer_Data = malloc(sizeof *ctl_transfer_Data + sizeof(unsigned char[48*4]));
        ctl_transfer_Data->BufferSize = 48 * 4;
        memset(ctl_transfer_Data->ctl_transfer_values, 0, sizeof(unsigned char) * (ctl_transfer_Data->BufferSize));
    }
    uint8_t buffer[length];
    for (i = 0; i < length; i++) {
        buffer[i] = 0x00;
    }
    buffer[0] = bmHint; // what fields shall be kept fixed (0x01: dwFrameInterval)
    buffer[1] = 0x00; //
    buffer[2] = camFormatIndex; // video format index
    buffer[3] = camFrameIndex; // video frame index
    buffer[4] = (camFrameInterval & 0xFF); // interval
    buffer[5] = ((camFrameInterval >> 8)& 0xFF); //   propose:   0x4c4b40 (500 ms)
    buffer[6] = ((camFrameInterval >> 16)& 0xFF); //   agreement: 0x1312d0 (125 ms)
    buffer[7] = ((camFrameInterval >> 24)& 0xFF); //
    int b;
/*
    LOGD("Wanted Streaming Pharms:           ");
    for (int i = 0; i < sizeof(buffer); i++)
        if (buffer[i] != 0){
            LOGD("[%d ] ", buffer[i]);}
*/

    if (createPointer == true) {
        memcpy(ctl_transfer_Data->ctl_transfer_values, buffer , length);
    }
    getStreamingParmsArray(initStreamingParmsIntArray , buffer);
    // wanted Pharms
    LOGD("initStreamingParmsIntArray[0] = %d", initStreamingParmsIntArray[0]);
    LOGD("initStreamingParmsIntArray[1] = %d", initStreamingParmsIntArray[1]);
    LOGD("initStreamingParmsIntArray[2] = %d", initStreamingParmsIntArray[2]);
    int len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_SET, SET_CUR, (VS_PROBE_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
    if (len != sizeof (buffer)) {
        LOGD("\nCamera initialization failed. Streaming parms probe set failed, len= %d.\n", len);
    } else {
        LOGD("1st: InitialContolTransfer Sucessful");
        LOGD("Camera initialization success, len= %d.\n", len);
    }
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (VS_PROBE_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 500);
    if (len != sizeof (buffer)) {
        LOGD("Camera initialization failed. Streaming parms probe set failed, len= %d.\n", len);
        if (createPointer == true) {
            memset(ctl_transfer_Data->ctl_transfer_values + 47, 0, length);
        }
    } else {
        if (createPointer == true) {
            memcpy(ctl_transfer_Data->ctl_transfer_values + 47, buffer, length);
        }
        LOGD ("2nd: CTL suressful");
    }

    getStreamingParmsArray(probedStreamingParmsIntArray , buffer);
/*
    LOGD("probedStreamingParmsIntArray[0] = %d", probedStreamingParmsIntArray[0]);
    LOGD("probedStreamingParmsIntArray[1] = %d", probedStreamingParmsIntArray[1]);
    LOGD("probedStreamingParmsIntArray[2] = %d", probedStreamingParmsIntArray[2]);

    LOGD("Probed Streaming Pharms:       ");
    for (int i = 0; i < sizeof(buffer); i++)
        if (buffer[i] != 0){
            LOGD("[%d ] ", buffer[i]);}
*/
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_SET, SET_CUR, (VS_COMMIT_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
    if (len != sizeof (buffer)) {
        LOGD("FAILED -- 3rd CTL failed");
        LOGD("Camera initialization failed. Streaming parms commit set failed, len= %d.", len);
        if (createPointer == true) {
            memset(ctl_transfer_Data->ctl_transfer_values + 95, 0, length);
        }
    } else {
        if (createPointer == true) {
            memcpy(ctl_transfer_Data->ctl_transfer_values + 95, buffer, length);
        }
        LOGD("3rd: CTL Sucessful");
    }
    getStreamingParmsArray(finalStreamingParmsIntArray_first , buffer);
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (short) (VS_COMMIT_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
    if (len != sizeof (buffer)) {
        LOGD("ERROR 4th CTL FAILED");
        LOGD("Camera initialization failed. Streaming parms commit get failed, len= %d.", len);
        if (createPointer == true) {
            memset(ctl_transfer_Data->ctl_transfer_values + 143, 0, length);
        }
    } else {
        LOGD("4th: FinalCTL Sucessful");
        if (createPointer == true) {
            memcpy(ctl_transfer_Data->ctl_transfer_values + 143, buffer, length);
        }
    }
    getStreamingParmsArray(finalStreamingParmsIntArray , buffer);
}

void print_endpoint(const struct libusb_endpoint_descriptor *endpoint, int bInterfaceNumber) {
    int i, ret;
    if (bInterfaceNumber == 1) {
        streamEndPointAdressOverNative = endpoint->bEndpointAddress;
    }
    LOGD("        wMaxPacketSize:   %d\n", endpoint->wMaxPacketSize);
    fflush(stdout);
}

void print_altsetting(const struct libusb_interface_descriptor *interface) {
    uint8_t i;

    if (interface->bInterfaceSubClass == SC_VIDEOCONTROL) {
        int camControlInterfaceNum = interface->bInterfaceNumber;

    }

    if (interface->bInterfaceSubClass == SC_VIDEOSTREAMING) {
        camStreamingInterfaceNum = interface->bInterfaceNumber;

    }
    LOGD("  interface->bNumEndpoints = %d\n", interface->bNumEndpoints);

    for (i = 0; i < interface->bNumEndpoints; i++)
        print_endpoint(&interface->endpoint[i], interface->bInterfaceNumber);
}

void print_interface(const struct libusb_interface *interface) {
    int i;
    LOGD("  interface->num_altsetting = %d\n", interface->num_altsetting);
    for (i = 0; i < interface->num_altsetting; i++)
        print_altsetting(&interface->altsetting[i]);
}

void print_configuration(struct libusb_config_descriptor *config) {
    uint8_t i;

    LOGD("  Configuration:\n");
    LOGD("    wTotalLength:         %d\n", config->wTotalLength);
    LOGD("    bNumInterfaces:       %d\n", config->bNumInterfaces);
    LOGD("    bConfigurationValue:  %d\n", config->bConfigurationValue);
    LOGD("    iConfiguration:       %d\n", config->iConfiguration);
    LOGD("    bmAttributes:         %02xh\n", config->bmAttributes);
    LOGD("    MaxPower:             %d\n\n", config->MaxPower);
    fflush(stdout);


    for (i = 0; i < config->bNumInterfaces; i++)
        // printf("                 Interface Nummer:       %d\n\n", i);
        print_interface(&config->interface[i]);
    fflush(stdout);

}

int print_device(libusb_device *dev, int level, libusb_device_handle *handle, struct libusb_device_descriptor desc) {

    char description[256];
    char string[256];
    int ret;
    uint8_t i;
    LOGD("Print Device");
    verbose = 1;
    ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0) {
        LOGD(stderr, "failed to get device descriptor");
        return -1;
    }

        LOGD("\nKamera gefunden\n");
        fflush(stdout);
        ret = libusb_open(dev, &handle);
        if (LIBUSB_SUCCESS == ret) {
            if (desc.iManufacturer) {
                ret = libusb_get_string_descriptor_ascii(handle, desc.iManufacturer, string, sizeof (string));
                if (ret > 0) {
                    snprintf(description, sizeof (description), "%s - ", string);
                } else
                    snprintf(description, sizeof (description), "%04X - ", desc.idVendor);
            } else
                snprintf(description, sizeof (description), "%04X - ",
                         desc.idVendor);
            if (desc.iProduct) {
                ret = libusb_get_string_descriptor_ascii(handle, desc.iProduct, string, sizeof (string));
                if (ret > 0)
                    snprintf(description + strlen(description), sizeof (description) -
                                                                strlen(description), "%s", string);
                else
                    snprintf(description + strlen(description), sizeof (description) -
                                                                strlen(description), "%04X", desc.idProduct);
            } else
                snprintf(description + strlen(description), sizeof (description) -
                                                            strlen(description), "%04X", desc.idProduct);
        } else {
            snprintf(description, sizeof (description), "%04X - %04X",
                     desc.idVendor, desc.idProduct);
        }
        LOGD("%.*sDev (bus %d, device %d): %s\n", level * 2, "                    ", libusb_get_bus_number(dev), libusb_get_device_address(dev), description);
        if (handle && verbose) {
            if (desc.iSerialNumber) {
                ret = libusb_get_string_descriptor_ascii(handle, desc.iSerialNumber, string, sizeof (string));
                if (ret > 0)
                    LOGD("%.*s  - Serial Number: %s\n", level * 2,
                           "                    ", string);
            }
        }
        if (verbose) {
            for (i = 0; i < desc.bNumConfigurations; i++) {
                struct libusb_config_descriptor *config;
                ret = libusb_get_config_descriptor(dev, i, &config);
                if (LIBUSB_SUCCESS != ret) {
                    LOGD("  Couldn't retrieve descriptors\n");
                    continue;
                }
                print_configuration(config);
                libusb_free_config_descriptor(config);
            }
        }
        return 0;
}

int wrap_camera_device(int FD) {
    int ret;
    enum libusb_error rc;
    rc = libusb_set_option(&ctx, LIBUSB_OPTION_WEAK_AUTHORITY, NULL);
    if (rc != LIBUSB_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,"libusb_init failed: %d\n", ret);
        return -1;
    }
    ret = libusb_init(&ctx);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_init failed: %d\n", ret);
        return -1;
    }
    ret = libusb_wrap_sys_device(NULL, (intptr_t)FD, &devh);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device failed: %d\n", ret);
        return -2;
    }
    else if (devh == NULL) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device returned invalid handle\n");
        return -3;
    }
    cameraDevice_is_wraped = true;
    return 0;
}

int libUsb_open_def_fd(int vid, int pid, const char *serial, int FD, int busnum, int devaddr) {
    unsigned char data[64];
    if (!cameraDevice_is_wraped) {
        if (wrap_camera_device(FD) == 0) LOGD("Successfully wraped The CameraDevice");
    }
    for (int if_num = 0; if_num < (camStreamingInterfaceNum + 1); if_num++) {
        if (libusb_kernel_driver_active(devh, if_num)) {
            libusb_detach_kernel_driver(devh, if_num);
        }
        int rc = libusb_claim_interface(devh, if_num);
        if (rc < 0) {
            LOGD(stderr, "Error claiming interface: %s\n",
                    libusb_error_name(rc));
        } else {
            LOGD("Interface %d erfolgreich eingehängt;\n", if_num);
        }
    }
    LOGD("Print Device");
    struct libusb_device_descriptor desc;
    print_device(libusb_get_device(devh) , 0, devh, desc);

    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum, 0); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Altsettings sucessfuly set! %d ; Altsetting = 0\n", r);
    }
    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "libusb_control_transfer start\n");
    initStreamingParms(devh);
    __android_log_print(ANDROID_LOG_INFO, TAG, "devh: %p\n", devh);
    LOGD("Exit");
    return 0;
}


/** @internal
* @brief Find the descriptor for a specific frame configuration
* @param stream_if Stream interface
* @param format_id Index of format class descriptor
* @param frame_id Index of frame descriptor
*/

void exit() {
    //uvc_exit(globalUVCContext);
    initialized = false;
}



int set_the_native_Values (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
           int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpointAdress, int camStreamingInterfaceNumber,
           const char* frameformat, int numberOfAutoFrame, int bcdUVC_int) {
    fd = FD;
    numberOfAutoFrames = numberOfAutoFrame;
    packetsPerRequest = packetsPerReques;
    maxPacketSize = maxPacketSiz;
    activeUrbs = activeUrb;
    camStreamingAltSetting = camStreamingAltSettin;
    camFormatIndex = camFormatInde;
    camFrameIndex = camFrameInde;
    camFrameInterval = camFrameInterva;
    camStreamingEndpoint = camStreamingEndpointAdress;
    camStreamingInterfaceNum = camStreamingInterfaceNumber;
    imageWidth = imageWidt;
    imageHeight = imageHeigh;
    frameFormat = frameformat;
    mUsbFs = mUsbFs;
    vendorID = vendorID;
    productID = productID;
    busnum = busnum;
    devaddr = devaddr;
    videoFrameData = malloc(sizeof *videoFrameData + sizeof(char[imageWidt*imageHeigh*2]));
    videoFrameData->FrameSize = imageWidt * imageHeigh;
    videoFrameData->FrameBufferSize = videoFrameData->FrameSize * 2;
    bcdUVC = bcdUVC_int;
    LOGD("bcdUVC = %d", bcdUVC);

    initialized = true;
    result ++;
    return result;
}


bool compareArrays(int a[], int b[]) {
    if(memcmp(a, b, sizeof(a)) == 0) return true;
    return false;
}

bool compareStreamingParmsValues() {
    if ( !compareArrays( initStreamingParmsIntArray, probedStreamingParmsIntArray ) || !compareArrays( initStreamingParmsIntArray, finalStreamingParmsIntArray_first )  )  {
        if (initStreamingParmsIntArray[0] != finalStreamingParmsIntArray_first[0]) {
            LOGD("The Controltransfer returned differnt Format Index's\n\n");
            LOGD("Your entered 'Camera Format Index' Values is: %d", initStreamingParmsIntArray[0]);
            LOGD("The 'Camera Format Index' from the Camera Controltransfer is: %d", finalStreamingParmsIntArray_first[0]);
        }
        if (initStreamingParmsIntArray[1] != finalStreamingParmsIntArray_first[1]) {
            LOGD("The Controltransfer returned differnt Frame Index's\n\n");
            LOGD("Your entered 'Camera Frame Index' Values is: " + initStreamingParmsIntArray[1], "\n");
            LOGD("The 'Camera Frame Index' from the Camera Controltransfer is: %d" , finalStreamingParmsIntArray_first[1] );
        }
        if (initStreamingParmsIntArray[2] != finalStreamingParmsIntArray_first[2]) {
            LOGD("The Controltransfer returned differnt FrameIntervall Index's\n\n");
            LOGD("Your entered 'Camera FrameIntervall' Values is: %d", initStreamingParmsIntArray[2] );
            LOGD("The 'Camera FrameIntervall' Value from the Camera Controltransfer is: %d", finalStreamingParmsIntArray_first[2] );
        }
        LOGD("The Values for the Control Transfer have a grey color in the 'edit values' screen");
        LOGD("To get the correct values for you camera, read out the UVC specifications of the camera manualy, or try out the 'Set Up With UVC Settings' Button");
        LOGD ("compareStreamingParmsValues returned false");
        return false;
    } else {
        LOGD("Camera Controltransfer Sucessful !\n\nThe returned Values from the Camera Controltransfer fits to your entered Values\nYou can proceed starting a test run!");
        return true;
    }
    return 0;
}

int initStreamingParms(int FD) {
    if (!camIsOpen) {
        int ret;
        enum libusb_error rc;
        unsigned char data[64];
        if (!cameraDevice_is_wraped) {
            if (wrap_camera_device(FD) == 0) LOGD("Successfully wraped The CameraDevice");
        }
    }
    for (int if_num = 0; if_num < (camStreamingInterfaceNum + 1); if_num++) {
        if (libusb_kernel_driver_active(devh, if_num)) {
            libusb_detach_kernel_driver(devh, if_num);
        }
        int rc = libusb_claim_interface(devh, if_num);
        if (rc < 0) {
            fprintf(stderr, "Error claiming interface: %s\n",
                    libusb_error_name(rc));
        } else {
            printf("Interface %d erfolgreich eingehängt;\n", if_num);
        }
    }
    struct libusb_device_descriptor desc;
    print_device(libusb_get_device(devh) , 0, devh, desc);
    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum, 0); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        return -4;
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = 0\n", r);
    }
    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "libusb_control_transfer start\n");
    initStreamingParms_controltransfer(devh, false);
    camIsOpen = compareStreamingParmsValues();
    if (camIsOpen) return 0;
    else return -4;
}

int fetchTheCamStreamingEndpointAdress (int FD) {
    if (!camIsOpen) {
        int ret;
        enum libusb_error rc;
        if (!cameraDevice_is_wraped) {
            if (wrap_camera_device(FD) == 0) LOGD("Successfully wraped The CameraDevice");
        }
        camIsOpen = true;
    }
    for (int if_num = 0; if_num < (2); if_num++) {
        if (libusb_kernel_driver_active(devh, if_num)) {
            libusb_detach_kernel_driver(devh, if_num);
        }
        int rc = libusb_claim_interface(devh, if_num);
        if (rc < 0) {
            fprintf(stderr, "Error claiming interface: %s\n",
                    libusb_error_name(rc));
        } else {
            printf("Interface %d erfolgreich eingehängt;\n", if_num);
        }
    }
    struct libusb_device_descriptor desc;
    print_device(libusb_get_device(devh) , 0, devh, desc);
    return streamEndPointAdressOverNative;
}

void setImageCapture() {
    imageCapture = true;
}

unsigned char * probeCommitControl(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva, int FD) {
    bmHint = bmHin;
    camFormatIndex = camFormatInde;
    camFrameIndex = camFrameInde;
    camFrameInterval = camFrameInterva;
    if (!camIsOpen) {
        int ret;
        enum libusb_error rc;
        if (!cameraDevice_is_wraped) {
            if (wrap_camera_device(FD) == 0) LOGD("Successfully wraped The CameraDevice");
        }
        camIsOpen = true;
    }
    for (int if_num = 0; if_num < (camStreamingInterfaceNum + 1); if_num++) {
        if (libusb_kernel_driver_active(devh, if_num)) {
            libusb_detach_kernel_driver(devh, if_num);
        }
        int rc = libusb_claim_interface(devh, if_num);
        if (rc < 0) {
            fprintf(stderr, "Error claiming interface: %s\n",
                    libusb_error_name(rc));
        } else {
            printf("Interface %d erfolgreich eingehängt;\n", if_num);
        }
    }
    struct libusb_device_descriptor desc;
    print_device(libusb_get_device(devh) , 0, devh, desc);
    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum, 0); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        return NULL;
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = 0\n", r);
    }

    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "libusb_control_transfer start\n");

    initStreamingParms_controltransfer(devh, true);
    camIsOpen = compareStreamingParmsValues();
    if (camIsOpen) return ctl_transfer_Data;
    else return ctl_transfer_Data;
}

void isoc_transfer_completion_handler_automaticdetection(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    autoStruct.requestCnt ++;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            autoStruct.packetCnt ++;
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            // packet only contains an acknowledge?
            if (packetLen == 0) {
                autoStruct.packet0Cnt++;
            }
            if (packetLen == 12) {
                autoStruct.packet12Cnt++;
            }
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                autoStruct.packetErrorCnt ++;
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    LOGD(stderr, "Die Framegröße musste gekürzt werden.\n");
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            autoStruct.frameLen += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                autoStruct.sframeLenArray[autoStruct.frameCnt] = autoStruct.frameLen;
                LOGD("Frame received");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;

                    if (total < videoFrameData->FrameBufferSize) {
                        LOGD(stderr, "insufficient frame data.\n");
                    }
                    LOGD("Länge des Frames = %d\n", total);
                    autoStruct.frameCnt ++;

                    if (numberOfAutoFrames == totalFrame) {
                        LOGD("calling autoStreamfinished");
                        runningStream = false;
                        autoStreamfinished();
                    }


                    total = 0;
                    autoStruct.frameLen = 0;
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "Die Übertragung ist gescheitert. \n");
        }
}

void startAutoDetection () {
    if (camIsOpen) {
        totalFrame = 0;
        int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum, camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
        if (r != LIBUSB_SUCCESS) {
            LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        } else {
            LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r, camStreamingAltSetting);
        }
        autoStruct.requestCnt = 0;
        autoStruct.frameCnt = 0;
        autoStruct.frameLen = 0;
        autoStruct.packet0Cnt= 0;
        autoStruct.packet12Cnt= 0;
        autoStruct.packetCnt= 0;
        autoStruct.packetDataCnt= 0;
        autoStruct.packetErrorCnt= 0;
        autoStruct.packetHdr8Ccnt= 0;
        for(int ii = 0; ii < 5; ii++) autoStruct.sframeLenArray[ii] = 0;
        // ------------------------------------------------------------
        // do an isochronous transfer
        struct libusb_transfer * xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);

            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize*packetsPerRequest, packetsPerRequest,
                    isoc_transfer_completion_handler_automaticdetection, NULL, 0);

            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);

            for (int j = 0; j < packetsPerRequest; j++) {
                xfers[i]->iso_packet_desc[j].status = -1;
            }

        }
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                fprintf(stderr, "submit xfer failed.\n");
            }
        }
        runningStream = true;
        while (runningStream) {
            if (runningStream == false) {
                break;
            }
            libusb_handle_events(ctx);
        }
    }
}

void closeLibUsb() {
    libusb_set_interface_alt_setting(devh,1,0);
    libusb_release_interface(devh, 0);
    libusb_release_interface(devh, 1);
    //libusb_close(devh);
    //libusb_exit(ctx);
    //close(fd);
}


void probeCommitControl_cleanup()
{
    free(ctl_transfer_Data->ctl_transfer_values);
    LOGD("probeCommitControl_cleanup Complete");
}

void stopJavaVM() {
    (*javaVm)->DetachCurrentThread(javaVm);
}


void stopStreaming() {
    LOGD("native stopStreaming");
    runningStream = false;
    /*
    for (i = 0; i < activeUrbs; i++) {
        int res = libusb_cancel_transfer(xfers[i]);
        if ((res < 0) && (res != LIBUSB_ERROR_NOT_FOUND)) {
            LOGD("libusb_cancel_transfer failed");
        } else LOGD("libusb_cancel_transfer sucess");
    }
     */
    //libusb_interrupt_event_handler(ctx);
    //LOGD("handle Events interrupted");
    //libusb_release_interface(devh, camStreamingInterfaceNum);

    //libusb_release_interface(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber);
}

void isoc_transfer_completion_handler_five_sec(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    manualFrameStruct.requestCnt ++;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            //manualFrameStruct.packetCnt ++;
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            // packet only contains an acknowledge?
            if (packetLen == 0) {
                manualFrameStruct.packet0Cnt++;
            }
            if (packetLen == 12) {
                //manualFrameStruct.packet12Cnt++;
            }
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                //manualFrameStruct.packetErrorCnt ++;
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    LOGD(stderr, "Die Framegröße musste gekürzt werden.\n");
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            //manualFrameStruct.frameLen += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                //manualFrameStruct.sframeLenArray[manualFrameStruct.frameCnt] = manualFrameStruct.frameLen;
                LOGD("Frame received_5_frame");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (total < videoFrameData->FrameBufferSize) {
                        LOGD(stderr, "insufficient frame data.\n");
                    }
                    LOGD("Länge des Frames = %d\n", total);
                    //manualFrameStruct.frameCnt ++;
                    LOGD("calling sendReceivedDataToJava");
                    //runningStream = false;
                    sendReceivedDataToJava(videoFrameData, total);
                    total = 0;
                    //manualFrameStruct.frameLen = 0;
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
                clock_t now = clock();
                float seconds = (float)(now - startTime) / CLOCKS_PER_SEC;
                LOGD("%.f seconds since the Stream is running.\n", seconds);

                if(seconds >= 5) {
                    LOGD ("5 sec");
                    runningStream = false;
                }
                else if(seconds >= 4) LOGD ("4 sec");
                else if(seconds >= 3) LOGD ("3 sec");
                else if(seconds >= 2) LOGD ("2 sec");
                else if(seconds >= 1) LOGD ("1 sec");
                else LOGD ("0 sec");

            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "Die Übertragung ist gescheitert. \n");
        }
}

void isoc_transfer_completion_handler_one_frame(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    manualFrameStruct.requestCnt ++;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            manualFrameStruct.packetCnt ++;
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            // packet only contains an acknowledge?
            if (packetLen == 0) {
                manualFrameStruct.packet0Cnt++;
            }
            if (packetLen == 12) {
                manualFrameStruct.packet12Cnt++;
            }
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                manualFrameStruct.packetErrorCnt ++;
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    LOGD(stderr, "Die Framegröße musste gekürzt werden.\n");
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            manualFrameStruct.frameLen += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                manualFrameStruct.sframeLenArray[manualFrameStruct.frameCnt] = manualFrameStruct.frameLen;
                LOGD("Frame received_one_Frame");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (total < videoFrameData->FrameBufferSize) {
                        LOGD(stderr, "insufficient frame data.\n");
                    }
                    LOGD("Länge des Frames = %d\n", total);
                    manualFrameStruct.frameCnt ++;
                    LOGD("calling sendReceivedDataToJava");
                    runningStream = false;
                    sendReceivedDataToJava(videoFrameData, total);



                    total = 0;
                    manualFrameStruct.frameLen = 0;
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "Die Übertragung ist gescheitert. \n");
        }
}

void clearManualFrameStruct() {
    manualFrameStruct.frameCnt = 0;
    manualFrameStruct.frameLen = 0;
    for (i = 0; i < 5; i++) {
        manualFrameStruct.sframeLenArray[i] = 0;
    }
    manualFrameStruct.packet0Cnt = 0;
    manualFrameStruct.packetHdr8Ccnt = 0;
    manualFrameStruct.requestCnt = 0;
    manualFrameStruct.packetErrorCnt = 0;
    manualFrameStruct.packetDataCnt = 0;
    manualFrameStruct.packetCnt = 0;
    manualFrameStruct.packet12Cnt = 0;
}


void isoc_transfer_completion_handler_stream(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    LOGD(stderr, "Die Framegröße musste gekürzt werden.\n");
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                LOGD("IsoStream over JNA --> Frame received");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (total < videoFrameData->FrameBufferSize) {
                        LOGD(stderr, "insufficient frame data.\n");
                    }
                    //LOGD("Länge des Frames = %d\n", total);
                    //LOGD("calling sendReceivedDataToJava");
                    jnaSendFrameToJava(videoFrameData, total);
                    total = 0;
                    runningStream = false;
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "Die Übertragung ist gescheitert. \n");
        }
}

void getFramesOverLibUsb( int yuvFrameIsZero, int stream, int whichTestrun) {
    clearManualFrameStruct();
    int requestMode = 0;
    probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);
    //probeCommitControl_cleanup();
    LOGD("ISO Stream");
    runningStream = true;
    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                             camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r,
             camStreamingAltSetting);
    }
    if (activeUrbs > 16) activeUrbs = 16;
    struct libusb_transfer *xfers[activeUrbs];
    if(stream == 1 || whichTestrun == 0) {
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    isoc_transfer_completion_handler_stream, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
    }
    if (whichTestrun == 1){
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    isoc_transfer_completion_handler_one_frame, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
    } else if (whichTestrun == 5){
        startTime = clock();  /* set current time;  */
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    isoc_transfer_completion_handler_five_sec, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
    }

    runningStream = true;
    for (i = 0; i < activeUrbs; i++) {
        if (libusb_submit_transfer(xfers[i]) != 0) {
            LOGD(stderr, "submit xfer failed.\n");
        }
    }
    runningStream = true;
    while (runningStream) {
        if (runningStream == false) {
            LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
            break;
        }
        libusb_handle_events(ctx);
    }
}

//////////////////////// LIB UVC FUNCTIONS

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


///////////////////// ACTIVITY FUNCTIONS

void cb_jni_stream_ImageView(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                LOGD("JniIsoStream --> Frame received");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (runningStream == false) stopStreaming();

                    JNIEnv * jenv;
                    int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                    jbyteArray array = (*jenv)->NewByteArray(jenv, videoFrameData->FrameBufferSize);
                    // Main Activity
                    (*jenv)->SetByteArrayRegion(jenv, array, 0, videoFrameData->FrameBufferSize, (jbyte *) videoFrameData->videoframe);
                    (*jenv)->CallVoidMethod(jenv, mainActivityObj, javainitializeStreamArray, array);
                    total = 0;
                    runningStream = false;


                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "SUBMISSION FAILED. \n");
        }
}

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetTheMethodsStreamActivityMJPEG
        (JNIEnv *env, jobject obj) {
    int status = (*env)->GetJavaVM(env, &javaVm);
    if(status != 0) {
        LOGE("failed to attach javaVm");
    }
    class = (*env)->GetObjectClass(env, obj);
    jniHelperClass = (*env)->NewGlobalRef(env, class);
    mainActivityObj = (*env)->NewGlobalRef(env, obj);
    javaRetrievedStreamActivityFrameFromLibUsb = (*env)->GetMethodID(env, jniHelperClass, "retrievedStreamActivityFrameFromLibUsb", "([B)V");
}


JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivity
        (JNIEnv *env, jobject obj, jint stream, jint frameIndex) {
    if (initialized) {
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);
        //probeCommitControl_cleanup();
        LOGD("ISO Stream");
        int status = (*env)->GetJavaVM(env, &javaVm);
        if(status != 0) {
            LOGE("failed to attach javaVm");
        }
        class = (*env)->GetObjectClass(env, obj);
        jniHelperClass = (*env)->NewGlobalRef(env, class);
        mainActivityObj = (*env)->NewGlobalRef(env, obj);
        javainitializeStreamArray = (*env)->GetMethodID(env, jniHelperClass, "initializeStreamArray", "([B)V");
        //javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, jniHelperClass, "processReceivedMJpegVideoFrameKamera", "([B)V");
        //javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, jniHelperClass, "processReceivedVideoFrameYuvFromJni", "([B)V");
        runningStream = true;
        int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                                 camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
        if (r != LIBUSB_SUCCESS) {
            LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        } else {
            LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r,
                 camStreamingAltSetting);
        }
        if (activeUrbs > 16) activeUrbs = 16;
        struct libusb_transfer *xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    cb_jni_stream_ImageView, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
        runningStream = true;
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                LOGD(stderr, "submit xfer failed.\n");
            }
        }
        runningStream = true;
        while (runningStream) {
            if (runningStream == false) {
                LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
                break;
            }
            libusb_handle_events(ctx);
        }
        LOGD("ISO Stream Start -- finished");
    }
}

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniGetAnotherFrame
        (JNIEnv *env, jobject obj, jint stream, jint frameIndex) {
    if (initialized) {
        struct libusb_transfer *xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    cb_jni_stream_ImageView, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
        runningStream = true;
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                LOGD(stderr, "submit xfer failed.\n");
            }
        }
        runningStream = true;
        while (runningStream) {
            if (runningStream == false) {
                LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
                break;
            }
            libusb_handle_events(ctx);
        }
        LOGD("ISO Stream Start -- finished");
    }
}

void cb_jni_stream_Surface_Activity(struct libusb_transfer *the_transfer) {
    if (runningStream == false) stopStreaming();
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                LOGD("Frame received_Surface_Act");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (runningStream == false) stopStreaming();
                    total = 0;
                    uvc_frame_t *rgb;
                    uvc_error_t ret;
                    // We'll convert the image from YUV/JPEG to BGR, so allocate space
                    rgb = uvc_allocate_frame(imageWidth * imageHeight * 4);
                    if (!rgb) {
                        printf("unable to allocate rgb frame!");
                        return;
                    }
                    // Do the BGR conversion
                    ret = uvc_yuyv2rgbx(rgb);
                    if (ret) {
                        uvc_perror(ret, "uvc_any2bgr");
                        uvc_free_frame(rgb);
                        return;
                    }
                    copyToSurface(rgb, &mCaptureWindow);
                    uvc_free_frame(rgb);
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "SUBMISSION FAILED. \n");
        }
}

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivitySurface
        (JNIEnv *env, jobject obj, jobject jSurface, jint stream, jint frameIndex) {

    ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
    // WINDOW_FORMAT_RGBA_8888
    ANativeWindow_setBuffersGeometry(preview_window, imageWidth, imageHeight, WINDOW_FORMAT_RGBA_8888);
    mCaptureWindow = preview_window;
    probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);
    if (initialized) {
        int status = (*env)->GetJavaVM(env, &javaVm);
        if(status != 0) {
            LOGE("failed to attach javaVm");
        }
        class = (*env)->GetObjectClass(env, obj);
        jniHelperClass = (*env)->NewGlobalRef(env, class);
        mainActivityObj = (*env)->NewGlobalRef(env, obj);
        //javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, jniHelperClass, "processReceivedMJpegVideoFrameKamera", "([B)V");
        //javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, jniHelperClass, "processReceivedVideoFrameYuvFromJni", "([B)V");
        int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                                 camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
        if (r != LIBUSB_SUCCESS) {
            LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        } else {
            LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r,
                 camStreamingAltSetting);
        }
        if (activeUrbs > 16) activeUrbs = 16;
        struct libusb_transfer *xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    cb_jni_stream_Surface_Activity, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                LOGD(stderr, "submit xfer failed.\n");
            }
        }
        runningStream = true;
        while (runningStream) {
            if (runningStream == false) {
                LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
                break;
            }
            libusb_handle_events(ctx);
        }
        LOGD("ISO Stream Start -- finished");
    }
}

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetSurfaceYuv
        (JNIEnv *env, jobject obj, jobject jSurface) {
    ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
    // WINDOW_FORMAT_RGBA_8888
    ANativeWindow_setBuffersGeometry(preview_window, imageWidth, imageHeight, WINDOW_FORMAT_RGBA_8888);
    mCaptureWindow = preview_window;
    int status = (*env)->GetJavaVM(env, &javaVm);
    if(status != 0) {
        LOGE("failed to attach javaVm");
    }
    class = (*env)->GetObjectClass(env, obj);
    jniHelperClass = (*env)->NewGlobalRef(env, class);
    mainActivityObj = (*env)->NewGlobalRef(env, obj);
    javaRetrievedStreamActivityFrameFromLibUsb = (*env)->GetMethodID(env, jniHelperClass, "retrievedStreamActivityFrameFromLibUsb", "([B)V");
    javaPicturCapture  = (*env)->GetMethodID(env, jniHelperClass, "picturCapture", "([B)V");
}

//////////// SERVICE

// Init SurfaceView for Service
JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniPrepairForStreamingfromService
        (JNIEnv *env, jobject obj) {
    if (initialized) {
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);
        //probeCommitControl_cleanup();
        /*
       LOGD("ISO Stream");
       int status = (*env)->GetJavaVM(env, &javaVm);
       if(status != 0) {
           LOGE("failed to attach javaVm");
       }

       class = (*env)->GetObjectClass(env, obj);
       jniHelperClass = (*env)->NewGlobalRef(env, class);
       mainActivityObj = (*env)->NewGlobalRef(env, obj);
       javaServicePublishResults = (*env)->GetMethodID(env, jniHelperClass, "publishResults", "([B)V");
       javaServiceReturnToStreamActivity = (*env)->GetMethodID(env, jniHelperClass, "returnToStreamActivity", "()V");;

         */

        runningStream = true;
        int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                                 camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
        if (r != LIBUSB_SUCCESS) {
            LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
        } else {
            LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r,
                 camStreamingAltSetting);
        }
        if (activeUrbs > 16) activeUrbs = 16;
    }
}






void cb_jni_stream_Surface_Service(struct libusb_transfer *the_transfer) {
    if (runningStream == false) stopStreaming();
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            if (packetLen == 0) {
                continue;
            }
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                //LOGD("Frame received from Surface Callback Service");
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (runningStream == false) stopStreaming();
                    //LOGD("frameFormat = %s", frameFormat);
                    if (strcmp(frameFormat, "MJPEG") == 0) {
                        JNIEnv * jenv;
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                        // Main Activity
                        (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaRetrievedStreamActivityFrameFromLibUsb, array);
                        total = 0;
                        /*
                        uvc_frame_t *rgb;
                        uvc_error_t ret;
                        rgb = uvc_allocate_frame(imageWidth * imageHeight * 4);
                        rgb->width = imageWidth;
                        rgb->height = imageHeight;
                        rgb->frame_format = UVC_FRAME_FORMAT_RGBX;
                        if (rgb->library_owns_data)
                            rgb->step = imageWidth * PIXEL_RGBX;
                        rgb->sequence = 0;
                        gettimeofday(&rgb->capture_time, NULL);
                        rgb->source = devh;
                        MJPGToARGB (videoFrameData->videoframe, rgb->width * rgb->height * 3,
                                    rgb->data, imageWidth * 4,
                                    rgb->width, rgb->height,
                                    rgb->width, rgb->height
                        );
                        ret = uvc_yuyv2rgbx(rgb);
                        if (ret) {
                            uvc_perror(ret, "uvc_any2bgr");
                            uvc_free_frame(rgb);
                            return;
                        }
                        uvc_frame_t *rgb_rot_nFlip = checkRotation(rgb);
                        copyToSurface(rgb_rot_nFlip, &mCaptureWindow);
                        uvc_free_frame(rgb_rot_nFlip);

                        /*
                         *  MJPGToARGB(const uint8* sample,
                        size_t sample_size,
                        uint8* argb, int argb_stride,
                        int w, int h,
                        int dw, int dh)
                         */
                    } else if (strcmp(frameFormat, "YUY2") == 0) {
                        //LOGD("YUY2");
                        uvc_frame_t *rgb;
                        uvc_error_t ret;
                        // We'll convert the image from YUV/JPEG to BGR, so allocate space
                        rgb = uvc_allocate_frame(imageWidth * imageHeight * 4);
                        if (!rgb) {
                            printf("unable to allocate rgb frame!");
                            return;
                        }
                        // Do the BGR conversion
                        ret = uvc_yuyv2rgbx(rgb);
                        if (ret) {
                            uvc_perror(ret, "uvc_any2bgr");
                            uvc_free_frame(rgb);
                            return;
                        }
                        uvc_frame_t *rgb_rot_nFlip = checkRotation(rgb);

                        if (imageCapture) {


                            imageCapture = false;
                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPicturCapture, array);
                        }
                        copyToSurface(rgb_rot_nFlip, &mCaptureWindow);
                        uvc_free_frame(rgb_rot_nFlip);
                        total = 0;
                    }
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "SUBMISSION FAILED. \n");
        }
}



JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniServiceOverSurface
        (JNIEnv *env, jobject obj) {
    if (initialized) {

        struct libusb_transfer *xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    cb_jni_stream_Surface_Service, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
        runningStream = true;
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                LOGD(stderr, "submit xfer failed.\n");
            }
        }
        while (runningStream) {
            if (runningStream == false) {
                LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
                break;
            }
            libusb_handle_events(ctx);
        }
        LOGD("ISO Stream Start -- finished");
    }
}




//////////////////////// Webrtc WebRtc webrtc


void cb_jni_WebRtc_Service(struct libusb_transfer *the_transfer) {
    //LOGD("Iso Transfer Callback Function");
    unsigned char *p;
    int packetLen;
    int i;
    p = the_transfer->buffer;
    for (i = 0; i < the_transfer->num_iso_packets; i++, p += maxPacketSize) {
        if (the_transfer->iso_packet_desc[i].status == LIBUSB_TRANSFER_COMPLETED) {
            packetLen = the_transfer->iso_packet_desc[i].actual_length;
            if (packetLen < 2) {
                continue;
            }
            // error packet
            if (p[1] & UVC_STREAM_ERR) // bmHeaderInfoh
            {
                LOGD("UVC_STREAM_ERR --> Package %d", i);
                frameUeberspringen = 1;
                continue;
            }
            packetLen -= p[0];
            if (packetLen + total > videoFrameData->FrameBufferSize) {
                if (ueberschreitungDerUebertragungslaenge == 1) {
                    ueberschreitungDerUebertragungslaenge = 1;
                    fflush(stdout);
                }
                packetLen = videoFrameData->FrameBufferSize - total;
            }
            memcpy(videoFrameData->videoframe + total, p + p[0], packetLen);
            total += packetLen;
            if (p[1] & UVC_STREAM_EOF) {
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (runningStream == false) stopStreaming();
                    uvc_frame_t *nv21;
                    uvc_error_t ret;
                    nv21 = uvc_allocate_frame(imageWidth * imageHeight * 4);
                    if (!nv21) {
                        printf("unable to allocate rgb frame!");
                        return;
                    }
                    // Do the NV21 conversion
                    ret = uvc_yuyv2yuv420SP(nv21);
                    if (ret) {
                        uvc_perror(ret, "uvc_any2bgr");
                        uvc_free_frame(nv21);
                        return;
                    }
                    JNIEnv * jenv;
                    int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                    jbyteArray array = (*jenv)->NewByteArray(jenv, nv21->data);
                    (*jenv)->SetByteArrayRegion(jenv, array, 0, videoFrameData->FrameBufferSize, (jbyte *) videoFrameData->videoframe);
                    // Service
                    (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaRetrievedFrameFromLibUsb, array);
                    total = 0;
                    uvc_free_frame(nv21);
                } else {
                    LOGD("Länge des Frames (Übersprungener Frame) = %d\n", total);
                    total = 0;
                    frameUeberspringen = 0;
                }
            }
        }
    }
    if (runningStream) if (libusb_submit_transfer(the_transfer) != 0) {
            LOGD(stderr, "SUBMISSION FAILED. \n");
        }
}

JNIEXPORT void JNICALL Java_com_example_androidthings_videortc_UsbCapturer_JniWebRtcJavaMethods
        (JNIEnv *env, jobject obj) {
    if (initialized) {

        class = (*env)->GetObjectClass(env, obj);
        jniHelperClass = (*env)->NewGlobalRef(env, class);
        mainActivityObj = (*env)->NewGlobalRef(env, obj);

        javaRetrievedFrameFromLibUsb = (*env)->GetMethodID(env, jniHelperClass, "retrievedFrameFromLibUsb", "([B)V");
        //javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, jniHelperClass, "processReceivedMJpegVideoFrameKamera", "([B)V");
        //javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, jniHelperClass, "processReceivedVideoFrameYuvFromJni", "([B)V");

        LOGD("ISO Stream complete");
    }
}

void prepairTheStream_WebRtc_Service() {
    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                             camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r,
             camStreamingAltSetting);
    }
    if (activeUrbs > 16) activeUrbs = 16;

}

void lunchTheStream_WebRtc_Service() {
    if (initialized) {

        struct libusb_transfer *xfers[activeUrbs];
        for (i = 0; i < activeUrbs; i++) {
            xfers[i] = libusb_alloc_transfer(packetsPerRequest);
            uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
            libusb_fill_iso_transfer(
                    xfers[i], devh, camStreamingEndpoint,
                    data, maxPacketSize * packetsPerRequest, packetsPerRequest,
                    cb_jni_WebRtc_Service, NULL, 5000);
            libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
        }
        runningStream = true;
        for (i = 0; i < activeUrbs; i++) {
            if (libusb_submit_transfer(xfers[i]) != 0) {
                LOGD(stderr, "submit xfer failed.\n");
            }
        }
        while (runningStream) {
            if (runningStream == false) {
                LOGD("stopped because runningStream == false !!  --> libusb_event_handling STOPPED");
                break;
            }
            libusb_handle_events(ctx);
        }
        LOGD("ISO Stream Start -- finished");
    }
}









    /*
    int r = libusb_set_interface_alt_setting(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber, camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r, camStreamingAltSetting);
    }
    if(activeUrbs > 16) activeUrbs = 16;
    //struct libusb_transfer * xfers[activeUrbs];
    for (i = 0; i < activeUrbs; i++) {
        xfers[i] = libusb_alloc_transfer(packetsPerRequest);
        uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
        libusb_fill_iso_transfer(
                xfers[i], globalUVCHandle->usb_devh, camStreamingEndpoint,
                data, maxPacketSize*packetsPerRequest, packetsPerRequest,
                isoc_transfer_completion_handler, NULL, 5000);
        libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
    }
    runningStream = true;
    for (i = 0; i < activeUrbs; i++) {
        if (libusb_submit_transfer(xfers[i]) != 0) {
            LOGD(stderr, "submit xfer failed.\n");
        }
    }
     *//*
}



/*
void cb_jni_stream_Surface(uvc_frame_t *frame, void *ptr) {



    if (runningStream == false) stopStreaming();


    uvc_frame_t *rgb;
    uvc_error_t ret;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgb = uvc_allocate_frame(frame->width * frame->height * 4);
    if (!rgb) {
        printf("unable to allocate rgb frame!");
        return;
    }

    // Do the BGR conversion
    ret = uvc_any2rgbx(frame, rgb);
    if (ret) {
        uvc_perror(ret, "uvc_any2bgr");
        uvc_free_frame(rgb);
        return;
    }
    copyToSurface(rgb, &mCaptureWindow);
    uvc_free_frame(rgb);
}


void cb_jni_stream_ImageView(uvc_frame_t *frame, void *ptr) {
    if (runningStream == false) stopStreaming();
    JNIEnv * jenv;
    int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
    jbyteArray array = (*jenv)->NewByteArray(jenv, frame->data_bytes);
    (*jenv)->SetByteArrayRegion(jenv, array, 0, frame->data_bytes, (jbyte *) frame->data);
    if (frame->frame_format == UVC_FRAME_FORMAT_MJPEG)     (*jenv)->CallVoidMethod(jenv, activity, javaProcessReceivedMJpegVideoFrameKamera, array);
    else if (frame->frame_format == UVC_FRAME_FORMAT_YUYV) (*jenv)->CallVoidMethod(jenv, activity, javaProcessReceivedVideoFrameYuv, array);
    else (*jenv)->CallVoidMethod(jenv, activity, javaProcessReceivedVideoFrameYuv, array);
    (*javaVm)->DetachCurrentThread(javaVm);
}




void getFramesOverLibUsb(int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
                         int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int yuvFrameIsZero, int stream ) {


    packetsPerRequest = packetsPerReques;
    maxPacketSize = maxPacketSiz;
    activeUrbs = activeUrb;
    camStreamingAltSetting = camStreamingAltSettin;
    camFormatIndex = camFormatInde;
    camFrameIndex = camFrameInde;
    camFrameInterval = camFrameInterva;
    imageWidth = imageWidt;
    imageHeight = imageHeigh;
    int requestMode = 0;

    probeCommitControl(bmHint, camFormatIndex, camFrameIndex,camFrameInterval);
    probeCommitControl_cleanup();

    LOGD("ISO Stream");


    uvc_error_t ret;
    uvc_stream_handle_t *strmh;

    for (int i = 0; i < sizeof(streamControl); i++)
        if (streamControl[i] != 0) {
            LOGD("%d -> [%d ] ",i,  streamControl[i]);}

    global_UVC_ctrl.bmHint = SW_TO_SHORT(streamControl);
    global_UVC_ctrl.bFormatIndex = streamControl[2];
    global_UVC_ctrl.bFrameIndex = streamControl[3];
    global_UVC_ctrl.dwFrameInterval = DW_TO_INT(streamControl + 4);
    global_UVC_ctrl.wKeyFrameRate = SW_TO_SHORT(streamControl + 8);
    global_UVC_ctrl.wPFrameRate = SW_TO_SHORT(streamControl + 10);
    global_UVC_ctrl.wCompQuality = SW_TO_SHORT(streamControl + 12);
    global_UVC_ctrl.wCompWindowSize = SW_TO_SHORT(streamControl + 14);
    global_UVC_ctrl.wDelay = SW_TO_SHORT(streamControl + 16);
    global_UVC_ctrl.dwMaxVideoFrameSize = DW_TO_INT(streamControl + 18);
    global_UVC_ctrl.dwMaxPayloadTransferSize = DW_TO_INT(streamControl + 22);
    global_UVC_ctrl.dwClockFrequency = DW_TO_INT(streamControl + 26);
    global_UVC_ctrl.bmFramingInfo = streamControl[30];
    global_UVC_ctrl.bPreferedVersion = streamControl[31];
    global_UVC_ctrl.bMinVersion = streamControl[32];
    global_UVC_ctrl.bMaxVersion = streamControl[33];
    global_UVC_ctrl.bUsage = streamControl[34];
    global_UVC_ctrl.bBitDepthLuma = streamControl[35];
    global_UVC_ctrl.bmSettings = streamControl[36];
    global_UVC_ctrl.bMaxNumberOfRefFramesPlus1 = streamControl[37];
    global_UVC_ctrl.bmRateControlModes = SW_TO_SHORT(streamControl + 38);
    global_UVC_ctrl.bmLayoutPerStream = QW_TO_LONG(streamControl + 40);



    ret = uvc_stream_open_ctrl(globalUVCHandle, &strmh, &global_UVC_ctrl);
    if (UNLIKELY(ret != UVC_SUCCESS))
        LOGD("return = %d", ret);

    runningStream = true;
    uvc_error_t err = uvc_stream_start_random(strmh, stream == 1 ? cb_stream : cb_test, 12345, 0, 0, activeUrb, packetsPerReques, camStreamingAltSetting, maxPacketSiz );
    if (err == 0) LOGD("0 return");
    else {
        LOGD("return = %d", err);
        uvc_perror(result, "failed start_streaming");
    }
    LOGD("ISO Stream complete");
    /*
    int r = libusb_set_interface_alt_setting(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber, camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r, camStreamingAltSetting);
    }
    if(activeUrbs > 16) activeUrbs = 16;
    //struct libusb_transfer * xfers[activeUrbs];
    for (i = 0; i < activeUrbs; i++) {
        xfers[i] = libusb_alloc_transfer(packetsPerRequest);
        uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
        libusb_fill_iso_transfer(
                xfers[i], globalUVCHandle->usb_devh, camStreamingEndpoint,
                data, maxPacketSize*packetsPerRequest, packetsPerRequest,
                isoc_transfer_completion_handler, NULL, 5000);
        libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
    }
    runningStream = true;
    for (i = 0; i < activeUrbs; i++) {
        if (libusb_submit_transfer(xfers[i]) != 0) {
            LOGD(stderr, "submit xfer failed.\n");
        }
    }
     *//*
}

uvc_preview_frame_callback(uvc_frame_t *frame, void *vptr_args) {

}

void stopStreaming() {
    uvc_stop_streaming(globalUVCHandle);
    runningStream = false;


    /*
    //runningStream = false;
    if(activeUrbs > 16) activeUrbs = 16;
    for (i = 0; i < activeUrbs; i++) {
        int res = libusb_cancel_transfer(xfers[i]);
        if ((res < 0) && (res != LIBUSB_ERROR_NOT_FOUND)) {
            LOGD("libusb_cancel_transfer failed");
        } else LOGD("libusb_cancel_transfer sucess");
    }

    uvc_release_if(globalUVCHandle, global_UVC_ctrl.bInterfaceNumber);
    //libusb_release_interface(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber);
     *//*
}





JNIEXPORT void JNICALL Java_humer_UvcCamera_SetUpTheUsbDevice_JniIsoStreamActivity
        (JNIEnv *env, jobject obj, jobject jSurface, jint stream, jint frameIndex) {


    if (initialized) {
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval);
        probeCommitControl_cleanup();
        LOGD("ISO Stream");
        uvc_error_t ret;
        uvc_stream_handle_t *strmh;

        for (int i = 0; i < sizeof(streamControl); i++)
            if (streamControl[i] != 0) {
                LOGD("%d -> [%d ] ", i, streamControl[i]);
            }

        global_UVC_ctrl.bmHint = SW_TO_SHORT(streamControl);
        global_UVC_ctrl.bFormatIndex = streamControl[2];
        global_UVC_ctrl.bFrameIndex = streamControl[3];
        global_UVC_ctrl.dwFrameInterval = DW_TO_INT(streamControl + 4);
        global_UVC_ctrl.wKeyFrameRate = SW_TO_SHORT(streamControl + 8);
        global_UVC_ctrl.wPFrameRate = SW_TO_SHORT(streamControl + 10);
        global_UVC_ctrl.wCompQuality = SW_TO_SHORT(streamControl + 12);
        global_UVC_ctrl.wCompWindowSize = SW_TO_SHORT(streamControl + 14);
        global_UVC_ctrl.wDelay = SW_TO_SHORT(streamControl + 16);
        global_UVC_ctrl.dwMaxVideoFrameSize = DW_TO_INT(streamControl + 18);
        global_UVC_ctrl.dwMaxPayloadTransferSize = DW_TO_INT(streamControl + 22);
        global_UVC_ctrl.dwClockFrequency = DW_TO_INT(streamControl + 26);
        global_UVC_ctrl.bmFramingInfo = streamControl[30];
        global_UVC_ctrl.bPreferedVersion = streamControl[31];
        global_UVC_ctrl.bMinVersion = streamControl[32];
        global_UVC_ctrl.bMaxVersion = streamControl[33];
        global_UVC_ctrl.bUsage = streamControl[34];
        global_UVC_ctrl.bBitDepthLuma = streamControl[35];
        global_UVC_ctrl.bmSettings = streamControl[36];
        global_UVC_ctrl.bMaxNumberOfRefFramesPlus1 = streamControl[37];
        global_UVC_ctrl.bmRateControlModes = SW_TO_SHORT(streamControl + 38);
        global_UVC_ctrl.bmLayoutPerStream = QW_TO_LONG(streamControl + 40);


        ret = uvc_stream_open_ctrl(globalUVCHandle, &strmh, &global_UVC_ctrl);
        if (UNLIKELY(ret != UVC_SUCCESS))
            LOGD("return = %d", ret);
        runningStream = true;



        LOGD("%d   <- bmHint  from jna", strmh->cur_ctrl.bmHint);
        LOGD("%d   <- bFormatIndex  from jna", strmh->cur_ctrl.bFormatIndex);
        LOGD("%d   <- bFrameIndex  from jna", strmh->cur_ctrl.bFrameIndex);
        LOGD("%d   <- dwFrameInterval  from jna", strmh->cur_ctrl.dwFrameInterval);



        uvc_error_t err = uvc_stream_start_random(strmh, cb_test , 12345, 0, 0, activeUrbs,
                                                  packetsPerRequest, camStreamingAltSetting,
                                                  maxPacketSize);
        if (err == 0) LOGD("0 return");
        else {
            LOGD("return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }

        LOGD("ISO Stream complete");
    }

}

JNIEXPORT unsigned char * JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniProbeCommitControl
        (JNIEnv *env, jobject obj, jint bmHin, jint camFormatInde, jint camFrameInde, jint camFrameInterva) {
    LOGD("probeCommitControl");
    bmHint = bmHin;
    camFormatIndex = camFormatInde;
    camFrameIndex = camFrameInde;
    camFrameInterval = camFrameInterva;
    ctl_transfer_Data = malloc(sizeof *ctl_transfer_Data + sizeof(unsigned char[48*4]));
    ctl_transfer_Data->BufferSize = 48 * 4;
    memset(ctl_transfer_Data->ctl_transfer_values, 0, sizeof(unsigned char) * (ctl_transfer_Data->BufferSize));
    uvc_streaming_interface_t *stream_if;
    uvc_format_desc_t *format;
    DL_FOREACH(globalUVCHandle->info->stream_ifs, stream_if)  {
        DL_FOREACH(stream_if->format_descs, format) {
            LOGD("format->bmFlags = ", format->bmFlags);

            if (!format->bDescriptorSubtype == UVC_FRAME_FORMAT_MJPEG)
                if (!_uvc_frame_format_matches_guid(UVC_FRAME_FORMAT_YUYV || UVC_FRAME_FORMAT_ANY || UVC_FRAME_FORMAT_MJPEG || UVC_FRAME_FORMAT_UYVY || UVC_FRAME_FORMAT_MJPEG, format->guidFormat))
                    continue;
            global_UVC_ctrl.bInterfaceNumber = stream_if->bInterfaceNumber;
            uvc_error_t err = uvc_claim_if(globalUVCHandle, global_UVC_ctrl.bInterfaceNumber);
            LOGD("Stream Interface Claimed");
        }
    }

    int r = uvc_claim_if(globalUVCHandle, (global_UVC_ctrl.bInterfaceNumber - 1) );
    if (r != LIBUSB_SUCCESS)    LOGD("Failed to claim Control Interface(devh, 1, 0, failed with error %d\n", r);
    else LOGD("Control Interface Claimed");

    r = libusb_set_interface_alt_setting(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber, 0);
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 0, failed with error %d\n", r);
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = 0\n", r);
    }

    write_Ctl_Buffer = true;
    controlTransfer(globalUVCHandle, UVC_SET_CUR, &global_UVC_ctrl , 1);
    controlTransfer(globalUVCHandle, UVC_GET_CUR, &global_UVC_ctrl , 1);
    controlTransfer(globalUVCHandle, UVC_SET_CUR, &global_UVC_ctrl , 0);
  //  controlTransfer(globalUVCHandle, UVC_GET_CUR, &global_UVC_ctrl , 0);
    write_Ctl_Buffer = false;
    LOGD("ctl_transfer_Data->ctl_transfer_values");
    for (int i = 0; i < (48*4); i++)
        if (ctl_transfer_Data->ctl_transfer_values[i] != 0) {
            LOGD("%d -> [%d ] ",i,  ctl_transfer_Data->ctl_transfer_values[i]);}
    LOGD("Allocation Complete");
    return ctl_transfer_Data->ctl_transfer_values;

}


JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivitySurface
        (JNIEnv *env, jobject obj, jobject jSurface, jint stream, jint frameIndex) {

    ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
    // WINDOW_FORMAT_RGBA_8888
    ANativeWindow_setBuffersGeometry(preview_window,
                                     imageWidth, imageHeight, WINDOW_FORMAT_RGBA_8888);
    mCaptureWindow = preview_window;



    if (initialized) {
        //probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval);
        //probeCommitControl_cleanup();
        LOGD("ISO Stream");


        int status = (*env)->GetJavaVM(env, &javaVm);
        if(status != 0) {
            LOGE("failed to attach javaVm");
        }
        class = (*env)->GetObjectClass(env, obj);
        activity = (*env)->NewGlobalRef(env, obj);

        if(!strcmp(frameFormat, "MJPEG"))
        {
            javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, class, "processReceivedMJpegVideoFrameKamera", "([B)V");
            LOGD("javaProcessReceivedMJpegVideoFrameKamera set");
        }

        if(!strcmp(frameFormat, "YUY2"))
        {
            javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, class, "processReceivedVideoFrameYuvFromJni", "([B)V");
            LOGD("javaProcessReceivedVideoFrameYuv set");
        }
        uvc_error_t ret;
        uvc_stream_handle_t *strmh;

        for (int i = 0; i < sizeof(streamControl); i++)
            if (streamControl[i] != 0) {
                LOGD("%d -> [%d ] ", i, streamControl[i]);
            }

        global_UVC_ctrl.bmHint = SW_TO_SHORT(streamControl);
        global_UVC_ctrl.bFormatIndex = streamControl[2];
        global_UVC_ctrl.bFrameIndex = streamControl[3];
        global_UVC_ctrl.dwFrameInterval = DW_TO_INT(streamControl + 4);
        global_UVC_ctrl.wKeyFrameRate = SW_TO_SHORT(streamControl + 8);
        global_UVC_ctrl.wPFrameRate = SW_TO_SHORT(streamControl + 10);
        global_UVC_ctrl.wCompQuality = SW_TO_SHORT(streamControl + 12);
        global_UVC_ctrl.wCompWindowSize = SW_TO_SHORT(streamControl + 14);
        global_UVC_ctrl.wDelay = SW_TO_SHORT(streamControl + 16);
        global_UVC_ctrl.dwMaxVideoFrameSize = DW_TO_INT(streamControl + 18);
        global_UVC_ctrl.dwMaxPayloadTransferSize = DW_TO_INT(streamControl + 22);
        global_UVC_ctrl.dwClockFrequency = DW_TO_INT(streamControl + 26);
        global_UVC_ctrl.bmFramingInfo = streamControl[30];
        global_UVC_ctrl.bPreferedVersion = streamControl[31];
        global_UVC_ctrl.bMinVersion = streamControl[32];
        global_UVC_ctrl.bMaxVersion = streamControl[33];
        global_UVC_ctrl.bUsage = streamControl[34];
        global_UVC_ctrl.bBitDepthLuma = streamControl[35];
        global_UVC_ctrl.bmSettings = streamControl[36];
        global_UVC_ctrl.bMaxNumberOfRefFramesPlus1 = streamControl[37];
        global_UVC_ctrl.bmRateControlModes = SW_TO_SHORT(streamControl + 38);
        global_UVC_ctrl.bmLayoutPerStream = QW_TO_LONG(streamControl + 40);
        ret = uvc_stream_open_ctrl(globalUVCHandle, &strmh, &global_UVC_ctrl);
        if (UNLIKELY(ret != UVC_SUCCESS))
            LOGD("return = %d", ret);
        runningStream = true;
        LOGD("%d   <- bmHint  from jna", strmh->cur_ctrl.bmHint);
        LOGD("%d   <- bFormatIndex  from jna", strmh->cur_ctrl.bFormatIndex);
        LOGD("%d   <- bFrameIndex  from jna", strmh->cur_ctrl.bFrameIndex);
        LOGD("%d   <- dwFrameInterval  from jna", strmh->cur_ctrl.dwFrameInterval);
        uvc_error_t err = uvc_stream_start_random(strmh, cb_jni_stream_Surface , 12345, 0, 0, activeUrbs,
                                                  packetsPerRequest, camStreamingAltSetting, maxPacketSize);
        if (err == 0) LOGD("0 return");
        else {
            LOGD("return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }
        if (jnalog) jnalog("JNA LOG\nISO Stream complete from JNA LOG");
        LOGD("ISO Stream complete");
    }
}


JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniIsoStreamActivity
        (JNIEnv *env, jobject obj, jint stream, jint frameIndex) {
    if (initialized) {
        /*
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval);
        probeCommitControl_cleanup();
         *//*
        LOGD("ISO Stream");
        int status = (*env)->GetJavaVM(env, &javaVm);
        if(status != 0) {
            LOGE("failed to attach javaVm");
        }
        class = (*env)->GetObjectClass(env, obj);
        activity = (*env)->NewGlobalRef(env, obj);
        if(!strcmp(frameFormat, "MJPEG"))
        {
            javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, class, "processReceivedMJpegVideoFrameKamera", "([B)V");
            LOGD("javaProcessReceivedMJpegVideoFrameKamera set");
        }
        if(!strcmp(frameFormat, "YUY2"))
        {
            javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, class, "processReceivedVideoFrameYuvFromJni", "([B)V");
            LOGD("javaProcessReceivedVideoFrameYuv set");

        }
        uvc_error_t ret;
        uvc_stream_handle_t *strmh;
        for (int i = 0; i < sizeof(streamControl); i++)
            if (streamControl[i] != 0) {
                LOGD("%d -> [%d ] ", i, streamControl[i]);
            }

        global_UVC_ctrl.bmHint = SW_TO_SHORT(streamControl);
        global_UVC_ctrl.bFormatIndex = streamControl[2];
        global_UVC_ctrl.bFrameIndex = streamControl[3];
        global_UVC_ctrl.dwFrameInterval = DW_TO_INT(streamControl + 4);
        global_UVC_ctrl.wKeyFrameRate = SW_TO_SHORT(streamControl + 8);
        global_UVC_ctrl.wPFrameRate = SW_TO_SHORT(streamControl + 10);
        global_UVC_ctrl.wCompQuality = SW_TO_SHORT(streamControl + 12);
        global_UVC_ctrl.wCompWindowSize = SW_TO_SHORT(streamControl + 14);
        global_UVC_ctrl.wDelay = SW_TO_SHORT(streamControl + 16);
        global_UVC_ctrl.dwMaxVideoFrameSize = DW_TO_INT(streamControl + 18);
        global_UVC_ctrl.dwMaxPayloadTransferSize = DW_TO_INT(streamControl + 22);
        global_UVC_ctrl.dwClockFrequency = DW_TO_INT(streamControl + 26);
        global_UVC_ctrl.bmFramingInfo = streamControl[30];
        global_UVC_ctrl.bPreferedVersion = streamControl[31];
        global_UVC_ctrl.bMinVersion = streamControl[32];
        global_UVC_ctrl.bMaxVersion = streamControl[33];
        global_UVC_ctrl.bUsage = streamControl[34];
        global_UVC_ctrl.bBitDepthLuma = streamControl[35];
        global_UVC_ctrl.bmSettings = streamControl[36];
        global_UVC_ctrl.bMaxNumberOfRefFramesPlus1 = streamControl[37];
        global_UVC_ctrl.bmRateControlModes = SW_TO_SHORT(streamControl + 38);
        global_UVC_ctrl.bmLayoutPerStream = QW_TO_LONG(streamControl + 40);


        ret = uvc_stream_open_ctrl(globalUVCHandle, &strmh, &global_UVC_ctrl);
        if (UNLIKELY(ret != UVC_SUCCESS))
            LOGD("return = %d", ret);
        runningStream = true;



        LOGD("%d   <- bmHint  from jna", strmh->cur_ctrl.bmHint);
        LOGD("%d   <- bFormatIndex  from jna", strmh->cur_ctrl.bFormatIndex);
        LOGD("%d   <- bFrameIndex  from jna", strmh->cur_ctrl.bFrameIndex);
        LOGD("%d   <- dwFrameInterval  from jna", strmh->cur_ctrl.dwFrameInterval);




        uvc_error_t err = uvc_stream_start_random(strmh, cb_jni_stream_ImageView , 12345, 0, 0, activeUrbs,
                                                  packetsPerRequest, camStreamingAltSetting, maxPacketSize);
        if (err == 0) LOGD("0 return");
        else {
            LOGD("return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }


        // *//*

        if (jnalog) jnalog("JNA LOG\nISO Stream complete from JNA LOG");
        LOGD("ISO Stream complete");
    }

}

*//*

JNIEXPORT void JNICALL Java_com_example_androidthings_videortc_UsbCapturer_JniWebRtc
        (JNIEnv *env, jobject obj, jint stream, jint frameIndex) {
    if (initialized) {
        /*
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval);
        probeCommitControl_cleanup();
         *//*
        LOGD("ISO Stream");
        int status = (*env)->GetJavaVM(env, &javaVm);
        if(status != 0) {
            LOGE("failed to attach javaVm");
        }
        class = (*env)->GetObjectClass(env, obj);
        activity = (*env)->NewGlobalRef(env, obj);
        if(!strcmp(frameFormat, "MJPEG"))
        {
            javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, class, "processReceivedMJpegVideoFrameKamera", "([B)V");
            LOGD("javaProcessReceivedMJpegVideoFrameKamera set");
        }
        if(!strcmp(frameFormat, "YUY2"))
        {
            javaProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, class, "processReceivedVideoFrameYuvFromJni", "([B)V");
            LOGD("javaProcessReceivedVideoFrameYuv set");
        }
        uvc_error_t ret;
        uvc_stream_handle_t *strmh;
        for (int i = 0; i < sizeof(streamControl); i++)
            if (streamControl[i] != 0) {
                LOGD("%d -> [%d ] ", i, streamControl[i]);
            }
        global_UVC_ctrl.bmHint = SW_TO_SHORT(streamControl);
        global_UVC_ctrl.bFormatIndex = streamControl[2];
        global_UVC_ctrl.bFrameIndex = streamControl[3];
        global_UVC_ctrl.dwFrameInterval = DW_TO_INT(streamControl + 4);
        global_UVC_ctrl.wKeyFrameRate = SW_TO_SHORT(streamControl + 8);
        global_UVC_ctrl.wPFrameRate = SW_TO_SHORT(streamControl + 10);
        global_UVC_ctrl.wCompQuality = SW_TO_SHORT(streamControl + 12);
        global_UVC_ctrl.wCompWindowSize = SW_TO_SHORT(streamControl + 14);
        global_UVC_ctrl.wDelay = SW_TO_SHORT(streamControl + 16);
        global_UVC_ctrl.dwMaxVideoFrameSize = DW_TO_INT(streamControl + 18);
        global_UVC_ctrl.dwMaxPayloadTransferSize = DW_TO_INT(streamControl + 22);
        global_UVC_ctrl.dwClockFrequency = DW_TO_INT(streamControl + 26);
        global_UVC_ctrl.bmFramingInfo = streamControl[30];
        global_UVC_ctrl.bPreferedVersion = streamControl[31];
        global_UVC_ctrl.bMinVersion = streamControl[32];
        global_UVC_ctrl.bMaxVersion = streamControl[33];
        global_UVC_ctrl.bUsage = streamControl[34];
        global_UVC_ctrl.bBitDepthLuma = streamControl[35];
        global_UVC_ctrl.bmSettings = streamControl[36];
        global_UVC_ctrl.bMaxNumberOfRefFramesPlus1 = streamControl[37];
        global_UVC_ctrl.bmRateControlModes = SW_TO_SHORT(streamControl + 38);
        global_UVC_ctrl.bmLayoutPerStream = QW_TO_LONG(streamControl + 40);
        ret = uvc_stream_open_ctrl(globalUVCHandle, &strmh, &global_UVC_ctrl);
        if (UNLIKELY(ret != UVC_SUCCESS))
            LOGD("return = %d", ret);
        runningStream = true;
        LOGD("%d   <- bmHint  from jna", strmh->cur_ctrl.bmHint);
        LOGD("%d   <- bFormatIndex  from jna", strmh->cur_ctrl.bFormatIndex);
        LOGD("%d   <- bFrameIndex  from jna", strmh->cur_ctrl.bFrameIndex);
        LOGD("%d   <- dwFrameInterval  from jna", strmh->cur_ctrl.dwFrameInterval);
        uvc_error_t err = uvc_stream_start_random(strmh, cb_jni_stream_ImageView , 12345, 0, 0, activeUrbs,
                                                  packetsPerRequest, camStreamingAltSetting, maxPacketSize);
        if (err == 0) LOGD("0 return");
        else {
            LOGD("return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }
        LOGD("ISO Stream complete");
    }
}





/*
int r = libusb_set_interface_alt_setting(globalUVCHandle->usb_devh, global_UVC_ctrl.bInterfaceNumber, camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
if (r != LIBUSB_SUCCESS) {
    LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
} else {
    LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = %d\n", r, camStreamingAltSetting);
}
if(activeUrbs > 16) activeUrbs = 16;
//struct libusb_transfer * xfers[activeUrbs];
for (i = 0; i < activeUrbs; i++) {
    xfers[i] = libusb_alloc_transfer(packetsPerRequest);
    uint8_t *data = malloc(maxPacketSize * packetsPerRequest);
    libusb_fill_iso_transfer(
            xfers[i], globalUVCHandle->usb_devh, camStreamingEndpoint,
            data, maxPacketSize*packetsPerRequest, packetsPerRequest,
            isoc_transfer_completion_handler, NULL, 5000);
    libusb_set_iso_packet_lengths(xfers[i], maxPacketSize);
}
runningStream = true;
for (i = 0; i < activeUrbs; i++) {
    if (libusb_submit_transfer(xfers[i]) != 0) {
        LOGD(stderr, "submit xfer failed.\n");
    }
}


static const uint8_t huffman_table[] =
        {
                0xFF, 0xC4, 0x01, 0xA2, 0x00, 0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01,
                0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02,
                0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x01, 0x00, 0x03,
                0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                0x0A, 0x0B, 0x10, 0x00, 0x02, 0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05,
                0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7D, 0x01, 0x02, 0x03, 0x00, 0x04,
                0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22,
                0x71, 0x14, 0x32, 0x81, 0x91, 0xA1, 0x08, 0x23, 0x42, 0xB1, 0xC1, 0x15,
                0x52, 0xD1, 0xF0, 0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0A, 0x16, 0x17,
                0x18, 0x19, 0x1A, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x34, 0x35, 0x36,
                0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A,
                0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66,
                0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A,
                0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x92, 0x93, 0x94, 0x95,
                0x96, 0x97, 0x98, 0x99, 0x9A, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7, 0xA8,
                0xA9, 0xAA, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA, 0xC2,
                0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xD2, 0xD3, 0xD4, 0xD5,
                0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7,
                0xE8, 0xE9, 0xEA, 0xF1, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9,
                0xFA, 0x11, 0x00, 0x02, 0x01, 0x02, 0x04, 0x04, 0x03, 0x04, 0x07, 0x05,
                0x04, 0x04, 0x00, 0x01, 0x02, 0x77, 0x00, 0x01, 0x02, 0x03, 0x11, 0x04,
                0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22,
                0x32, 0x81, 0x08, 0x14, 0x42, 0x91, 0xA1, 0xB1, 0xC1, 0x09, 0x23, 0x33,
                0x52, 0xF0, 0x15, 0x62, 0x72, 0xD1, 0x0A, 0x16, 0x24, 0x34, 0xE1, 0x25,
                0xF1, 0x17, 0x18, 0x19, 0x1A, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x35, 0x36,
                0x37, 0x38, 0x39, 0x3A, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A,
                0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5A, 0x63, 0x64, 0x65, 0x66,
                0x67, 0x68, 0x69, 0x6A, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7A,
                0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x92, 0x93, 0x94,
                0x95, 0x96, 0x97, 0x98, 0x99, 0x9A, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6, 0xA7,
                0xA8, 0xA9, 0xAA, 0xB2, 0xB3, 0xB4, 0xB5, 0xB6, 0xB7, 0xB8, 0xB9, 0xBA,
                0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC7, 0xC8, 0xC9, 0xCA, 0xD2, 0xD3, 0xD4,
                0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7,
                0xE8, 0xE9, 0xEA, 0xF2, 0xF3, 0xF4, 0xF5, 0xF6, 0xF7, 0xF8, 0xF9, 0xFA
        };

unsigned char* convertMjpegFrameToJpeg(unsigned char* frameData, int frameLen) {
    //int frameLen = frameData.length;
    while (frameLen > 0 && frameData[frameLen - 1] == 0) {
        frameLen--;
    }
    if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9) {
        LOGE("Invalid MJPEG frame structure, length= %d", frameLen);
    }
    bool hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
    bool exit = false;
    if (hasHuffmanTable) {
        LOGD ("hasHuffmanTable ...");
        return frameData;
/*
            if (frameData.length == frameLen) {
                return frameData;
            }
            return Arrays.copyOf(frameData, frameLen);
*//*

} else {
int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);

if (segmentDaPos == -1) {
exit = true;
LOGE("Segment 0xDA not found in MJPEG frame data.");
}
if (exit ==false) {
// unsigned char buffer[64]={0xef,0xaa,0x03,0x05,0x05,0x06,0x07,0x08,......};


LOGD ("Converting ...");



/*
unsigned char *a = malloc( sizeof( unsigned char ) * frameLen + sizeof (huffman_table));
memcpy(a, frameData, segmentDaPos);
memcpy(a + segmentDaPos, huffman_table, sizeof (huffman_table));
memcpy(a + (segmentDaPos + sizeof (huffman_table)), frameData + segmentDaPos, frameLen - segmentDaPos);


return a;




//byte[]* a = new byte[frameLen + mjpgHuffmanTable.length];
System.arraycopy(frameData, 0, a, 0, segmentDaPos);
System.arraycopy(mjpgHuffmanTable, 0, a, segmentDaPos, mjpgHuffmanTable.length);
System.arraycopy(frameData, segmentDaPos, a, segmentDaPos + mjpgHuffmanTable.length, frameLen - segmentDaPos);




return NULL;

} else
return NULL;
}
}


/* This callback function runs once per frame. Use it to perform any
 * quick processing you need, or have it put the frame into your application's
 * input queue. If this function takes too long, you'll start losing frames. */

/*
void cb_stream(uvc_frame_t *frame, void *ptr) {

    LOGD("CallbackFunction called");
    LOGD("actual len = %d   /// len = %d", frame->actual_bytes, frame->data_bytes);
    LOGD("width = %d   /// height = %d", frame->width, frame->height);
    uvc_frame_t *bgr;
    uvc_error_t ret;

    if (fameJnaCallback != NULL) fameJnaCallback(frame->data, frame->data_bytes) ;
}

void cb_test(uvc_frame_t *frame, void *ptr) {

    LOGD("CallbackFunction called");
    LOGD("actual len = %d   /// len = %d", frame->actual_bytes, frame->data_bytes);
    LOGD("width = %d   /// height = %d", frame->width, frame->height);

    if (fameJnaCallback != NULL) fameJnaCallback(&frame->data, frame->actual_bytes) ;
}
*/

// see USB video class standard, USB_Video_Payload_MJPEG_1.5.pdf



/* this function is run by the second thread *//*
void *preview_thread_func(void *x_void_ptr)
{
/* increment x to 100 *//*
    int *x_ptr = (int *)x_void_ptr;
    while(++(*x_ptr) < 100);
    printf("x increment finished\n");
/* the function must return something - NULL will do */ /*
    return NULL;
}


int findJpegSegment(unsigned char *a, int dataLen, int segmentType) {
    int p = 2;
    while (p <= dataLen - 6) {
        if ((a[p] & 0xff) != 0xff) {
            LOGE("Unexpected JPEG data structure (marker expected).");
            break;
        }
        int markerCode = a[p + 1] & 0xff;
        if (markerCode == segmentType) {
            return p;
        }
        if (markerCode >= 0xD0 && markerCode <= 0xDA) {       // stop when scan data begins
            break;
        }
        int len = ((a[p + 2] & 0xff) << 8) + (a[p + 3] & 0xff);
        p += len + 2;
    }
    return -1;
}
 */
