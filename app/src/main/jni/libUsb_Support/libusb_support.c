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

#include <android-libjpeg-turbo/jni/vendor/libjpeg-turbo/libjpeg-turbo-2.0.1/turbojpeg.h>
//#include <android-libjpeg-turbo/jni/vendor/libjpeg-turbo/libjpeg-turbo-2.0.1/jpeglib.h>

#include <jpeg8d/jpeglib.h>

#include <android/bitmap.h>
#include <setjmp.h>

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

struct error_mgr {
    struct jpeg_error_mgr super;
    jmp_buf jmp;
};

static void _error_exit(j_common_ptr dinfo) {
    struct error_mgr *myerr = (struct error_mgr *) dinfo->err;
#ifndef NDEBUG
    #if (defined(ANDROID) || defined(__ANDROID__))
	char err_msg[1024];
	(*dinfo->err->format_message)(dinfo, err_msg);
	err_msg[1023] = 0;
	LOGW("err=%s", err_msg);
#else
	(*dinfo->err->output_message)(dinfo);
#endif
#endif
    longjmp(myerr->jmp, 1);
}


/* ISO/IEC 10918-1:1993(E) K.3.3. Default Huffman tables used by MJPEG UVC devices
 which don't specify a Huffman table in the JPEG stream. */
static const unsigned char dc_lumi_len[] = {
        0, 0, 1, 5, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 };
static const unsigned char dc_lumi_val[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

static const unsigned char dc_chromi_len[] = {
        0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0 };
static const unsigned char dc_chromi_val[] = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };

static const unsigned char ac_lumi_len[] = {
        0, 0, 2, 1, 3, 3, 2, 4, 3, 5, 5, 4, 4, 0, 0, 1, 0x7d };
static const unsigned char ac_lumi_val[] = {
        0x01, 0x02, 0x03, 0x00, 0x04, 0x11,	0x05, 0x12,
        0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
        0x22, 0x71,	0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
        0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
        0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
        0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
        0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
        0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
        0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
        0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
        0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
        0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
        0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
        0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
        0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
        0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
        0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
};
static const unsigned char ac_chromi_len[] = {
        0, 0, 2, 1, 2, 4, 4, 3, 4, 7, 5, 4, 4, 0, 1, 2, 0x77 };
static const unsigned char ac_chromi_val[] = {
        0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
        0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
        0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
        0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
        0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
        0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
        0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
        0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
        0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
        0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
        0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
        0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
        0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
        0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
        0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
        0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
        0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
        0xf9, 0xfa
};

#define COPY_HUFF_TABLE(dinfo,tbl,name) do { \
	if (dinfo->tbl == NULL) dinfo->tbl = jpeg_alloc_huff_table((j_common_ptr)dinfo); \
		memcpy(dinfo->tbl->bits, name##_len, sizeof(name##_len)); \
		memset(dinfo->tbl->huffval, 0, sizeof(dinfo->tbl->huffval)); \
		memcpy(dinfo->tbl->huffval, name##_val, sizeof(name##_val)); \
	} while(0)

static inline void insert_huff_tables(j_decompress_ptr dinfo) {
    COPY_HUFF_TABLE(dinfo, dc_huff_tbl_ptrs[0], dc_lumi);
    COPY_HUFF_TABLE(dinfo, dc_huff_tbl_ptrs[1], dc_chromi);
    COPY_HUFF_TABLE(dinfo, ac_huff_tbl_ptrs[0], ac_lumi);
    COPY_HUFF_TABLE(dinfo, ac_huff_tbl_ptrs[1], ac_chromi);
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
    const uint8_t *pyuv_end = pyuv + (imageWidth*imageHeight*2) - PIXEL8_YUYV;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // YUYV => RGBX8888
#if USE_STRIDE
    if ((imageWidth * 3/2) && out->step && ((imageWidth * 3/2) != out->step)) {
		const int hh = imageHeight < out->height ? imageWidth : out->height;
		const int ww = imageWidth < out->width ? imageWidth : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = videoFrameData->videoframe + (imageWidth * 3/2) * h;
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

#define MAX_READLINE 8

#ifndef MAX_READLINE
#define MAX_READLINE 1
#endif
#if MAX_READLINE < 1
#undef MAX_READLINE
#define MAX_READLINE 1
#endif

#define IUYVY2RGBX_2(pyuv, prgbx, ax, bx) { \
		const int d0 = (pyuv)[ax+0]; \
		const int d2 = (pyuv)[ax+2]; \
	    const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
	    const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
	    const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
		const int y1 = (pyuv)[ax+1]; \
		(prgbx)[bx+0] = sat(y1 + r); \
		(prgbx)[bx+1] = sat(y1 + g); \
		(prgbx)[bx+2] = sat(y1 + b); \
		(prgbx)[bx+3] = 0xff; \
		const int y3 = (pyuv)[ax+3]; \
		(prgbx)[bx+4] = sat(y3 + r); \
		(prgbx)[bx+5] = sat(y3 + g); \
		(prgbx)[bx+6] = sat(y3 + b); \
		(prgbx)[bx+7] = 0xff; \
    }
#define IUYVY2RGBX_16(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_8(pyuv, prgbx, ax + PIXEL8_UYVY, bx + PIXEL8_RGBX)
#define IUYVY2RGBX_8(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_4(pyuv, prgbx, ax + PIXEL4_UYVY, bx + PIXEL4_RGBX)
#define IUYVY2RGBX_4(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_2(pyuv, prgbx, ax, bx) \
	IUYVY2RGBX_2(pyuv, prgbx, ax + PIXEL2_UYVY, bx + PIXEL2_RGBX)
#define IUYVY2RGB_8(pyuv, prgb, ax, bx) \
	IUYVY2RGB_4(pyuv, prgb, ax, bx) \
	IUYVY2RGB_4(pyuv, prgb, ax + 8, bx + 12)
#define IUYVY2RGB_4(pyuv, prgb, ax, bx) \
	IUYVY2RGB_2(pyuv, prgb, ax, bx) \
	IUYVY2RGB_2(pyuv, prgb, ax + 4, bx + 6)
#define IYUYV2RGB_2(pyuv, prgb, ax, bx) { \
		const int d1 = (pyuv)[ax+1]; \
		const int d3 = (pyuv)[ax+3]; \
		const int r = (22987 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
		const int g = (-5636 * (d1/*(pyuv)[ax+1]*/ - 128) - 11698 * (d3/*(pyuv)[ax+3]*/ - 128)) >> 14; \
		const int b = (29049 * (d1/*(pyuv)[ax+1]*/ - 128)) >> 14; \
		const int y0 = (pyuv)[ax+0]; \
		(prgb)[bx+0] = sat(y0 + r); \
		(prgb)[bx+1] = sat(y0 + g); \
		(prgb)[bx+2] = sat(y0 + b); \
		const int y2 = (pyuv)[ax+2]; \
		(prgb)[bx+3] = sat(y2 + r); \
		(prgb)[bx+4] = sat(y2 + g); \
		(prgb)[bx+5] = sat(y2 + b); \
    }
#define IUYVY2RGB_2(pyuv, prgb, ax, bx) { \
		const int d0 = (pyuv)[ax+0]; \
		const int d2 = (pyuv)[ax+2]; \
	    const int r = (22987 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
	    const int g = (-5636 * (d0/*(pyuv)[ax+0]*/ - 128) - 11698 * (d2/*(pyuv)[ax+2]*/ - 128)) >> 14; \
	    const int b = (29049 * (d0/*(pyuv)[ax+0]*/ - 128)) >> 14; \
		const int y1 = (pyuv)[ax+1]; \
		(prgb)[bx+0] = sat(y1 + r); \
		(prgb)[bx+1] = sat(y1 + g); \
		(prgb)[bx+2] = sat(y1 + b); \
		const int y3 = (pyuv)[ax+3]; \
		(prgb)[bx+3] = sat(y3 + r); \
		(prgb)[bx+4] = sat(y3 + g); \
		(prgb)[bx+5] = sat(y3 + b); \
    }
#define IYUYV2RGB_16(pyuv, prgb, ax, bx) \
	IYUYV2RGB_8(pyuv, prgb, ax, bx) \
	IYUYV2RGB_8(pyuv, prgb, ax + PIXEL8_YUYV, bx + PIXEL8_RGB)
#define IYUYV2RGB_8(pyuv, prgb, ax, bx) \
	IYUYV2RGB_4(pyuv, prgb, ax, bx) \
	IYUYV2RGB_4(pyuv, prgb, ax + PIXEL4_YUYV, bx + PIXEL4_RGB)
#define IYUYV2RGB_4(pyuv, prgb, ax, bx) \
	IYUYV2RGB_2(pyuv, prgb, ax, bx) \
	IYUYV2RGB_2(pyuv, prgb, ax + PIXEL2_YUYV, bx + PIXEL2_RGB)

/** @brief Convert a frame from UYVY to RGBX8888
 * @ingroup frame
 * @param ini UYVY frame
 * @param out RGBX8888 frame
 */
uvc_error_t uvc_uyvy2rgbx(unsigned char* data, int data_bytes, int width, int height, uvc_frame_t *out) {

    out->width = width;
    out->height = height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->step = width * PIXEL_RGBX;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    uint8_t *pyuv = data;
    const uint8_t *pyuv_end = pyuv + data_bytes - PIXEL8_UYVY;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // UYVY => RGBX8888
#if USE_STRIDE
    if ((imageWidth * 3/2) && out->step && ((imageWidth * 3/2) != out->step)) {
		const int hh = height < out->height ? height : out->height;
		const int ww = width < out->width ? width : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = data + (imageWidth * 3/2) * h;
			prgbx = out->data + out->step * h;
			for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
				IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

				prgbx += PIXEL8_RGBX;
				pyuv += PIXEL8_UYVY;
				w += 8;
			}
		}
	} else {
		// compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
		for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
			IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

			prgbx += PIXEL8_RGBX;
			pyuv += PIXEL8_UYVY;
		}
	}
