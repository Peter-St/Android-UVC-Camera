#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#define TEXTURE_LOAD_ERROR 0
AAsset* MyFileOpen( const char* fname2);
int ReadStringAM(AAsset* asset, char * buffer, int count, int offset);
GLuint loadTextureFromJPEG(const char* filename, int &width, int &height);
