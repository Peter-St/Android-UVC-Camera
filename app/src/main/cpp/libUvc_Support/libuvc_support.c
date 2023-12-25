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

#include "uvc_support.h"
#include "../libyuv/include/libyuv/rotate.h"
#include "../libyuv/include/libyuv/row.h"
#include "../libyuv/include/libyuv/rotate_argb.h"
#include "../libyuv/include/libyuv/cpu_id.h"
#include "../libyuv/include/libyuv/planar_functions.h"
#include <android/bitmap.h>
#include <setjmp.h>
//#include "jpeglib.h"
//#include "../UVCPreview.h"

volatile bool write_Ctl_Buffer = false;
int ueberschreitungDerUebertragungslaenge = 0 ;

int verbose = 0;
#define USE_EOF


// Camera Values
/*
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
volatile bool auto_values_collected = false;
volatile bool runningStreamFragment = false;
static uint8_t frameUeberspringen = 0;
static uint8_t numberOfAutoFrames;
static int streamEndPointAdressOverNative;

volatile int stopTheStream;
*/

// uvc_context_t *uvcContext_global;
// uvc_device_t *mDevice_global;
// uvc_device_handle_t *uvcDeviceHandle_global;
// uvc_stream_ctrl_t *global_UVC_ctrl;


//libusb_device_handle *devh = NULL;

//AutotransferStruct autoStruct;
//ManualTestFrameStruct manualFrameStruct;
clock_t startTime;
bool exit_stream = false;
// Skip Flip for LowerAndroid
bool low_Android = false;

int initStreamingParmsIntArray[3];
int probedStreamingParmsIntArray[3];
int finalStreamingParmsIntArray_first[3];
int finalStreamingParmsIntArray[3];
autoStreamComplete autoStreamfinished = NULL;
void setAutoStreamComplete(autoStreamComplete autoStream)
{
    autoStreamfinished = autoStream;
}
/*
AutotransferStruct get_autotransferStruct () {
    return autoStruct;
}
*/

uint8_t streamControl[48];
uint8_t unpackUsbInt(uint8_t *p, int i);

bool camIsOpen = false;
uint8_t libusb_Is_initialized;
uint8_t cameraDevice_is_wraped;
uint8_t cameraDevice_is_rewraped;


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

// CALLBACK METHODS

eventCallback sendReceivedDataToJava = NULL;
void setCallback(eventCallback evnHnd) {
    sendReceivedDataToJava = evnHnd;
}
  //AUTO DETECT METHOID
eventCallbackAuto automatic_callback = NULL;
void setCallbackAuto(eventCallbackAuto evnHnd) {
    automatic_callback = evnHnd;
}

frameComplete sendFrameComplete = NULL;
void setFrameComplete(frameComplete evnHnd){
    sendFrameComplete = evnHnd;
}