#else
    for (; (prgbx <= prgbx_end) && (pyuv <= pyuv_end) ;) {
        IUYVY2RGBX_8(pyuv, prgbx, 0, 0);

        prgbx += PIXEL8_RGBX;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

/** @brief Convert a frame from YUYV to RGBX8888
 * @ingroup frame
 * @param ini YUYV frame
 * @param out RGBX8888 frame
 */
uvc_error_t uvc_yuyv2_rgbx(unsigned char* data, int data_bytes, int width, int height, uvc_frame_t *out) {

    out->width = width;
    out->height = height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;
    out->step = width * PIXEL_RGBX;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    uint8_t *pyuv = data;
    const uint8_t *pyuv_end = pyuv + data_bytes - PIXEL8_YUYV;
    uint8_t *prgbx = out->data;
    const uint8_t *prgbx_end = prgbx + out->data_bytes - PIXEL8_RGBX;

    // YUYV => RGBX8888
#if USE_STRIDE
    if ((imageWidth * 3/2) && out->step && ((imageWidth * 3/2) != out->step)) {
		const int hh = height < out->height ? height : out->height;
		const int ww = width < out->width ? width : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = data + (imageWidth * 3/2) * h;
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


/** @brief Convert a frame from UYVY to RGB888
 * @ingroup frame
 * @param ini UYVY frame
 * @param out RGB888 frame
 */
uvc_error_t uvc_uyvy2rgb(unsigned char* data, int data_bytes, int width, int height, uvc_frame_t *out) {

    out->width = width;
    out->height = height;
    out->frame_format = UVC_FRAME_FORMAT_RGB;
    out->step = width * PIXEL_RGB;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    uint8_t *pyuv = data;
    const uint8_t *pyuv_end = pyuv + data_bytes - PIXEL8_UYVY;
    uint8_t *prgb = out->data;
    const uint8_t *prgb_end = prgb + out->data_bytes - PIXEL8_RGB;

    // UYVY => RGB888
#if USE_STRIDE
    if ((imageWidth * 3/2) && out->step && ((imageWidth * 3/2) != out->step)) {
		const int hh = height < out->height ? height : out->height;
		const int ww = width < out->width ? width : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = data + (imageWidth * 3/2) * h;
			prgb = out->data + out->step * h;
			for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
				IUYVY2RGB_8(pyuv, prgb, 0, 0);

				prgb += PIXEL8_RGB;
				pyuv += PIXEL8_UYVY;
				w += 8;
			}
		}
	} else {
		// compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
		for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {
			IUYVY2RGB_8(pyuv, prgb, 0, 0);

			prgb += PIXEL8_RGB;
			pyuv += PIXEL8_UYVY;
		}
	}
#else
    for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {

        IUYVY2RGB_8(pyuv, prgb, 0, 0);

        prgb += PIXEL8_RGB;
        pyuv += PIXEL8_UYVY;
    }
#endif
    return UVC_SUCCESS;
}

/** @brief Convert a frame from YUYV to RGB888
 * @ingroup frame
 *
 * @param in YUYV frame
 * @param out RGB888 frame
 */
uvc_error_t uvc_yuyv2rgb(unsigned char* data, int data_bytes, int width, int height, uvc_frame_t *out) {

    out->width = width;
    out->height = height;
    out->frame_format = UVC_FRAME_FORMAT_RGB;
    out->step = width * PIXEL_RGB;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    uint8_t *pyuv = data;
    const uint8_t *pyuv_end = pyuv + data_bytes - PIXEL8_YUYV;
    uint8_t *prgb = out->data;
    const uint8_t *prgb_end = prgb + out->data_bytes - PIXEL8_RGB;

#if USE_STRIDE
    if ((imageWidth * 3/2) && out->step && ((imageWidth * 3/2) != out->step)) {
		const int hh = height < out->height ? height : out->height;
		const int ww = width < out->width ? width : out->width;
		int h, w;
		for (h = 0; h < hh; h++) {
			w = 0;
			pyuv = data + (imageWidth * 3/2) * h;
			prgb = out->data + out->step * h;
			for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) && (w < ww) ;) {
				IYUYV2RGB_8(pyuv, prgb, 0, 0);

				prgb += PIXEL8_RGB;
				pyuv += PIXEL8_YUYV;
				w += 8;
			}
		}
	} else {
		// compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
		for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {
			IYUYV2RGB_8(pyuv, prgb, 0, 0);

			prgb += PIXEL8_RGB;
			pyuv += PIXEL8_YUYV;
		}
	}
