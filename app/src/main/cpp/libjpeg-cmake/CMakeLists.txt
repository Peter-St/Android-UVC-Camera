cmake_minimum_required(VERSION 3.2)

project(libjpeg)

include(CheckIncludeFile)
include(CheckSymbolExists)
include(CheckCSourceCompiles)

check_include_file(stddef.h HAVE_STDDEF_H)
check_include_file(stdlib.h HAVE_STDLIB_H)
check_include_file(string.h HAVE_STRING_H)
check_symbol_exists(size_t stddef.h stdlib.h stdio.h HAVE_ANSI_SIZE_T)
check_symbol_exists(setmode io.h USE_SETMODE)
check_c_source_compiles(
"
void f()
{
    char x[ (char)0xff ]; /* x[-1] if signed */
}
"
CHAR_IS_UNSIGNED)

check_c_source_compiles(
"
void f()
{
    char x[ ((signed char)0xff) >> 1 ]; /* x[-1] if signed */
}
"
RIGHT_SHIFT_IS_UNSIGNED)

if(NOT HAVE_STRING_H)
    set(NEED_BSD_STRINGS TRUE)
else()
    set(NEED_BSD_STRINGS FALSE)
endif()

if(NOT HAVE_ANSI_SIZE_T)
    set(NEED_SYS_TYPES_H TRUE)
else()
    set(NEED_SYS_TYPES_H FALSE)
endif()


configure_file(${CMAKE_CURRENT_SOURCE_DIR}/jconfig.h.in ${CMAKE_CURRENT_SOURCE_DIR}/jconfig.h)

add_library(libjpeg SHARED
    jaricom.c
    jcapimin.c
    jcapistd.c
    jcarith.c
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
    jdarith.c
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
    jmemnobs.c
)

add_executable(cjpeg cjpeg.c rdppm.c rdgif.c rdtarga.c rdrle.c rdbmp.c rdswitch.c cdjpeg.c)
target_link_libraries(cjpeg libjpeg)

add_executable(djpeg djpeg.c wrppm.c wrgif.c wrtarga.c wrrle.c wrbmp.c rdcolmap.c cdjpeg.c)
target_link_libraries(djpeg libjpeg)

add_executable(jpegtran jpegtran.c rdswitch.c cdjpeg.c transupp.c)
target_link_libraries(jpegtran libjpeg)

add_executable(rdjpgcom rdjpgcom.c)

add_executable(wrjpgcom wrjpgcom.c)

install(
    TARGETS libjpeg cjpeg djpeg jpegtran rdjpgcom wrjpgcom
    EXPORT "libjpegTargets"
    LIBRARY DESTINATION "lib"
    ARCHIVE DESTINATION "lib"
    RUNTIME DESTINATION "bin"
    INCLUDES DESTINATION "include"
)
install(
    FILES
        jconfig.h jerror.h jmorecfg.h jpeglib.h
    DESTINATION "include"
)

install(
    EXPORT "libjpegTargets"
    DESTINATION "lib/cmake/libjpeg"
)