jnaFrameCallback jnaSendFrameToJava = NULL;
void setJnaFrameCallback(jnaFrameCallback evnHnd) {
    jnaSendFrameToJava = evnHnd;
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


static inline unsigned char sat(int i) {
    return (unsigned char) (i >= 255 ? 255 : (i < 0 ? 0 : i));
}
/*
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
*/


void stopStreaming(uvc_camera_t *uvc_camera) {
    LOGD("native stopStreaming");
    uvc_camera->runningStream = false;
}



int findJpegSegment(unsigned char* a, int dataLen, int segmentType) {
    int p = 2;
    while (p <= dataLen - 6) {
        if ((a[p] & 0xff) != 0xff) {
            LOGD("Unexpected JPEG data structure (marker expected).");
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

// see USB video class standard, USB_Video_Payload_MJPEG_1.5.pdf
unsigned char * convertMjpegFrameToJpeg(unsigned char* frameData, int data_size,  int* jpglength_ptr) {
    int frameLen = data_size;
    while (frameLen > 0 && frameData[frameLen - 1] == 0) {
        frameLen--;
    }
    if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9) {
        LOGD("Invalid MJPEG frame structure, length= %d", data_size);
    }
    bool hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
    exit_stream = false;
    if (hasHuffmanTable) {
        if (data_size == frameLen) {
            *jpglength_ptr = data_size;
            return frameData;
        }
        unsigned char *dest =  (unsigned char*)malloc(sizeof(unsigned char)*frameLen);
        memcpy(dest, frameData, frameLen);
        *jpglength_ptr = frameLen;
        return dest;
    } else {
        int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);

        if (segmentDaPos == -1) {
            exit_stream = true;
            LOGD("Segment 0xDA not found in MJPEG frame data.");}
        //          throw new Exception("Segment 0xDA not found in MJPEG frame data.");
        if (exit_stream ==false) {
            int huflength = sizeof(mjpgHuffmanTable) / sizeof(unsigned char);
            int newlength = frameLen + huflength;
            unsigned char * a = (unsigned char*)malloc(sizeof(unsigned char)*newlength);
            memcpy(a, frameData,  segmentDaPos);
            memcpy(a + segmentDaPos, mjpgHuffmanTable,  huflength);
            memcpy(a + segmentDaPos + huflength, frameData + segmentDaPos,  frameLen - segmentDaPos);
            *jpglength_ptr = newlength;
            return a;
        } else
            return NULL;
    }
}


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



/** @internal
* @brief Find the descriptor for a specific frame configuration
* @param stream_if Stream interface
* @param format_id Index of format class descriptor
* @param frame_id Index of frame descriptor
*/
static uvc_frame_desc_t *_uvc_find_frame_desc_stream_if(
        uvc_streaming_interface_t *stream_if, uint16_t format_id,
        uint16_t frame_id) {

    uvc_format_desc_t *format = NULL;
    uvc_frame_desc_t *frame = NULL;

    DL_FOREACH(stream_if->format_descs, format)
    {
        if (format->bFormatIndex == format_id) {
            DL_FOREACH(format->frame_descs, frame)
            {
                if (frame->bFrameIndex == frame_id)
                    return frame;
            }
        }
    }

    return NULL ;
}

/** @internal
 * @brief Find the descriptor for a specific frame configuration
 * @param devh UVC device
 * @param format_id Index of format class descriptor
 * @param frame_id Index of frame descriptor
 */
uvc_frame_desc_t *uvc_find_frame_desc(uvc_device_handle_t *devh, uint16_t format_id, uint16_t frame_id) {
    uvc_streaming_interface_t *stream_if;
    uvc_frame_desc_t *frame;
    DL_FOREACH(devh->info->stream_ifs, stream_if)
    {
        frame = _uvc_find_frame_desc_stream_if(stream_if, format_id, frame_id);
        if (frame)
            return frame;
    }

    return NULL;
}

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

    CopyPlane(src_argb, src_stride_argb, dst_argb, dst_stride_argb, width * 4, height);
    return 0;
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
/*
    uvc_frame_t *flip_img = uvc_allocate_frame(imageWidth * imageHeight * 4);
    flip_img->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (flip_img->library_owns_data)
        flip_img->step = imageWidth * PIXEL_RGBX;
    flip_img->sequence = 0;
    flip_img->capture_time = frame->capture_time;
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

    */
}

uvc_frame_t *checkFlip(uvc_frame_t *rot_img) {
    if (!verticalFlip && !horizontalFlip) return rot_img;
    else if (verticalFlip && horizontalFlip) flip_vertically(rot_img);
    if (verticalFlip) { flip_vertically(rot_img); return flip_horizontal(rot_img); }
    if (horizontalFlip) return flip_horizontal(rot_img);

    uvc_frame_t *frame;






    return rot_img;
}

uvc_frame_t *checkRotation(uvc_frame_t *rgbx) {
    /*
    if (rotation == 0) return checkFlip(rgbx);
    uvc_frame_t *rot_img = uvc_allocate_frame(imageWidth * imageHeight * 4);
    rot_img->frame_format = UVC_FRAME_FORMAT_RGBX;
    if (rot_img->library_owns_data)
        rot_img->step = imageWidth * PIXEL_RGBX;
    rot_img->sequence = 0;
    rot_img->capture_time = rgbx->capture_time;
    int src_stride_argb = imageWidth * 4;
    uint* dst_argb = rot_img->data;
    int src_width = imageWidth;
    int src_height = imageHeight;

    if (rotation == 90) {
        rot_img->width = imageHeight ;
        rot_img->height = imageWidth;
        int dst_stride_argb = imageHeight * 4;
        enum RotationMode mode = kRotate90;
        ARGBRotate(rgbx->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgbx);
        return checkFlip(rot_img);
    } else if (rotation == 180) {
        rot_img->width = imageWidth;
        rot_img->height = imageHeight;
        int dst_stride_argb = imageWidth * 4;
        enum RotationMode mode = kRotate180;
        ARGBRotate(rgbx->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgbx);
        return checkFlip(rot_img);
    } else if (rotation == 270) {
        rot_img->width = imageHeight ;
        rot_img->height = imageWidth;
        int dst_stride_argb = imageHeight * 4;
        enum RotationMode mode = kRotate270;
        ARGBRotate(rgbx->data,  src_stride_argb,
                   rot_img->data, dst_stride_argb,
                   src_width, src_height,
                   mode);
        uvc_free_frame(rgbx);
        return checkFlip(rot_img);
    }
    return rot_img;
    */
}

///////////////////////////////////////////////////   STANDARD CAMERA FUNCTIONS  /////////////////////////////////////////

void initStreamingParms_controltransfer(uvc_camera_t *uvc_camera, libusb_device_handle *handle, bool createPointer) {
    LOGD("bool createPointer = %d", createPointer);
    size_t length;
    if (uvc_camera->bcdUVC >= 0x0150)
        length = 48;
    else if (uvc_camera->bcdUVC >= 0x0110)
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
    for (int i = 0; i < length; i++) {
        buffer[i] = 0x00;
    }
    buffer[0] = uvc_camera->bmHint; // what fields shall be kept fixed (0x01: dwFrameInterval)
    buffer[1] = 0x00; //
    buffer[2] = uvc_camera->camFormatIndex; // video format index
    buffer[3] = uvc_camera->camFrameIndex; // video frame index
    buffer[4] = (uvc_camera->camFrameInterval & 0xFF); // interval
    buffer[5] = ((uvc_camera->camFrameInterval >> 8)& 0xFF); //   propose:   0x4c4b40 (500 ms)
    buffer[6] = ((uvc_camera->camFrameInterval >> 16)& 0xFF); //   agreement: 0x1312d0 (125 ms)
    buffer[7] = ((uvc_camera->camFrameInterval >> 24)& 0xFF); //
    int b;

    if (createPointer == true) {
        memcpy(ctl_transfer_Data->ctl_transfer_values, buffer , length);
    }
    getStreamingParmsArray(initStreamingParmsIntArray , buffer);
    // wanted Pharms
    LOGD("initStreamingParmsIntArray[0] = %d", initStreamingParmsIntArray[0]);
    LOGD("initStreamingParmsIntArray[1] = %d", initStreamingParmsIntArray[1]);
    LOGD("initStreamingParmsIntArray[2] = %d", initStreamingParmsIntArray[2]);
    int len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_SET, SET_CUR, (VS_PROBE_CONTROL << 8), uvc_camera->camStreamingInterfaceNum,
                                      buffer, sizeof (buffer), 2000);
    if (len != sizeof (buffer)) {
        LOGD("\nCamera initialization failed. Streaming parms probe set failed, len= %d.\n", len);
    } else {
        LOGD("1st: InitialContolTransfer Sucessful");
        LOGD("Camera initialization success, len= %d.\n", len);
    }
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (VS_PROBE_CONTROL << 8), uvc_camera->camStreamingInterfaceNum,
                                  buffer, sizeof (buffer), 500);
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
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_SET, SET_CUR, (VS_COMMIT_CONTROL << 8),
                                  uvc_camera->camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
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
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (short) (VS_COMMIT_CONTROL << 8),
                                  uvc_camera->camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
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
        //streamEndPointAdressOverNative = endpoint->bEndpointAddress;
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
        //camStreamingInterfaceNum = interface->bInterfaceNumber;

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
    /*
    ret = libusb_get_device_descriptor(dev, &desc);
    if (ret < 0) {
        LOGD(stderr, "failed to get device descriptor");
        return -1;
    }

    LOGD("\nFound the camera\n");
    fflush(stdout);
    ret = libusb_open(dev, &handle);
    LOGD("libusb_open returned = %d", ret);
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
    */
    return 0;
}

