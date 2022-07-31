#include "test.h"
#include "trace.h"

extern "C" {

#include <jpeglib.h>
}

extern AAssetManager *mgr;


int ReadStringAM(AAsset* asset, char * buffer, int count, int offset) {


	AAsset_seek(asset, offset, SEEK_SET);
	int r_count = AAsset_read(asset, buffer, count);

	if (r_count <= 0)
		return r_count;

	for (int i = 0; i < r_count; ++i) {
		if (buffer[i] == '\n') {
			buffer[i] = '\0';
			AAsset_seek(asset, offset + i + 1, SEEK_SET);
			return offset + i + 1;
		}
	}

	if (count == r_count && buffer[r_count - 1] != '\n')
		return 0;
	return offset + r_count + 1;
}

AAsset* MyFileOpen(const char* fname2) {

	char fname[80];
	strcpy(fname, "");
	strcat(fname, fname2);
	if(mgr == NULL)
		LOGE("AAssetManager null");


	AAsset* asset = AAssetManager_open(mgr, fname, AASSET_MODE_UNKNOWN);
	if (mgr == NULL) {
		LOGE("AAssetManager NULL");
		return NULL;
	}
	if (NULL == asset) {
		LOGE("_ASSET_NOT_FOUND_ %s", fname);
		return NULL;
	}

	return asset;
}


static void _JpegError(j_common_ptr cInfo){
    char pszMessage[JMSG_LENGTH_MAX];

    (*cInfo->err->format_message)(cInfo, pszMessage);

    LOGE("Jpeg Lib","error!  %s", pszMessage);
}
static uint32_t  make8888(int red, int green, int blue, int alpha){
	//LOGD("[%i;%i;%i]",red,green,blue);
    return (uint32_t)( ((alpha   << 24) & 0xff000000) |
                       ((blue << 16) & 0x00ff0000) |
                       ((green << 8) & 0x0000ff00) |
                       ( red & 0x000000ff) );
}
GLuint loadTextureFromJPEG(const char* filename, int &width, int &height){
	AAsset* pAsset = NULL;


	struct jpeg_decompress_struct cInfo;
	struct jpeg_error_mgr jError;
	cInfo.err = jpeg_std_error(&jError); // register error handler 1
	jError.error_exit = _JpegError; // register error handler 2
	jpeg_create_decompress(&cInfo); // create a decompresser


	// load from asset
	pAsset = AAssetManager_open(mgr, filename, AASSET_MODE_UNKNOWN);
	if (!pAsset) {
		LOGD("!pAsset");
		return NULL;
	}

	unsigned char* ucharRawData = (unsigned char*)AAsset_getBuffer(pAsset);
	long myAssetLength = (long)AAsset_getLength(pAsset);

	// the jpeg_stdio_src alternative func, which is also included in IJG's lib.
	jpeg_mem_src(&cInfo, ucharRawData, myAssetLength);


	uint32_t* pTexUint;
	int yy;
	int  pixelSize, lineSize;
	char* lpbtBits;
	JSAMPLE tmp;
	int rectHeight, rectWidth;

	jpeg_read_header(&cInfo, TRUE); // read header
	jpeg_start_decompress(&cInfo); // start decompression
	//LOGD("cInfo %i - %i",cInfo.output_width,cInfo.output_height);
	width = cInfo.output_width;
	height = height = cInfo.output_height;
	pixelSize = cInfo.output_components;
	lineSize = width * pixelSize;

	pTexUint = (uint32_t*)calloc(sizeof(uint32_t), width * height);
	if (pTexUint == NULL){
		AAsset_close(pAsset);

		return NULL;
	}

	JSAMPLE* pSample = (JSAMPLE*)calloc(sizeof(JSAMPLE), lineSize + 10);
	if (!pSample){
		LOGE("Jpeg Lib","cannot alloc pSample");
		if (pTexUint) free(pTexUint);

		AAsset_close(pAsset);

		return NULL; //error
	}

	JSAMPROW buffer[1];
	buffer[0] = pSample;

	uint32_t* pPixelsUint = pTexUint;
	yy = 0;
	while(cInfo.output_scanline < cInfo.output_height){
		if(yy >= cInfo.output_height)
			break;

		jpeg_read_scanlines(&cInfo, buffer, 1);

		int xx;
		int x3;
		for(xx = 0, x3 = 0; xx < width; xx++, x3 += 3)
			pPixelsUint[xx] = make8888(buffer[0][x3], buffer[0][x3 + 1], buffer[0][x3 + 2], 0xff);

		pPixelsUint = (uint32_t*)pPixelsUint + width;
		yy++;
	}
	//LOGD("sizeof(*pPixelsUint) = %i", sizeof(*pPixelsUint));
	GLuint texture;
	glGenTextures(1, &texture);
	glBindTexture(GL_TEXTURE_2D, texture);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (GLvoid*)pTexUint);


	jpeg_finish_decompress(&cInfo);
	jpeg_destroy_decompress(&cInfo);

	if (pSample) free(pSample);

	AAsset_close(pAsset);
	//free(pTexUint);
	return texture;
	//return (unsigned char*)pTexUint;

}