#else
    // YUYV => RGB888
    for (; (prgb <= prgb_end) && (pyuv <= pyuv_end) ;) {
        IYUYV2RGB_8(pyuv, prgb, 0, 0);

        prgb += PIXEL8_RGB;
        pyuv += PIXEL8_YUYV;
    }
#endif
    return UVC_SUCCESS;
}


/** @brief Convert an MJPEG frame to RGBX
 * @ingroup frame
 *
 * @param in MJPEG frame
 * @param out RGBX frame
 */
uvc_error_t uvc_mjpeg2rgbx(unsigned char* data, int data_size, int width, int height, uvc_frame_t *out) {
    // variables:
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    //unsigned int width, height;
// data points to the mjpeg frame received from v4l2.
    //unsigned char *data;
    //size_t data_size;
// a *to be allocated* heap array to put the data for
// all the pixels after conversion to RGB.
    //unsigned char *pixels;

// ... In the initialization of the program:
    cinfo.err = jpeg_std_error(&jerr);
    jpeg_create_decompress(&cinfo);
    //pixels = new unsigned char[width * height * sizeof(Pixel)];

// ... Every frame:
    if (!(data == nullptr) && data_size > 0) {
        jpeg_mem_src(&cinfo, data, data_size);
        int rc = jpeg_read_header(&cinfo, TRUE);
        if (cinfo.dc_huff_tbl_ptrs[0] == NULL) {
            insert_huff_tables(&cinfo);
        }
        jpeg_start_decompress(&cinfo);
        cinfo.out_color_space = JCS_EXT_RGBA;
        cinfo.dct_method = JDCT_IFAST;


        while (cinfo.output_scanline < cinfo.output_height) {
            unsigned char *temp_array[] = {out->data + (cinfo.output_scanline) * width * 3};
            jpeg_read_scanlines(&cinfo, temp_array, 1);
        }

        jpeg_finish_decompress(&cinfo);
    }


    return UVC_SUCCESS;





    /*
    struct jpeg_decompress_struct dinfo;
    struct error_mgr jerr;
    size_t lines_read;
    // local copy
    uint8_t *data = out->data;
    const int out_step = out->step;

    int num_scanlines, i;
    lines_read = 0;
    unsigned char *buffer[MAX_READLINE];

    out->actual_bytes = 0;	// XXX

    if (uvc_ensure_frame_size(out, width * height * 4) < 0)
        return UVC_ERROR_NO_MEM;

    out->width = width;
    out->height = height;
    out->frame_format = UVC_FRAME_FORMAT_RGBX;	// XXX
    out->step = width * 4;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;

    dinfo.err = jpeg_std_error(&jerr.super);
    jerr.super.error_exit = _error_exit;
    if (setjmp(jerr.jmp)) {
        LOGD("goto FAIL !!");
        goto fail;
    }

    jpeg_create_decompress(&dinfo);
    jpeg_mem_src(&dinfo, in_data, data_bytes);	// XXX
    jpeg_read_header(&dinfo, TRUE);

    if (dinfo.dc_huff_tbl_ptrs[0] == NULL) {
        insert_huff_tables(&dinfo);
    }

    dinfo.out_color_space = JCS_EXT_RGBA;
    dinfo.dct_method = JDCT_IFAST;

    jpeg_start_decompress(&dinfo);

    if (LIKELY(dinfo.output_height == out->height)) {
        for (; dinfo.output_scanline < dinfo.output_height ;) {
            buffer[0] = data + (lines_read) * out_step;
            for (i = 1; i < MAX_READLINE; i++)
                buffer[i] = buffer[i-1] + out_step;
            num_scanlines = jpeg_read_scanlines(&dinfo, buffer, MAX_READLINE);
            lines_read += num_scanlines;
        }
        out->actual_bytes = width * height * 4;	// XXX
    }
    jpeg_finish_decompress(&dinfo);
    jpeg_destroy_decompress(&dinfo);
    if (lines_read == out->height) LOGD ("UVC_SUCCESS");
    else LOGD ("UVC_FAILED");
    return lines_read == out->height ? UVC_SUCCESS : UVC_ERROR_OTHER;	// XXX

    fail:
    jpeg_destroy_decompress(&dinfo);
    return UVC_ERROR_OTHER+1;


    */

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

/** @brief Duplicate a frame, preserving color format
 * @ingroup frame
 *
 * @param in Original frame
 * @param out Duplicate frame
 */
uvc_error_t uvc_duplicate_frame(uvc_frame_t *out) {

    out->width = imageWidth;
    out->height = imageHeight;
    out->frame_format = UVC_FRAME_FORMAT_YUYV;
    if (out->library_owns_data)
        out->step = imageWidth*2;
    out->sequence = 0;
    gettimeofday(&out->capture_time, NULL);
    out->source = devh;
    out->actual_bytes = total;	// XXX

#if USE_STRIDE	 // XXX
    if (in->step && out->step) {
		const int istep = imageWidth*2;
		const int ostep = out->step;
		const int hh = imageHeight < out->height ? imageHeight : out->height;
		const int rowbytes = istep < ostep ? istep : ostep;
		register void *ip = videoFrameData->videoframe;
		register void *op = out->data;
		int h;
		for (h = 0; h < hh; h += 4) {
			memcpy(op, ip, rowbytes);
			ip += istep; op += ostep;
			memcpy(op, ip, rowbytes);
			ip += istep; op += ostep;
			memcpy(op, ip, rowbytes);
			ip += istep; op += ostep;
			memcpy(op, ip, rowbytes);
			ip += istep; op += ostep;
		}
	} else {
		// compressed format? XXX if only one of the frame in / out has step, this may lead to crash...
		memcpy(out->data, in->data, in->actual_bytes);
	}
#else
    memcpy(out->data, videoFrameData->videoframe, total); // XXX
#endif
    return UVC_SUCCESS;
}


uvc_error_t uvc_yuyv2iyuv420SP(uvc_frame_t *in, uvc_frame_t *out) {


    if (UNLIKELY(uvc_ensure_frame_size(out, (in->width * in->height * 3) / 2) < 0))
        return UVC_ERROR_NO_MEM;

    const uint8_t *src = in->data;
    uint8_t *dest =out->data;
    const int32_t width = in->width;
    const int32_t height = in->height;
    const int32_t src_width = in->step;
    const int32_t src_height = in->height;
    const int32_t dest_width = out->width = out->step = in->width;
    const int32_t dest_height = out->height = in->height;

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
            *(uv++) = yuv[3];	// v
            *(uv++) = yuv[1];	// u
            *(uv++) = yuv[7];	// v
            *(uv++) = yuv[5];	// u
            *(y1++) = yuv[src_width+0];	// y on next low
            *(y1++) = yuv[src_width+2];	// y' on next low
            *(y1++) = yuv[src_width+4];	// y''  on next low
            *(y1++) = yuv[src_width+6];	// y'''  on next low
            yuv += 8;	// (1pixel=2bytes)x4pixels=8bytes
        }
    }

    return(UVC_SUCCESS);
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
// For capturing Images, Videos
jmethodID javaPictureVideoCaptureYUV;
jmethodID javaPictureVideoCaptureMJPEG;
jmethodID javaPictureVideoCaptureRGB;