int wrap_camera_device(int FD, uvc_context_t **pctx, struct libusb_context *usb_ctx, uvc_device_handle_t **devh, uvc_device_t *dev) {
    uvc_error_t ret = UVC_SUCCESS;

    LOGD("wrap_camera_device()");

    uvc_context_t *ctx = calloc(1, sizeof(*ctx));
    uvc_device_handle_t *internal_devh;
    internal_devh = calloc(1, sizeof(*internal_devh));
    internal_devh->dev = dev;
    dev = calloc(1, sizeof(*dev));
    ret = libusb_set_option(ctx->usb_ctx, LIBUSB_OPTION_NO_DEVICE_DISCOVERY, NULL);
    if (ret != LIBUSB_SUCCESS) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,"libusb_set_option failed: %d\n", ret);
        return -1;
    }
    ret = libusb_init(&ctx->usb_ctx);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_init failed: %d\n", ret);
        LOGD("Trying to initialize with NULL CTX");
        ret = libusb_init(NULL);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, TAG,
                                "libusb_init failed: %d\n", ret);
            LOGD("Trying to initialize with NULL CTX");
            return -1;
        } else LOGD("libusb_init only sucessful without Context");
    } else LOGD("libusb_init with Context sucessful");


    ctx->own_usb_ctx = 1;
    ret = libusb_wrap_sys_device(ctx->usb_ctx, (intptr_t)FD, &internal_devh->usb_devh);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device failed: %d\n", ret);
        return -2;
    }
    else if (internal_devh->usb_devh == NULL) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device returned invalid handle\n");
        return -3;
    }
    cameraDevice_is_wraped = 1;
    //dev->ctx = ctx;
    LOGD("get the device");
    dev->usb_dev = libusb_get_device(internal_devh->usb_devh);
    dev->ref++;	// これ排他制御要るんちゃうかなぁ(｡･_･｡)
    libusb_ref_device(dev->usb_dev);

    *devh = internal_devh;

    if (ctx != NULL)
        *pctx = ctx;
    LOGD("wrap complete");
    return 0;
}

