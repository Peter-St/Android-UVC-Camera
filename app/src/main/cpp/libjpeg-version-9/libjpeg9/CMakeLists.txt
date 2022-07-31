project(jpeg9)
cmake_minimum_required(VERSION 3.10)

set(PACKAGE "libjpeg9")

include(CheckIncludeFiles)
include(CheckFunctionExists)
include(CheckSymbolExists)
include(CheckStructHasMember)
include(CheckCCompilerFlag)
include(CTest)

check_function_exists(clock_gettime             HAVE_CLOCK_GETTIME)
check_function_exists(pthread_condattr_setclock HAVE_PTHREAD_CONDATTR_SETCLOCK)
check_function_exists(pthread_setname_np        HAVE_PTHREAD_SETNAME_NP)
check_function_exists(pthread_threadid_np       HAVE_PTHREAD_THREADID_NP)
check_function_exists(eventfd                   HAVE_EVENTFD)
check_function_exists(pipe2                     HAVE_PIPE2)
check_function_exists(syslog                    HAVE_SYSLOG)

check_include_files(asm/types.h      HAVE_ASM_TYPES_H) 
check_include_files(sys/eventfd.h    HAVE_EVENTFD) 
check_include_files(dlfcn.h          HAVE_DLFCN_H)
check_include_files(inttypes.h       HAVE_INTTYPES_H)
check_include_files(stdint.h         HAVE_STDINT_H)
check_include_files(stdlib.h         HAVE_STDLIB_H)
check_include_files(string.h         HAVE_STRING_H)
check_include_files(strings.h        HAVE_STRINGS_H)
check_include_files(sys/stat.h       HAVE_SYS_STAT_H)
check_include_files(sys/time.h       HAVE_SYS_TIME_H)
check_include_files(sys/types.h      HAVE_SYS_TYPES_H)
check_include_files(unistd.h         HAVE_UNISTD_H)
check_include_files("stdlib.h;stdarg.h;string.h;float.h" STDC_HEADERS)

check_symbol_exists(EFD_CLOEXEC     "sys/eventfd.h" HAVE_DECL_EFD_CLOEXEC)
check_symbol_exists(EFD_NONBLOCK    "sys/eventfd.h" HAVE_DECL_EFD_NONBLOCK)
check_symbol_exists(TFD_CLOEXEC     "sys/eventfd.h" HAVE_DECL_TFD_CLOEXEC)
check_symbol_exists(timerfd_create  "sys/timerfd.h" HAVE_TIMERFD) 

check_struct_has_member("struct timespec" tv_sec time.h HAVE_STRUCT_TIMESPEC)

#option(libusb_BUILD_TESTING "Build Tests" ON)

if (WIN32)
    set(PLATFORM_WINDOWS 1 CACHE INTERNAL "")
endif()
if (UNIX)
    set(PLATFORM_POSIX 1  CACHE INTERNAL "")
    check_c_compiler_flag("-fvisibility=hidden" HAVE_VISIBILITY)
endif()

if (HAVE_VISIBILITY)
    set(DEFAULT_VISIBILITY "__attribute__((visibility(\"default\")))"  CACHE INTERNAL "")
else()
    set(DEFAULT_VISIBILITY ""  CACHE INTERNAL "")
endif()

#configure_file(config.h.in config.h @ONLY)

add_library(jpeg9 SHARED
        jcapimin.c
        jcapistd.c
        jccoefct.c
        jccolor.c
        jcdctmgr.c
        jchuff.c
        jcinit.c
        jcmainct.c
        jcmarker.c
        jcmaster.c
        jcomapi.c
        jcparam.c
        jcprepct.c
        jcsample.c
        jctrans.c
        jdapimin.c
        jdapistd.c
        jdatadst.c
        jdatasrc.c
        jdcoefct.c
        jdcolor.c
        jddctmgr.c
        jdhuff.c
        jdinput.c
        jdmainct.c
        jdmarker.c
        jdmaster.c
        jdmerge.c
        jdpostct.c
        jdsample.c
        jdtrans.c
        jerror.c
        jfdctflt.c
        jfdctfst.c
        jfdctint.c
        jidctflt.c
        jidctfst.c
        jidctint.c
        jquant1.c
        jquant2.c
        jutils.c
        jmemmgr.c
        jcarith.c
        jdarith.c
        jaricom.c
        # Use the no backing store memory manager provided by
        # libjpeg. See install.txt
        jmemnobs.c
        )




install(
        TARGETS jpeg9
        ARCHIVE DESTINATION lib
        LIBRARY DESTINATION lib
        COMPONENT library
        RUNTIME DESTINATION "bin"
        INCLUDES DESTINATION "include"
)

install(FILES jconfig.h jerror.h jmorecfg.h jpeglib.h DESTINATION "include")