// SERVICE
jmethodID javaServicePublishResults;
jmethodID javaServiceReturnToStreamActivity;


// WebRtc
jmethodID javaRetrievedFrameFromLibUsb21;
jmethodID javaProcessReceivedMJpegVideoFrameKamera;
jmethodID javaWEBrtcProcessReceivedVideoFrameYuv;

////////////////////////////////////////////////////////////////////////// YUV Methods

int rotation = 0;
volatile bool horizontalFlip = false;
volatile bool verticalFlip = false;
volatile bool imageCapture = false;
volatile bool imageCapturelongClick = false;

volatile bool videoCapture = false;
volatile bool videoCaptureLongClick = false;


void setRotation(int rot, int horizontalFl, int verticalFl) {
    rotation = rot;
    if (horizontalFl == 1) horizontalFlip = true;
    else horizontalFlip = false;
    if (verticalFl == 1) verticalFlip = true;
    else verticalFlip = false;
    LOGD("Rotation SET !!!!! horizontalFlip = %d,    verticalFlip = %d", horizontalFl, verticalFl);
}

int ARGBCopy(const uint8_t* src_argb,
             int src_stride_argb,
             uint8_t* dst_argb,
             int dst_stride_argb,
             int width,
             int height) {
    if (!src_argb || !dst_argb || width <= 0 || height == 0) {
        return -1;
    }
    // Negative height means invert the image.
    if (height < 0) {
        height = -height;
        src_argb = src_argb + (height - 1) * src_stride_argb;
        src_stride_argb = -src_stride_argb;
    }

    CopyPlane(src_argb, src_stride_argb, dst_argb, dst_stride_argb, width * 4,
              height);
    return 0;
}

void ScaleARGBRowDownEven_C(const uint8_t* src_argb,
                            ptrdiff_t src_stride,
                            int src_stepx,
                            uint8_t* dst_argb,
                            int dst_width) {
    const uint32_t* src = (const uint32_t*)(src_argb);
    uint32_t* dst = (uint32_t*)(dst_argb);
    (void)src_stride;
    int x;
    for (x = 0; x < dst_width - 1; x += 2) {
        dst[0] = src[0];
        dst[1] = src[src_stepx];
        src += src_stepx * 2;
        dst += 2;
    }
    if (dst_width & 1) {
        dst[0] = src[0];
    }
}

static int ARGBTranspose(const uint8_t* src_argb,
                         int src_stride_argb,
                         uint8_t* dst_argb,
                         int dst_stride_argb,
                         int width,
                         int height) {
    int i;
    int src_pixel_step = src_stride_argb >> 2;
    void (*ScaleARGBRowDownEven)(
            const uint8_t* src_argb, ptrdiff_t src_stride_argb, int src_step,
            uint8_t* dst_argb, int dst_width) = ScaleARGBRowDownEven_C;
    // Check stride is a multiple of 4.
    if (src_stride_argb & 3) {
        return -1;
    }
#if defined(HAS_SCALEARGBROWDOWNEVEN_SSE2)
    if (TestCpuFlag(kCpuHasSSE2)) {
    ScaleARGBRowDownEven = ScaleARGBRowDownEven_Any_SSE2;
    if (IS_ALIGNED(height, 4)) {  // Width of dest.
      ScaleARGBRowDownEven = ScaleARGBRowDownEven_SSE2;
    }
  }
#endif
#if defined(HAS_SCALEARGBROWDOWNEVEN_NEON)
    if (TestCpuFlag(kCpuHasNEON)) {
    ScaleARGBRowDownEven = ScaleARGBRowDownEven_Any_NEON;
    if (IS_ALIGNED(height, 4)) {  // Width of dest.
      ScaleARGBRowDownEven = ScaleARGBRowDownEven_NEON;
    }
  }
#endif
#if defined(HAS_SCALEARGBROWDOWNEVEN_MMI)
    if (TestCpuFlag(kCpuHasMMI)) {
    ScaleARGBRowDownEven = ScaleARGBRowDownEven_Any_MMI;
    if (IS_ALIGNED(height, 4)) {  // Width of dest.
      ScaleARGBRowDownEven = ScaleARGBRowDownEven_MMI;
    }
  }
#endif
#if defined(HAS_SCALEARGBROWDOWNEVEN_MSA)
    if (TestCpuFlag(kCpuHasMSA)) {
    ScaleARGBRowDownEven = ScaleARGBRowDownEven_Any_MSA;
    if (IS_ALIGNED(height, 4)) {  // Width of dest.
      ScaleARGBRowDownEven = ScaleARGBRowDownEven_MSA;
    }
  }
#endif

    for (i = 0; i < width; ++i) {  // column of source to row of dest.
        ScaleARGBRowDownEven(src_argb, 0, src_pixel_step, dst_argb, height);
        dst_argb += dst_stride_argb;
        src_argb += 4;
    }
    return 0;
}

static int ARGBRotate90(const uint8_t* src_argb,
                        int src_stride_argb,
                        uint8_t* dst_argb,
                        int dst_stride_argb,
                        int width,
                        int height) {
    // Rotate by 90 is a ARGBTranspose with the source read
    // from bottom to top. So set the source pointer to the end
    // of the buffer and flip the sign of the source stride.
    src_argb += src_stride_argb * (height - 1);
    src_stride_argb = -src_stride_argb;
    return ARGBTranspose(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                         width, height);
}