int set_the_native_Values (uvc_camera_t *uvc_camera, int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
           int camFrameInde, int camFrameInterva, int imageWidt, int imageHeigh, int camStreamingEndpointAdress, int camStreamingInterfaceNumber,
           const char* frameformat, int numberOfAutoFrame, int bcdUVC_int, int lowAndroid) {
    uvc_camera->fd = FD;
    uvc_camera->numberOfAutoFrames = numberOfAutoFrame;
    uvc_camera->packetsPerRequest = packetsPerReques;
    uvc_camera->maxPacketSize = maxPacketSiz;
    uvc_camera->activeUrbs = activeUrb;
    uvc_camera->camStreamingAltSetting = camStreamingAltSettin;
    uvc_camera->camFormatIndex = camFormatInde;
    uvc_camera->camFrameIndex = camFrameInde;
    uvc_camera->camFrameInterval = camFrameInterva;
    uvc_camera->camStreamingEndpoint = camStreamingEndpointAdress;
    uvc_camera->camStreamingInterfaceNum = camStreamingInterfaceNumber;
    uvc_camera->imageWidth = imageWidt;
    uvc_camera->imageHeight = imageHeigh;
    uvc_camera->frameFormat = frameformat;
    //uvc_camera->mUsbFs = mUsbFs;
    //uvc_camera->vendorID = vendorID;
    //uvc_camera->productID = productID;
    //uvc_camera->busnum = busnum;
    //uvc_camera->devaddr = devaddr;
    uvc_camera->bcdUVC = bcdUVC_int;
    if (lowAndroid == 1) uvc_camera->low_Android = true;
    uvc_camera->initialized = true;
    uvc_camera->valuesSet ++;
    return uvc_camera->valuesSet;
/*
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
    bcdUVC = bcdUVC_int;
    LOGD("bcdUVC = %d", bcdUVC);
    if (lowAndroid == 1) low_Android = true;
    initialized = true;
    result ++;
    return result;
    */
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

int initStreamingParms(uvc_camera_t *uvc_camera, int FD) {
    LOGD("initStreamingParms");
    uvc_error_t ret = -1;
    if (!libusb_Is_initialized) {
        ret = uvc_init2(&uvc_camera->camera_context, NULL);
        if (ret == UVC_SUCCESS) libusb_Is_initialized = 1;
    }
    if (!cameraDevice_is_wraped) {
        if (uvc_get_device_with_fd(uvc_camera->camera_context, &uvc_camera->camera_device,  &uvc_camera->camera_deviceHandle, FD) == 0) {
            LOGD("Successfully wraped The CameraDevice");
            cameraDevice_is_wraped = 1;
        } else return (int) NULL;
    }
    if (!camIsOpen) {
        ret = uvc_open(uvc_camera->camera_device, uvc_camera->camera_deviceHandle);
        if (ret == UVC_SUCCESS) {
            LOGD("uvc_open sucessful");
            camIsOpen = true;
        } else {
            LOGD("uvc_open failed. Return = %d", ret);
            return ret;
        }
    }
    LOGD("trying to claim the interfaces");
    for (int if_num = 0; if_num < (uvc_camera->camStreamingInterfaceNum + 1); if_num++) {
        if (libusb_kernel_driver_active(uvc_camera->camera_deviceHandle->usb_devh, if_num)) {
            libusb_detach_kernel_driver(uvc_camera->camera_deviceHandle->usb_devh, if_num);
        }
        int rc = libusb_claim_interface(uvc_camera->camera_deviceHandle->usb_devh, if_num);
        if (rc < 0) {
            fprintf(stderr, "Error claiming interface: %s\n",
                    libusb_error_name(rc));
        } else {
            printf("Interface %d erfolgreich eingehängt;\n", if_num);
        }
    }
    LOGD("Print Device");

    //struct libusb_device_descriptor desc;
    //print_device(libusb_get_device(uvc_camera->camera_deviceHandle->usb_devh) , 0, uvc_camera->camera_deviceHandle->usb_devh, desc);
    int r = libusb_set_interface_alt_setting(uvc_camera->camera_deviceHandle->usb_devh, uvc_camera->camStreamingInterfaceNum, 0); // camStreamingAltSetting = 7;    // 7 = 3x1024 bytes packet size
    if (r != LIBUSB_SUCCESS) {
        LOGD("libusb_set_interface_alt_setting(uvc_camera->camera_deviceHandle->usb_devh, Interface 1, alternate_setting 0) failed with error %d\n", r);
        return r;
    } else {
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = 0\n", r);
    }
    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "libusb_control_transfer start\n");
    initStreamingParms_controltransfer(uvc_camera, uvc_camera->camera_deviceHandle->usb_devh, false);
    camIsOpen = compareStreamingParmsValues();
    if (camIsOpen) return 0;
    else return -4;
}

