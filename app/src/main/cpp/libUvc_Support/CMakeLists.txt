# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("myapplication")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

file(GLOB_RECURSE SOURCE_C ${YOUR_PROJECT_SRC_DIR}/ *.cpp)
#file(GLOB_RECURSE SOURCE_H ${YOUR_PROJECT_HEADER_DIR}/libusb/libusb/libusb.h)

add_library( # Sets the name of the library.
        Uvc_Support

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        libuvc_support.c
        uvc_support.cpp
        #            ${SOURCE_H}
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
        # Specifies the target library.
        Uvc_Support
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
)



#set (LIBUVC_DIR ../libuvc)
#add_subdirectory (${LIBUVC_DIR} ./uvc)
#include_directories (${LIBUVC_DIR}/include/libuvc)
#target_link_libraries(Uvc_Support uvc)



set (LIBYUV_DIR ../libyuv)
add_subdirectory (${LIBYUV_DIR} ./yuv)
include_directories (${LIBYUV_DIR}/include)
include_directories (${LIBYUV_DIR}/include/libyuv)
target_link_libraries(Uvc_Support yuv)


set (LIBPREVIEW_DIR UVC_Camera_Saki)
add_subdirectory (${LIBPREVIEW_DIR} ./preview)
target_link_libraries(Uvc_Support uvc_preview)


#set (LIBJPEG_DIR ../jpeg)
#add_subdirectory (${LIBJPEG_DIR} ./jpeg)
#include_directories (${LIBJPEG_DIR}/include)
#target_link_libraries(Uvc_Support jpeg)

