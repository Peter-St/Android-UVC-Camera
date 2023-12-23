cmake_minimum_required(VERSION 2.8)
project(libuvc)

if (NOT CMAKE_BUILD_TYPE)
  message(STATUS "No build type selected, default to Release")
  set(CMAKE_BUILD_TYPE "Release" CACHE STRING "" FORCE)
endif ()


if (NOT CMAKE_BUILD_TARGET)
  message(STATUS "No target type selected, default to shared library")
  set(CMAKE_BUILD_TARGET "Shared" CACHE STRING "" FORCE)
endif()

set(libuvc_VERSION_MAJOR 0)
set(libuvc_VERSION_MINOR 0)
set(libuvc_VERSION_PATCH 4)
set(libuvc_VERSION ${libuvc_VERSION_MAJOR}.${libuvc_VERSION_MINOR}.${libuvc_VERSION_PATCH})


#find_path(LIBUSB_INCLUDE_DIR libusb1.0/libusb.h
#	PATHS /opt/local/include)

# Try to find JPEG using a module or pkg-config. If that doesn't work, search for the header.
#find_package(jpeg QUIET)
#if(NOT JPEG_FOUND)
#  find_path(JPEG_INCLUDE_DIR jpeglib.h)
#  if(JPEG_INCLUDE_DIR)
#    set(JPEG_FOUND ON)
#    set(JPEG_LIBRARIES -ljpeg)
#  endif()
#endif()

SET(CMAKE_C_FLAGS_DEBUG "-g -DUVC_DEBUGGING")

SET(INSTALL_CMAKE_DIR "${CMAKE_INSTALL_PREFIX}/lib/cmake/libuvc" CACHE PATH
	"Installation directory for CMake files")

SET(SOURCES
        src/ctrl.c
        src/device.c
        src/diag.c
        src/frame.c
        src/init.c
        src/stream.c
        src/misc.c
        src/frame-mjpeg.c)

set (LIBUSB_INCLUDE_DIR ../libusb/libusb.h)

include_directories(
  ${libuvc_SOURCE_DIR}/include
  ${libuvc_BINARY_DIR}/include
  ${LIBUSB_INCLUDE_DIR}
)


if(${CMAKE_BUILD_TARGET} MATCHES "Shared")
  add_library(uvc SHARED ${SOURCES})
  # add_library(nativeegl SHARED ${SRCS} ${HDRS})
  # add_library(nativeegl SHARED ${SRCS} ${HDRS})
elseif(${CMAKE_BUILD_TARGET} MATCHES "Static")
  add_library(uvc STATIC ${SOURCES})
else()
  message( FATAL_ERROR "Invalid build type ${CMAKE_BUILD_TARGET}" )
endif()

configure_file(include/libuvc/libuvc_config.h.in
  ${PROJECT_BINARY_DIR}/include/libuvc/libuvc_config.h @ONLY)

set_target_properties(uvc PROPERTIES
  PUBLIC_HEADER "include/libuvc/libuvc.h;${libuvc_BINARY_DIR}/include/libuvc/libuvc_config.h" )

if(JPEG_FOUND)
  target_link_libraries (uvc ${JPEG_LIBRARIES})
endif(JPEG_FOUND)



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
        uvc
        # Android Bitmap
        -ljnigraphics
        -landroid
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
)



#android_ndk_gdb_enable()


# target_link_libraries(nativeegl log android)

#android_ndk_gdb_debuggable(nativeegl)



#include_directories (/include)

# Set the path to the libusb directory.
set (LIBUSB_DIR ../libusb)

#set (LIBUSB_LIBRARY_NAMES ../libusb)
#find_library(LIBUSB_LIBRARY_NAMES usb1.0
#	.PATHS /opt/local/lib)

set (LIBUSB_DIR ../libusb)
add_subdirectory (${LIBUSB_DIR}/cmake ./usb1.0)
include_directories (${LIBUSB_DIR}/libusb)
include_directories (${LIBUSB_DIR}/libusb/os)
target_include_directories(uvc PUBLIC "../libusb/libusb/")
target_include_directories(uvc PUBLIC "${CMAKE_CURRENT_SOURCE_DIR}/os")
target_link_libraries(uvc usb1.0)



set (JPG9_DIR ../libjpeg-version-9/libjpeg9)
add_subdirectory (${JPG9_DIR} ./jpeg9)
# (uvc PUBLIC ${JPG_DIR}/simd)
target_link_libraries(uvc jpeg9)


#set (JPG_DIR ../libjpeg)
#add_subdirectory (${JPG_DIR} ./jpeg)
#include_directories (uvc PUBLIC ${JPG_DIR})
#target_link_libraries(uvc jpeg)

#set(JPEG_INCLUDE_DIR ${JPG_DIR})
#  if(JPEG_INCLUDE_DIR)
#    set(JPEG_FOUND ON)
#    set(JPEG_LIBRARIES -ljpeg)
#  endif()

#message(STATUS "Building libuvc with JPEG support.")
#include_directories(${JPEG_INCLUDE_DIR})
#SET(HAVE_JPEG TRUE)
#SET(SOURCES ${SOURCES} src/frame-mjpeg.c)
#target_link_libraries (uvc ${JPEG_LIBRARIES})

#set (TURBOJPG_DIR ../libjpeg-turbo)
#add_subdirectory (${TURBOJPG_DIR} ./jpegturbo)
#include_directories (${TURBOJPG_DIR})
#target_link_libraries(uvc turbojpeg)
#set(JPEG_INCLUDE_DIR ${TURBOJPG_DIR})
#  if(JPEG_INCLUDE_DIR)
#    set(JPEG_FOUND ON)
#    set(JPEG_LIBRARIES -ljpeg)
#  endif()




#target_link_libraries(uvc ${LIBUSB_LIBRARY_NAMES})

#add_executable(test src/test.c)
#target_link_libraries(test uvc ${LIBUSB_LIBRARY_NAMES} opencv_highgui
#  opencv_core)

#install( TARGETS uvc
#        EXPORT Uvc_Support
#        LIBRARY DESTINATION "${CMAKE_INSTALL_PREFIX}/lib"
#        ARCHIVE DESTINATION "${CMAKE_INSTALL_PREFIX}/lib"
#        PUBLIC_HEADER DESTINATION "${CMAKE_INSTALL_PREFIX}/include/libuvc"
#        )


#export(TARGETS uvc
#  FILE "${PROJECT_BINARY_DIR}/libuvcTargets.cmake")
#export(PACKAGE libuvc)

set(CONF_INCLUDE_DIR "${CMAKE_INSTALL_PREFIX}/include")
set(CONF_LIBRARY "${CMAKE_INSTALL_PREFIX}/lib/libuvc.so")

configure_file(libuvcConfig.cmake.in ${PROJECT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/libuvcConfig.cmake)

configure_file(libuvcConfigVersion.cmake.in ${PROJECT_BINARY_DIR}/libuvcConfigVersion.cmake @ONLY)


install(TARGETS uvc
        LIBRARY DESTINATION "${CMAKE_INSTALL_PREFIX}/lib"
        ARCHIVE DESTINATION "${CMAKE_INSTALL_PREFIX}/lib"
        PUBLIC_HEADER DESTINATION "${CMAKE_INSTALL_PREFIX}/include/libuvc"
        )


install(FILES
  "${PROJECT_BINARY_DIR}${CMAKE_FILES_DIRECTORY}/libuvcConfig.cmake"
  "${PROJECT_BINARY_DIR}/libuvcConfigVersion.cmake"
  DESTINATION "${INSTALL_CMAKE_DIR}")