// JNA Methods:
void setImageCapture() {    imageCapture = true;}
void setImageCaptureLongClick() {    imageCapturelongClick = true;}
void startVideoCapture() {   videoCapture = true;}
void stopVideoCapture() {    videoCapture = false;}
void startVideoCaptureLongClick() {    videoCaptureLongClick = true;}
void stopVideoCaptureLongClick() { videoCaptureLongClick = false;}

void probeCommitControl_cleanup()
{
    free(ctl_transfer_Data->ctl_transfer_values);
    LOGD("probeCommitControl_cleanup Complete");
}

void stopJavaVM() {
    (*javaVm)->DetachCurrentThread(javaVm);
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

// Init SurfaceView for Service
JNIEXPORT void JNICALL Java_humer_UvcCamera_StartIsoStreamActivityUvc_JniPrepairStreamOverSurfaceUVC
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

// Frame Conversation JNA METHOD:
unsigned char* convertUYVYtoJPEG (unsigned char* UYVY_frame_array, int* jpgLength, int UYVYframeLength, int width, int height) {
    uvc_frame_t *rgb;
    // We'll convert the image from YUV/JPEG to BGR, so allocate space
    rgb = uvc_allocate_frame(width * height * 4);
    if (!rgb) {
        printf("unable to allocate rgb frame!");
        return NULL;
    }
    uvc_error_t ret;
    ret = uvc_uyvy2rgbx_new(UYVY_frame_array, UYVYframeLength, width, height, rgb);
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
    /*
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
     */
    return _compressedImage;
}

// Frame Conversation JNA METHOD:
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
    /*
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
    */
    return _compressedImage;
}

