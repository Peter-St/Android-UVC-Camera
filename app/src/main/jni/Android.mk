#include $(call all-subdir-makefiles)
PROJ_PATH	:= $(call my-dir)
include $(CLEAR_VARS)

include $(PROJ_PATH)/libusb/android/jni/Android.mk
include $(PROJ_PATH)/libUsb_Support/Android.mk
include $(PROJ_PATH)/android-libjpeg-turbo/Android.mk
include $(PROJ_PATH)/jpeg8d/Android.mk
include $(PROJ_PATH)/libyuv/Android.mk

LOCAL_LDLIBS := -llog
