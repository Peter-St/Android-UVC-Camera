//#ifndef DEF_H_
//#define DEF_H_
//#define SUNNY_PHYSICS
#include <android/log.h>
//#include <zip.h>
//#include <zipint.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>



#ifdef __ANDROID__
#define STRINGIFY(x) #x
#define LOG_TAG    __FILE__ ":" STRINGIFY(__MyNative__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)



#endif

//#endif /* DEF_H_ */