// Frame Conversation JNI METHOD:
void
Java_humer_UvcCamera_StartIsoStreamActivityUvc_UYVYpixeltobmp( JNIEnv* env, jobject thiz, jbyteArray data, jobject bitmap, int im_width, int im_height){
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
    ret = uvc_uyvy2rgbx_new(UYVY_frame_array, UYVYframeLength, im_width, im_height, rgbx);
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

// Frame Conversation JNI METHOD:
void
Java_humer_UvcCamera_StartIsoStreamActivityUvc_YUY2pixeltobmp( JNIEnv* env, jobject thiz, jbyteArray data, jobject bitmap, int im_width, int im_height){
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

struct uvc_camera_t* get_uvc_camera_t (uvc_camera_t *uvc_camera) {

    LOGD("The FrameFormat is %s", uvc_camera->frameFormat);
    return uvc_camera;
}

struct uvc_device_info * listDeviceUvc(uvc_camera_t *uvc_camera, int fd) {
    uvc_error_t result;
    if (!libusb_Is_initialized) {
        result = uvc_init2(&uvc_camera->camera_context, NULL);
        if (result == UVC_SUCCESS) libusb_Is_initialized = 1;
    }
    if (!cameraDevice_is_wraped) {
                if (uvc_get_device_with_fd(uvc_camera->camera_context, &uvc_camera->camera_device,  &uvc_camera->camera_deviceHandle, fd) == 0) {
            LOGD("Successfully wraped The CameraDevice");
            cameraDevice_is_wraped = 1;
        } else return  NULL;
    }
    if (!camIsOpen) {
        result = uvc_open(uvc_camera->camera_device, uvc_camera->camera_deviceHandle);
        if (result == UVC_SUCCESS) camIsOpen = true;
    }
    if (camIsOpen) return  uvc_camera->camera_deviceHandle->info;
    else return NULL;
}

struct uvc_stream_ctrl probeSetCur_TransferUVC(uvc_camera_t *uvc_camera) {
    uvc_error_t err;
    // Check if interface is already claimed.
    err = uvc_claim_if(uvc_camera->camera_deviceHandle, uvc_camera->camera_deviceHandle->info->stream_ifs->bInterfaceNumber);
    if (UNLIKELY(err)) {
        LOGE("uvc_claim_if:err=%d", err);
        //return err;
    } else	LOGDEB("uvc_claim_if = UVC_SUCCESS");

    uvc_error_t result;
    enum uvc_frame_format format;
    LOGD("Trying to set the format");
    if (uvc_camera->frameFormat == NULL) {
        LOGD("FrameFormat == NULL");
    }
    LOGD("frameFormat = %s", uvc_camera->frameFormat);
    if (strcmp(uvc_camera->frameFormat, "MJPEG") == 0) {
        format = UVC_FRAME_FORMAT_MJPEG;
    } else if (strcmp(uvc_camera->frameFormat, "YUY2") == 0 ||   strcmp(uvc_camera->frameFormat, "UYVY") == 0  ) {
        format = UVC_FRAME_FORMAT_YUYV;
    } else if (strcmp(uvc_camera->frameFormat, "NV21") == 0) {
        format = UVC_FRAME_FORMAT_NV21;
    }else if (strcmp(uvc_camera->frameFormat, "UYVY") == 0) {
        format = UVC_FRAME_FORMAT_UYVY;
    } else {
        format = UVC_FRAME_FORMAT_YUYV;
    }
    LOGD("format = %d", format);
    uvc_stream_ctrl_t ctrl;
    ctrl.bInterfaceNumber = uvc_camera->camera_deviceHandle->info->stream_ifs->bInterfaceNumber;
    ctrl.bmHint = 1;
    ctrl.bFormatIndex = uvc_camera->camFormatIndex;
    ctrl.bFrameIndex = uvc_camera->camFrameIndex;
    ctrl.dwFrameInterval = uvc_camera->camFrameInterval;
    enum uvc_req_code request;
    request = UVC_SET_CUR;
    LOGD("PROBE SET CUR QUERY");
    result = uvc_query_stream_ctrl(uvc_camera->camera_deviceHandle, &ctrl, 0, request);
    LOGD("result = %d", result);
    if (result == UVC_SUCCESS) return ctrl;
    else {
        ctrl.bInterfaceNumber = result ;
        return ctrl;
    }
}

struct uvc_stream_ctrl probeGetCur_TransferUVC(uvc_camera_t *uvc_camera, struct uvc_stream_ctrl ctrl) {
    uvc_error_t result;
    enum uvc_req_code request;
    request = UVC_GET_CUR;
    LOGD("PROBE GET CUR QUERY");
    result = uvc_query_stream_ctrl(uvc_camera->camera_deviceHandle, &ctrl, 0, request);
    if (result == UVC_SUCCESS) LOGD("uvc_query_stream_ctrl == UVC_SUCCESS");
    else LOGD("uvc_query_stream_ctrl FAILED // result = %d", result);
    return ctrl;
}

struct uvc_stream_ctrl CommitSetCur_TransferUVC(uvc_camera_t *uvc_camera, struct uvc_stream_ctrl ctrl) {
    uvc_error_t result;
    enum uvc_req_code request;
    request = UVC_SET_CUR;
    LOGD("COMMIT SET CUR QUERY");
    result = uvc_query_stream_ctrl(uvc_camera->camera_deviceHandle, &ctrl, 1, request);
    if (result == UVC_SUCCESS) LOGD("uvc_query_stream_ctrl == UVC_SUCCESS");
    else LOGD("uvc_query_stream_ctrl FAILED // result = %d", result);
    return ctrl;
}

struct uvc_stream_ctrl CommitGetCur_TransferUVC(uvc_camera_t *uvc_camera, struct uvc_stream_ctrl ctrl){
    uvc_error_t result;
    enum uvc_req_code request;
    request = UVC_GET_CUR;
    LOGD("COMMIT GET CUR QUERY");
    result = uvc_query_stream_ctrl(uvc_camera->camera_deviceHandle, &ctrl, 1, request);
    if (result == UVC_SUCCESS) LOGD("uvc_query_stream_ctrl == UVC_SUCCESS");
    else LOGD("uvc_query_stream_ctrl FAILED // result = %d", result);
    return ctrl;
}

void cb_one_frame_UVC(uvc_frame_t *frame, void *ptr) {
    LOGD("Frame");
    uvc_camera_t *uvc_camera = (uvc_camera_t *)ptr;
    if (frame->frame_format == UVC_FRAME_FORMAT_YUYV) {
        LOGD("frame_format == UVC_FRAME_FORMAT_YUYV");
        LOGD("frame->width = %d, frame->height = %d", frame->width, frame->height);
        LOGD("frame->actual_bytes = %d", frame->actual_bytes);
        if (sendReceivedDataToJava != NULL) {
            LOGD("sending data to java");
            sendReceivedDataToJava(frame->data, (int) frame->actual_bytes) ;
        }
    } else if (frame->frame_format == UVC_FRAME_FORMAT_MJPEG) {
        LOGD("frame_format == UVC_FRAME_FORMAT_MJPEG");
        LOGD("frame->width = %d, frame->height = %d", frame->width, frame->height);
        LOGD("frame->actual_bytes = %d", frame->actual_bytes);
        if (sendReceivedDataToJava != NULL) sendReceivedDataToJava(frame->data, frame->actual_bytes) ;
    }
    uvc_camera->runningStream = false;


    if (!uvc_camera->runningStream) {
        LOGD("trying to stop the stream");
        if (!uvc_camera->runningStream) uvc_stop_streaming(uvc_camera->camera_deviceHandle);
        LOGD("uvc_stop_streaming sucessful");

    }
}

void getOneFrameUVC(uvc_camera_t *uvc_camera, struct uvc_stream_ctrl ctrl) {
    uvc_error_t result;
    uvc_stream_handle_t *strmh;
    int overload_buffer_value = uvc_camera->packetsPerRequest * uvc_camera->activeUrbs * uvc_camera->maxPacketSize;
    size_t frame_size;
    if (strcmp(uvc_camera->frameFormat, "MJPEG") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "YUY2") == 0 ||   strcmp(uvc_camera->frameFormat, "UYVY") == 0  ) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight  * 2 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "NV21") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 / 2 + overload_buffer_value;
    } else {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 2 + overload_buffer_value;
    }
    result = uvc_stream_open_ctrl(uvc_camera->camera_deviceHandle, &strmh, &ctrl, frame_size);

    if (UNLIKELY(result != UVC_SUCCESS)) {
        LOGD("uvc_stream_open_ctrl failed: %d", result);
        uvc_camera->runningStream = false;

    } else {
        uvc_camera->runningStream = true;
        LOGD("uvc_stream_open_ctrl returned UVC_SUCCESS");

        uvc_error_t err = uvc_stream_start_random(strmh, cb_one_frame_UVC, (void *) uvc_camera, 0, 0, uvc_camera->activeUrbs,
                                                  uvc_camera->packetsPerRequest, uvc_camera->camStreamingAltSetting, uvc_camera->maxPacketSize );
        if (err == 0) LOGD("uvc_stream_start_random sucessful\n     return = %d", err);
        else {
            LOGD("uvc_stream_start_random failed !!!!\n   return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }
        LOGD("getOneFrameUVC complete");
    }
}


void cb_frames5sec_UVC(uvc_frame_t *frame, void *ptr) {

    uvc_camera_t *uvc_camera = (uvc_camera_t *)ptr;

    if (frame->frame_format == UVC_FRAME_FORMAT_YUYV) {
        LOGD("frame_format == UVC_FRAME_FORMAT_YUYV");
        LOGD("frame->width = %d, frame->height = %d", frame->width, frame->height);
        LOGD("frame->actual_bytes = %d", frame->actual_bytes);
        if (sendReceivedDataToJava != NULL) sendReceivedDataToJava(frame->data, frame->actual_bytes) ;
    } else if (frame->frame_format == UVC_FRAME_FORMAT_MJPEG) {
        LOGD("frame_format == UVC_FRAME_FORMAT_MJPEG");
        LOGD("frame->width = %d, frame->height = %d", frame->width, frame->height);
        LOGD("frame->actual_bytes = %d", frame->actual_bytes);
        if (sendReceivedDataToJava != NULL) sendReceivedDataToJava(frame->data, frame->actual_bytes) ;
    }
    if (!uvc_camera->runningStream) {
        uvc_device_handle_t *deviceHandle = (uvc_stream_handle_t *)ptr;
        if (!uvc_camera->runningStream) uvc_stop_streaming(uvc_camera->camera_deviceHandle);
    }
}

void getFramesOverLibUsb5sec(uvc_camera_t *uvc_camera, struct uvc_stream_ctrl ctrl) {
    uvc_error_t result;
    uvc_stream_handle_t *strmh;
    int overload_buffer_value = uvc_camera->packetsPerRequest * uvc_camera->activeUrbs * uvc_camera->maxPacketSize;
    size_t frame_size;
    if (strcmp(uvc_camera->frameFormat, "MJPEG") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "YUY2") == 0 ||   strcmp(uvc_camera->frameFormat, "UYVY") == 0  ) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight  * 2 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "NV21") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 / 2 + overload_buffer_value;
    } else {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 2 + overload_buffer_value;
    }
    result = uvc_stream_open_ctrl(uvc_camera->camera_deviceHandle, &strmh, &ctrl, frame_size);
    if (UNLIKELY(result != UVC_SUCCESS)) {
        LOGD("uvc_stream_open_ctrl failed: %d", result);
        uvc_camera->runningStream = false;
    } else {
        uvc_camera->runningStream = true;
        LOGD("uvc_stream_start_random");

        uvc_error_t err = uvc_stream_start_random(strmh, cb_frames5sec_UVC, (void *) uvc_camera, 0, 0, uvc_camera->activeUrbs,
                                                  uvc_camera->packetsPerRequest, uvc_camera->camStreamingAltSetting, uvc_camera->maxPacketSize );
        if (err == 0) LOGD("uvc_stream_start_random sucessful");
        else {
            LOGD("return = %d", err);
            uvc_perror(result, "failed start_streaming");
        }
        LOGD("getFramesOverLibUsb5sec complete");
    }
}

