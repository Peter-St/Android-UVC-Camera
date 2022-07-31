LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := jpegtest
LOCAL_STATIC_LIBRARIES :=  libjpeg9


    
LOCAL_DEFAULT_CPP_EXTENSION := cpp

LOCAL_C_INCLUDES :=  \
	$(LOCAL_PATH)/../libjpeg9 \
	
#LOCAL_CFLAGS := -DANDROID_NDK  -Wno-psabi 
#LOCAL_CFLAGS += -DGL_GLEXT_PROTOTYPES=1 

#LOCAL_MODULE    := AndroidNDK
LOCAL_SRC_FILES := test.cpp \

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -ldl -llog -lz -landroid -lEGL 

include $(BUILD_SHARED_LIBRARY)