static int ARGBRotate270(const uint8_t* src_argb,
                         int src_stride_argb,
                         uint8_t* dst_argb,
                         int dst_stride_argb,
                         int width,
                         int height) {
    // Rotate by 270 is a ARGBTranspose with the destination written
    // from bottom to top. So set the destination pointer to the end
    // of the buffer and flip the sign of the destination stride.
    dst_argb += dst_stride_argb * (width - 1);
    dst_stride_argb = -dst_stride_argb;
    return ARGBTranspose(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                         width, height);
}

static int ARGBRotate180(const uint8_t* src_argb,
                         int src_stride_argb,
                         uint8_t* dst_argb,
                         int dst_stride_argb,
                         int width,
                         int height) {
    // Swap first and last row and mirror the content. Uses a temporary row.
    align_buffer_64(row, width * 4);
    const uint8_t* src_bot = src_argb + src_stride_argb * (height - 1);
    uint8_t* dst_bot = dst_argb + dst_stride_argb * (height - 1);
    int half_height = (height + 1) >> 1;
    int y;
    void (*ARGBMirrorRow)(const uint8_t* src_argb, uint8_t* dst_argb, int width) =
    ARGBMirrorRow_C;
    void (*CopyRow)(const uint8_t* src_argb, uint8_t* dst_argb, int width) =
    CopyRow_C;
#if defined(HAS_ARGBMIRRORROW_NEON)
    if (TestCpuFlag(kCpuHasNEON)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_NEON;
    if (IS_ALIGNED(width, 8)) {
      ARGBMirrorRow = ARGBMirrorRow_NEON;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_SSE2)
    if (TestCpuFlag(kCpuHasSSE2)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_SSE2;
    if (IS_ALIGNED(width, 4)) {
      ARGBMirrorRow = ARGBMirrorRow_SSE2;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_AVX2)
    if (TestCpuFlag(kCpuHasAVX2)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_AVX2;
    if (IS_ALIGNED(width, 8)) {
      ARGBMirrorRow = ARGBMirrorRow_AVX2;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_MMI)
    if (TestCpuFlag(kCpuHasMMI)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_MMI;
    if (IS_ALIGNED(width, 2)) {
      ARGBMirrorRow = ARGBMirrorRow_MMI;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_MSA)
    if (TestCpuFlag(kCpuHasMSA)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_MSA;
    if (IS_ALIGNED(width, 16)) {
      ARGBMirrorRow = ARGBMirrorRow_MSA;
    }
  }
#endif
#if defined(HAS_COPYROW_SSE2)
    if (TestCpuFlag(kCpuHasSSE2)) {
    CopyRow = IS_ALIGNED(width * 4, 32) ? CopyRow_SSE2 : CopyRow_Any_SSE2;
  }
#endif
#if defined(HAS_COPYROW_AVX)
    if (TestCpuFlag(kCpuHasAVX)) {
    CopyRow = IS_ALIGNED(width * 4, 64) ? CopyRow_AVX : CopyRow_Any_AVX;
  }
#endif
#if defined(HAS_COPYROW_ERMS)
    if (TestCpuFlag(kCpuHasERMS)) {
    CopyRow = CopyRow_ERMS;
  }
#endif
#if defined(HAS_COPYROW_NEON)
    if (TestCpuFlag(kCpuHasNEON)) {
    CopyRow = IS_ALIGNED(width * 4, 32) ? CopyRow_NEON : CopyRow_Any_NEON;
  }
#endif

    // Odd height will harmlessly mirror the middle row twice.
    for (y = 0; y < half_height; ++y) {
        ARGBMirrorRow(src_argb, row, width);      // Mirror first row into a buffer
        ARGBMirrorRow(src_bot, dst_argb, width);  // Mirror last row into first row
        CopyRow(row, dst_bot, width * 4);  // Copy first mirrored row into last
        src_argb += src_stride_argb;
        dst_argb += dst_stride_argb;
        src_bot -= src_stride_argb;
        dst_bot -= dst_stride_argb;
    }
    free_aligned_buffer_64(row);
    return 0;
}

int ARGBRotate(const uint8_t* src_argb,
               int src_stride_argb,
               uint8_t* dst_argb,
               int dst_stride_argb,
               int width,
               int height,
               enum RotationMode mode) {
    if (!src_argb || width <= 0 || height == 0 || !dst_argb) {
        return -1;
    }

    // Negative height means invert the image.
    if (height < 0) {
        height = -height;
        src_argb = src_argb + (height - 1) * src_stride_argb;
        src_stride_argb = -src_stride_argb;
    }

    switch (mode) {
        case kRotate0:
            // copy frame
            return ARGBCopy(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                            width, height);
        case kRotate90:
            return ARGBRotate90(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                                width, height);
        case kRotate270:
            return ARGBRotate270(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                                 width, height);
        case kRotate180:
            return ARGBRotate180(src_argb, src_stride_argb, dst_argb, dst_stride_argb,
                                 width, height);
        default:
            break;
    }
    return -1;
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

int ARGBMirror(const uint8_t* src_argb,
               int src_stride_argb,
               uint8_t* dst_argb,
               int dst_stride_argb,
               int width,
               int height) {
    int y;
    void (*ARGBMirrorRow)(const uint8_t* src, uint8_t* dst, int width) =
    ARGBMirrorRow_C;
    if (!src_argb || !dst_argb || width <= 0 || height == 0) {
        return -1;
    }
    // Negative height means invert the image.
    if (height < 0) {
        height = -height;
        src_argb = src_argb + (height - 1) * src_stride_argb;
        src_stride_argb = -src_stride_argb;
    }
#if defined(HAS_ARGBMIRRORROW_NEON)
    if (TestCpuFlag(kCpuHasNEON)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_NEON;
    if (IS_ALIGNED(width, 8)) {
      ARGBMirrorRow = ARGBMirrorRow_NEON;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_SSE2)
    if (TestCpuFlag(kCpuHasSSE2)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_SSE2;
    if (IS_ALIGNED(width, 4)) {
      ARGBMirrorRow = ARGBMirrorRow_SSE2;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_AVX2)
    if (TestCpuFlag(kCpuHasAVX2)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_AVX2;
    if (IS_ALIGNED(width, 8)) {
      ARGBMirrorRow = ARGBMirrorRow_AVX2;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_MMI)
    if (TestCpuFlag(kCpuHasMMI)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_MMI;
    if (IS_ALIGNED(width, 2)) {
      ARGBMirrorRow = ARGBMirrorRow_MMI;
    }
  }
#endif
#if defined(HAS_ARGBMIRRORROW_MSA)
    if (TestCpuFlag(kCpuHasMSA)) {
    ARGBMirrorRow = ARGBMirrorRow_Any_MSA;
    if (IS_ALIGNED(width, 16)) {
      ARGBMirrorRow = ARGBMirrorRow_MSA;
    }
  }
#endif

    // Mirror plane
    for (y = 0; y < height; ++y) {
        ARGBMirrorRow(src_argb, dst_argb, width);
        src_argb += src_stride_argb;
        dst_argb += dst_stride_argb;
    }
    return 0;
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





























///////////////////////////////////////////////////   STANDARD CAMERA FUNCTIONS

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
    LOGD("initStreamingParms");
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
// JNA Methods:
void setImageCapture() {    imageCapture = true;}
void setImageCaptureLongClick() {    imageCapturelongClick = true;}
void startVideoCapture() {   videoCapture = true;}
void stopVideoCapture() {    videoCapture = false;}
void startVideoCaptureLongClick() {    videoCaptureLongClick = true;}
void stopVideoCaptureLongClick() { videoCaptureLongClick = false;}


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

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetSurfaceView
        (JNIEnv *env, jobject obj, jobject jSurface) {
    if (jSurface != NULL ) {
        ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
        // WINDOW_FORMAT_RGBA_8888
        ANativeWindow_setBuffersGeometry(preview_window, imageWidth, imageHeight, WINDOW_FORMAT_RGBA_8888);
        mCaptureWindow = preview_window;
        LOGD("mCaptureWindow, JniSetSurfaceView");
    }
    int status = (*env)->GetJavaVM(env, &javaVm);
    if(status != 0) {
        LOGE("failed to attach javaVm");
    }
    class = (*env)->GetObjectClass(env, obj);
    jniHelperClass = (*env)->NewGlobalRef(env, class);
    mainActivityObj = (*env)->NewGlobalRef(env, obj);
    javaRetrievedStreamActivityFrameFromLibUsb = (*env)->GetMethodID(env, jniHelperClass, "retrievedStreamActivityFrameFromLibUsb", "([B)V");
    javaPictureVideoCaptureYUV  = (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureYUV", "([B)V");
    javaPictureVideoCaptureMJPEG  = (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureMJPEG", "([B)V");
    javaPictureVideoCaptureRGB =  (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureRGB", "([B)V");
}

JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivity_JniSetSurfaceYuv
        (JNIEnv *env, jobject obj, jobject jSurface) {
    if (jSurface != NULL ) {
        ANativeWindow *preview_window = jSurface ? ANativeWindow_fromSurface(env, jSurface) : NULL;
        // WINDOW_FORMAT_RGBA_8888
        ANativeWindow_setBuffersGeometry(preview_window, imageWidth, imageHeight, WINDOW_FORMAT_RGBA_8888);
        mCaptureWindow = preview_window;
        LOGD("mCaptureWindow, JniSetSurfaceYuv");

    }
    int status = (*env)->GetJavaVM(env, &javaVm);
    if(status != 0) {
        LOGE("failed to attach javaVm");
    }
    class = (*env)->GetObjectClass(env, obj);
    jniHelperClass = (*env)->NewGlobalRef(env, class);
    mainActivityObj = (*env)->NewGlobalRef(env, obj);
    javaRetrievedStreamActivityFrameFromLibUsb = (*env)->GetMethodID(env, jniHelperClass, "retrievedStreamActivityFrameFromLibUsb", "([B)V");
    javaPictureVideoCaptureYUV  = (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureYUV", "([B)V");
    javaPictureVideoCaptureMJPEG  = (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureMJPEG", "([B)V");
    javaPictureVideoCaptureRGB =  (*env)->GetMethodID(env, jniHelperClass, "pictureVideoCaptureRGB", "([B)V");
}

//////////// SERVICE

// Init SurfaceView for Service
JNIEXPORT void JNICALL Java_humer_UvcCamera_LibUsb_StartIsoStreamService_JniPrepairForStreamingfromService
        (JNIEnv *env, jobject obj) {
    if (initialized) {
        probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);
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
                ueberschreitungDerUebertragungslaenge = 0;
                if (frameUeberspringen == 0) {
                    ++totalFrame;
                    if (runningStream == false) stopStreaming();
                    if (strcmp(frameFormat, "MJPEG") == 0) {
                       // LOGD("MJPEG");



/*
                        uvc_frame_t *rgbx;
                        uvc_error_t ret;
                        // We'll convert the image from YUV/JPEG to BGR, so allocate space
                        rgbx = uvc_allocate_frame(imageWidth * imageHeight * 4);
                        if (!rgbx) {
                            LOGD("unable to allocate rgb frame!");
                            return;
                        }
                        ret = uvc_mjpeg2rgbx(videoFrameData->videoframe, total, imageWidth, imageHeight, rgbx);
                        if (ret == UVC_SUCCESS) LOGD("uvc_mjpeg2rgbx SUCESSFUL");
                        else LOGD("uvc_mjpeg2rgbx failed\n ret = %d",ret);
                        copyToSurface(rgbx, &mCaptureWindow);
                        uvc_free_frame(rgbx);
                        total = 0;



*/

                        //       -->   mjpeg2rgbx






                        JNIEnv * jenv;
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                        // Main Activity
                        (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaRetrievedStreamActivityFrameFromLibUsb, array);
                        total = 0;
                        //*/
                    } else {
                        //LOGD("YUV ...");


                        uvc_frame_t *rgbx;
                        uvc_error_t ret;
                        // We'll convert the image from YUV/JPEG to BGR, so allocate space
                        rgbx = uvc_allocate_frame(imageWidth * imageHeight * 4);
                        if (!rgbx) {
                            LOGD("unable to allocate rgb frame!");
                            return;
                        }
                        /*
                        // Do the BGR conversion
                        ret = uvc_yuyv2rgbx(rgb);
                        if (ret) {
                            uvc_perror(ret, "uvc_any2bgr");
                            uvc_free_frame(rgb);
                            return;
                        }
                        */
                        if (strcmp(frameFormat, "YUY2") == 0) {
                            ret = uvc_yuyv2_rgbx(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgbx);
                        } else if (strcmp(frameFormat, "UYVY") == 0) {
                            ret = uvc_uyvy2rgbx(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgbx);
                        }
                        uvc_frame_t *rgb_rot_nFlip = checkRotation(rgbx);
                        if (imageCapture) {
                            imageCapture = false;

                            uvc_frame_t *rgb;
                            uvc_error_t ret;
                            // We'll convert the image from YUV/JPEG to BGR, so allocate space
                            rgb = uvc_allocate_frame(imageWidth * imageHeight * 3);
                            if (!rgb) {
                                LOGD("unable to allocate rgb frame!");
                                return;
                            }

                            if (strcmp(frameFormat, "YUY2") == 0) {
                                ret = uvc_yuyv2rgb(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgb);
                            } else if (strcmp(frameFormat, "UYVY") == 0) {
                                ret = uvc_uyvy2rgb(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgb);
                            }


                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, imageWidth * imageHeight * 3);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, imageWidth * imageHeight * 3, (jbyte *) rgb->data);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPictureVideoCaptureRGB, array);



                            // Skip TurboJPEG
                            /*
                            const int JPEG_QUALITY = 100;
                            const int COLOR_COMPONENTS = 3;
                            int _width = rgb_rot_nFlip->width;
                            int _height = rgb_rot_nFlip->height;
                            long unsigned int _jpegSize = 0;
                            unsigned char* _compressedImage = NULL; //!< Memory is allocated by tjCompress2 if _jpegSize == 0
                            tjhandle _jpegCompressor = tjInitCompress();
                            int ret = tjCompress2(_jpegCompressor, rgb_rot_nFlip->data, _width, _width*4, _height, TJPF_RGBA,
                                        &_compressedImage, &_jpegSize, TJSAMP_444, JPEG_QUALITY,
                                                  TJFLAG_FASTDCT);
                            if (ret == 0) LOGD("tjCompress2 sucessful, ret = %d", ret);
                            else LOGD("tjCompress2 failed !!!!!!, ret = %d", ret);
                            if (ret != 0 ) return;
                            LOGD("_jpegSize = %d", _jpegSize);
                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, _jpegSize);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, _jpegSize, (jbyte *) _compressedImage);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPictureVideoCaptureMJPEG, array);
                            tjDestroy(_jpegCompressor);
                            */
                        } else if (imageCapturelongClick) {
                            imageCapturelongClick = false;
                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPictureVideoCaptureYUV, array);
                        }
                        if (videoCapture) {

                            uvc_frame_t *rgb;
                            uvc_error_t ret;
                            // We'll convert the image from YUV/JPEG to BGR, so allocate space
                            rgb = uvc_allocate_frame(imageWidth * imageHeight * 3);
                            if (!rgb) {
                                printf("unable to allocate rgb frame!");
                                return;
                            }

                            if (strcmp(frameFormat, "YUY2") == 0) {
                                ret = uvc_yuyv2rgb(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgb);
                            } else if (strcmp(frameFormat, "UYVY") == 0) {
                                ret = uvc_uyvy2rgb(videoFrameData->videoframe, imageWidth * imageHeight * 3, imageWidth, imageHeight, rgb);
                            }
                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, imageWidth * imageHeight * 3);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, imageWidth * imageHeight * 3, (jbyte *) rgb->data);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPictureVideoCaptureRGB, array);
                        } else if (videoCaptureLongClick) {
                            JNIEnv * jenv;
                            int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                            jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                            // Main Activity
                            (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                            (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaPictureVideoCaptureYUV, array);
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
                    if (strcmp(frameFormat, "MJPEG") == 0) {

                        JNIEnv * jenv;
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                        (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                        // Service
                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaProcessReceivedMJpegVideoFrameKamera, array);
                        total = 0;
                    } else {


                        LOGD("frameFormat = %s", frameFormat);


                        uvc_frame_t *yuyv;
                        uvc_error_t ret;
                        // We'll convert the image from YUV/JPEG to BGR, so allocate space
                        yuyv = uvc_allocate_frame(imageWidth * imageHeight *3/ 2);

                        uvc_duplicate_frame(yuyv);



                        uvc_frame_t *nv21;
                        // We'll convert the image from YUV/JPEG to BGR, so allocate space
                        nv21 = uvc_allocate_frame(imageWidth * imageHeight *3/ 2);

                        uvc_yuyv2iyuv420SP (yuyv, nv21);



                        JNIEnv * jenv;
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        jbyteArray array = (*jenv)->NewByteArray(jenv, nv21->data_bytes);
                        (*jenv)->SetByteArrayRegion(jenv, array, 0, nv21->data_bytes, (jbyte *) nv21->data);
                        // Service

                        LOGD("CallVoidMethod");


                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaRetrievedFrameFromLibUsb21, array);

                        uvc_free_frame(nv21);

/*

                        JNIEnv * jenv;
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        jbyteArray array = (*jenv)->NewByteArray(jenv, total);
                        (*jenv)->SetByteArrayRegion(jenv, array, 0, total, (jbyte *) videoFrameData->videoframe);
                        // Service

                        LOGD("CallVoidMethod");


                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaWEBrtcProcessReceivedVideoFrameYuv, array);
                        /*
                         * uvc_frame_t *nv21;
                        uvc_error_t ret;

                        nv21 = uvc_allocate_frame(imageWidth * imageHeight * 3 /2);
                        if (!nv21) {
                            LOGD("unable to allocate rgb frame!");
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
                        LOGD("nv21->data_bytes = %d", nv21->data_bytes);
                        int errorCode = (*javaVm)->AttachCurrentThread(javaVm, (void**) &jenv, NULL);
                        LOGD("jbyteArray array");

                        jbyteArray array = (*jenv)->NewByteArray(jenv, nv21->data_bytes);
                        LOGD("SetByteArrayRegion");

                        (*jenv)->SetByteArrayRegion(jenv, array, 0, nv21->data_bytes, (jbyte *) nv21->data);
                        // Service
                        LOGD("calling method");
                        (*jenv)->CallVoidMethod(jenv, mainActivityObj, javaRetrievedFrameFromLibUsb, array);
                        LOGD("Method called");
                        uvc_free_frame(nv21);
                        */
                    }
                    total = 0;
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
    int status = (*env)->GetJavaVM(env, &javaVm);
    if(status != 0) {
        LOGE("failed to attach javaVm");
    }
    class = (*env)->GetObjectClass(env, obj);
    jniHelperClass = (*env)->NewGlobalRef(env, class);
    mainActivityObj = (*env)->NewGlobalRef(env, obj);
    javaRetrievedFrameFromLibUsb21 = (*env)->GetMethodID(env, jniHelperClass, "retrievedFrameFromLibUsbNV21", "([B)V");
    javaProcessReceivedMJpegVideoFrameKamera = (*env)->GetMethodID(env, jniHelperClass, "processReceivedMJpegVideoFrameKamera", "([B)V");
    javaWEBrtcProcessReceivedVideoFrameYuv = (*env)->GetMethodID(env, jniHelperClass, "usbCapturerProcessReceivedVideoFrameYuvFromJni", "([B)V");
    LOGD("WeBRTC - Native Methods and Values from JNI initialized");
}

void prepairTheStream_WebRtc_Service() {

    probeCommitControl(bmHint, camFormatIndex, camFrameIndex, camFrameInterval, fd);

    int r = libusb_set_interface_alt_setting(devh, camStreamingInterfaceNum,
                                             camStreamingAltSetting); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(devh, 1, 1) failed with error %d\n", r);
    } else {
        LOGD("Altsettings sucessfully set:\nAltsetting = %d\n",
             camStreamingAltSetting);
    }
    if (activeUrbs > 16) activeUrbs = 16;
}

void lunchTheStream_WebRtc_Service() {

    if (strcmp(frameFormat, "MJPEG") == 0) {
        //cinfo.err = jpeg_std_error(&jerr);
        //jpeg_create_decompress(&cinfo);
    }




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

// Frame Conversation:

unsigned char* convertUYVYtoJPEG (unsigned char* UYVY_frame_array, int* jpgLength, int UYVYframeLength, int width, int height) {
    uvc_frame_t *rgb;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgb = uvc_allocate_frame(width * height * 4);
    if (!rgb) {
        printf("unable to allocate rgb frame!");
        return NULL;
    }
    uvc_error_t ret;


    ret = uvc_uyvy2rgbx(UYVY_frame_array, UYVYframeLength, width, height, rgb);


    if (ret) {
        uvc_perror(ret, "uvc_any2bgr");
        uvc_free_frame(rgb);
        return NULL;
    }
    const int JPEG_QUALITY = 100;
    const int COLOR_COMPONENTS = 3;
    int _width = rgb->width;
    int _height = rgb->height;
    long unsigned int _jpegSize = 0;
    unsigned char* _compressedImage = NULL; //!< Memory is allocated by tjCompress2 if _jpegSize == 0
    tjhandle _jpegCompressor = tjInitCompress();
    int r = tjCompress2(_jpegCompressor, rgb->data, _width, _width*4, _height, TJPF_RGBA,
                          &_compressedImage, &_jpegSize, TJSAMP_444, JPEG_QUALITY,
                          TJFLAG_FASTDCT);
    if (r == 0) LOGD("tjCompress2 sucessful, ret = %d", ret);
    else LOGD("tjCompress2 failed !!!!!!, ret = %d", ret);
    if (r != 0 ) return NULL;
    LOGD("_jpegSize = %d", _jpegSize);
    *jpgLength = _jpegSize;
    tjDestroy(_jpegCompressor);
    return _compressedImage;
}

unsigned char* convertYUY2toJPEG (unsigned char* YUY2_frame_array, int* jpgLength, int YUY2frameLength, int width, int height) {

    uvc_frame_t *rgb;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgb = uvc_allocate_frame(width * height * 4);
    if (!rgb) {
        printf("unable to allocate rgb frame!");
        return NULL;
    }
    uvc_error_t ret;
    ret = uvc_yuyv2_rgbx(YUY2_frame_array, YUY2frameLength, width, height, rgb);
    if (ret) {
        uvc_perror(ret, "uvc_any2bgr");
        uvc_free_frame(rgb);
        return NULL;
    }
    const int JPEG_QUALITY = 100;
    const int COLOR_COMPONENTS = 3;
    int _width = rgb->width;
    int _height = rgb->height;
    long unsigned int _jpegSize = 0;
    unsigned char* _compressedImage = NULL; //!< Memory is allocated by tjCompress2 if _jpegSize == 0
    tjhandle _jpegCompressor = tjInitCompress();
    int r = tjCompress2(_jpegCompressor, rgb->data, _width, _width*4, _height, TJPF_RGBA,
                        &_compressedImage, &_jpegSize, TJSAMP_444, JPEG_QUALITY,
                        TJFLAG_FASTDCT);
    if (r == 0) LOGD("tjCompress2 sucessful, ret = %d", ret);
    else LOGD("tjCompress2 failed !!!!!!, ret = %d", ret);
    if (r != 0 ) return NULL;
    LOGD("_jpegSize = %d", _jpegSize);
    *jpgLength = _jpegSize;
    tjDestroy(_jpegCompressor);
    return _compressedImage;
}


void
Java_humer_UvcCamera_StartIsoStreamActivity_UYVYpixeltobmp( JNIEnv* env, jobject thiz, jbyteArray data, jobject bitmap, int im_width, int im_height){

    jsize num_bytes = (*env)->GetArrayLength(env, data);
    unsigned char* UYVY_frame_array;
    UYVY_frame_array = (char *) malloc (num_bytes);
    jbyte  *lib = (*env)->GetByteArrayElements(env , data, 0);
    memcpy ( UYVY_frame_array , lib , num_bytes ) ;

    uvc_frame_t *rgbx;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgbx = uvc_allocate_frame(im_width * im_height * 4);
    if (!rgbx) {
        LOGD("unable to allocate rgb frame!");
        return;
    }
    uvc_error_t ret;
    int UYVYframeLength = num_bytes;

    ret = uvc_uyvy2rgbx(UYVY_frame_array, UYVYframeLength, im_width, im_height, rgbx);

    AndroidBitmapInfo  info;
    void*              pixels;
    int i;
    int *colors;
    int width=0;
    int height=0;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGD("AndroidBitmap_getInfo() failed ! error=%d", ret);
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    width = info.width;
    height = info.height;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGD("Bitmap format is not RGBA_8888 !");
        LOGE("Bitmap format is not RGBA_8888 !");
        return;
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGD("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    memcpy(pixels, rgbx->data, width*height*4);
    AndroidBitmap_unlockPixels(env, bitmap);
}



void
Java_humer_UvcCamera_StartIsoStreamActivity_YUY2pixeltobmp( JNIEnv* env, jobject thiz, jbyteArray data, jobject bitmap, int im_width, int im_height){
    jsize num_bytes = (*env)->GetArrayLength(env, data);
    unsigned char* YUY2_frame_array;
    YUY2_frame_array = (char *) malloc (num_bytes);
    jbyte  *lib = (*env)->GetByteArrayElements(env , data, 0);
    memcpy ( YUY2_frame_array , lib , num_bytes ) ;
    uvc_frame_t *rgbx;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgbx = uvc_allocate_frame(im_width * im_height * 4);
    if (!rgbx) {
        LOGD("unable to allocate rgb frame!");
        return;
    }
    uvc_error_t ret;
    int YUY2frameLength = num_bytes;
    ret = uvc_yuyv2_rgbx(YUY2_frame_array, YUY2frameLength, im_width, im_height, rgbx);
    AndroidBitmapInfo  info;
    void*              pixels;
    int i;
    int *colors;
    int width=0;
    int height=0;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGD("AndroidBitmap_getInfo() failed ! error=%d", ret);
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return;
    }
    width = info.width;
    height = info.height;
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGD("Bitmap format is not RGBA_8888 !");
        LOGE("Bitmap format is not RGBA_8888 !");
        return;
    }
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGD("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    memcpy(pixels, rgbx->data, width*height*4);
    AndroidBitmap_unlockPixels(env, bitmap);
}