void cb_automatic_detection(uvc_frame_t *frame, void *ptr) {

    uvc_camera_t *uvc_camera = (uvc_camera_t *)ptr;

    if (!uvc_camera->auto_values_collected) {
        uvc_camera->auto_values_collected = true;
        auto_detect_struct_t *stream_values = (auto_detect_struct_t *) ptr;
        LOGD("stream_values->activeUrbs: %d", stream_values->activeUrbs);
        LOGD("stream_values->altsetting: %d", stream_values->altsetting);
        LOGD("stream_values->packetsPerRequest: %d", stream_values->packetsPerRequest);
        LOGD("stream_values->maxPacketSize: %d", stream_values->maxPacketSize);
        if (automatic_callback != NULL) automatic_callback(stream_values) ;
        free (stream_values);
        LOGD("Frame received and freed");
    }

    if (!uvc_camera->runningStream) {
        uvc_stop_streaming(uvc_camera->camera_deviceHandle);
        LOGD("uvc_stop_streaming called");
    }
}

void automaticDetection(uvc_camera_t *uvc_camera) {
    uvc_error_t ret;
    uvc_camera->auto_values_collected = false;
    int requestMode = 0;
    if (strcmp(uvc_camera->frameFormat, "MJPEG") == 0) requestMode = 1;
    else if (strcmp(uvc_camera->frameFormat, "YUY2") == 0) requestMode = 0;
    else if (strcmp(uvc_camera->frameFormat, "UYVY") == 0) requestMode = 0;
    else if (strcmp(uvc_camera->frameFormat, "NV21") == 0) requestMode = 0;
    else requestMode = 0;
    uvc_stream_ctrl_t ctrl;
    LOGD("uvc_get_stream_ctrl_format_size:");
    ret = uvc_get_stream_ctrl_format_size( uvc_camera->camera_deviceHandle, &ctrl, !requestMode ? UVC_FRAME_FORMAT_YUYV : UVC_FRAME_FORMAT_MJPEG, uvc_camera->imageWidth, uvc_camera->imageHeight, (10000000/uvc_camera->camFrameInterval));
    LOGD("uvc_get_stream_ctrl_format_size returned: %d", ret);
    LOGD("ctrl.dwFrameInterval: %d", ctrl.dwFrameInterval);
    uvc_stream_handle_t *strmh;
    LOGD("uvc_stream_open_ctrl:");
    int overload_buffer_value = uvc_camera->packetsPerRequest * uvc_camera->activeUrbs * uvc_camera->maxPacketSize;
    size_t frame_size;
    if (strcmp(uvc_camera->frameFormat, "MJPEG") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "YUY2") == 0 ||   strcmp(uvc_camera->frameFormat, "UYVY") == 0  ) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight  * 2 + overload_buffer_value;
    } else if (strcmp(uvc_camera->frameFormat, "NV21") == 0) {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 3 / 2 + overload_buffer_value;
    } else {
        frame_size = uvc_camera->imageWidth * uvc_camera->imageHeight * 2 + overload_buffer_value;
    }
    ret = uvc_stream_open_ctrl(uvc_camera->camera_deviceHandle, &strmh, &ctrl, frame_size);
    if (UNLIKELY(ret != UVC_SUCCESS))
        return ;
    auto_detect_struct_t *custom_Values = malloc(sizeof(auto_detect_struct_t));
    LOGD("uvc_stream_start_automatic_detection:");
    uvc_camera->runningStream = true;
    ret = uvc_stream_start_automatic_detection(strmh, cb_automatic_detection, custom_Values, 0, 0);
    if (UNLIKELY(ret != UVC_SUCCESS)) {
        uvc_stream_close(strmh);
        uvc_camera->runningStream = false;
        return ;
    }
}


uvc_camera_t* write_a_value(uvc_camera_t* cam_pointer) {

    uvc_camera_t *cam = malloc(sizeof(uvc_camera_t));

    cam->preview_pointer = 55;


    LOGD("From C reate:\ncam->preview_pointer = %d", cam->preview_pointer);
    cam_pointer = cam;

    return cam;
}
