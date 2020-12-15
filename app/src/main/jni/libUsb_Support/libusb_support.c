#include <stdio.h>
#include <stdio.h>

#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
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


#define  LOG_TAG    "From LibUsb"

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
static const char* frameFormat;
volatile int total = 0;
volatile int totalFrame = 0;
volatile bool initialized = false;
volatile  bool runningStream = false;
static uint8_t frameUeberspringen = 0;
static uint8_t numberOfAutoFrames;

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
        LOGD("Camera initialization success, len= %d.\n", len);
    }
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (VS_PROBE_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 500);
    if (len != sizeof (buffer)) {
        LOGD("Camera initialization failed. Streaming parms probe set failed, len= %d.\n", len);
    }
    if (createPointer == true) {
        memcpy(ctl_transfer_Data->ctl_transfer_values + 47, buffer, length);
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
        LOGD("Camera initialization failed. Streaming parms commit set failed, len= %d.", len);
    }
    if (createPointer == true) {
        memcpy(ctl_transfer_Data->ctl_transfer_values + 95, buffer, length);
    }
    getStreamingParmsArray(finalStreamingParmsIntArray_first , buffer);
    len = libusb_control_transfer(handle, RT_CLASS_INTERFACE_GET, GET_CUR, (short) (VS_COMMIT_CONTROL << 8), camStreamingInterfaceNum, buffer, sizeof (buffer), 2000);
    if (len != sizeof (buffer)) {
        LOGD("Camera initialization failed. Streaming parms commit get failed, len= %d.", len);
    }
    if (createPointer == true) {
        memcpy(ctl_transfer_Data->ctl_transfer_values + 143, buffer, length);
    }
    getStreamingParmsArray(finalStreamingParmsIntArray , buffer);
}

void print_endpoint(const struct libusb_endpoint_descriptor *endpoint, int bInterfaceNumber) {
    int i, ret;
    if (bInterfaceNumber == camStreamingInterfaceNum) {
        int endpunktadresse = endpoint->bEndpointAddress;
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

        //Erstellen der Bus - und Geräteadressen

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


int libUsb_open_def_fd(int vid, int pid, const char *serial, int FD, int busnum, int devaddr) {
    int ret;
    unsigned char data[64];
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
        return 1;
    }
    ret = libusb_wrap_sys_device(NULL, (intptr_t)FD, &devh);
    if (ret < 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device failed: %d\n", ret);
        return 2;
    }
    else if (devh == NULL) {
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_wrap_sys_device returned invalid handle\n");
        return 3;
    }
    __android_log_print(ANDROID_LOG_INFO, TAG,
                        "libusb_control_transfer start\n");
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
        LOGD("Die Alternativeinstellungen wurden erfolgreich gesetzt: %d ; Altsetting = 0\n", r);
    }
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



int init (int FD, int packetsPerReques, int maxPacketSiz, int activeUrb, int camStreamingAltSettin, int camFormatInde,
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

        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_control_transfer start\n");
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
    initStreamingParms_controltransfer(devh, false);
    camIsOpen = compareStreamingParmsValues();
    if (camIsOpen) return 0;
    else return -4;
}

unsigned char * probeCommitControl(int bmHin, int camFormatInde, int camFrameInde, int camFrameInterva) {
    bmHint = bmHin;
    camFormatIndex = camFormatInde;
    camFrameIndex = camFrameInde;
    camFrameInterval = camFrameInterva;
    if (!camIsOpen) {
        int ret;
        enum libusb_error rc;
        rc = libusb_set_option(&ctx, LIBUSB_OPTION_WEAK_AUTHORITY, NULL);
        if (rc != LIBUSB_SUCCESS) {
            __android_log_print(ANDROID_LOG_ERROR, TAG,"libusb_init failed: %d\n", ret);
            return NULL;
        }
        ret = libusb_init(&ctx);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, TAG,
                                "libusb_init failed: %d\n", ret);
            return NULL;
        }
        ret = libusb_wrap_sys_device(NULL, (intptr_t)fd, &devh);
        if (ret < 0) {
            __android_log_print(ANDROID_LOG_INFO, TAG,
                                "libusb_wrap_sys_device failed: %d\n", ret);
            return NULL;
        }
        else if (devh == NULL) {
            __android_log_print(ANDROID_LOG_INFO, TAG,
                                "libusb_wrap_sys_device returned invalid handle\n");
            return NULL;
        }
        __android_log_print(ANDROID_LOG_INFO, TAG,
                            "libusb_control_transfer start\n");
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
    initStreamingParms_controltransfer(devh, true);
    camIsOpen = compareStreamingParmsValues();
    if (camIsOpen) return ctl_transfer_Data;
    else return ctl_transfer_Data;
}

void isoc_transfer_completion_handler_automaticdetection(struct libusb_transfer *the_transfer) {
    LOGD("Iso Transfer Callback Function");
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

        if(!strcmp(frameFormat, "mjpeg"))
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
        if(!strcmp(frameFormat, "mjpeg"))
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
        if(!strcmp(frameFormat, "mjpeg"))
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
