package humer.uvc_camera.UVC_Descriptor;

import android.util.Log;

import static humer.uvc_camera.UVC_Descriptor.UVC_Descriptor.log;

public class UVC_Initializer implements IUVC_Descriptor {

    // MJpeg
    public static int [] [] mJpegResolutions = null;
    public static int [] [] arrayToResolutionFrameInterValArrayMjpeg = null;

    // Yuv
    public static int [] [] yuvResolutions = null;
    public static int [] [] arrayToResolutionFrameInterValArrayYuv = null;

    public UVC_Initializer (UVC_Descriptor uvc_desc) {

        UVC_Descriptor.FormatIndex mJpegFormatIndex = null;
        UVC_Descriptor.FormatIndex yUvFormatIndex = null;

        for (int i = 0; i < uvc_desc.formatIndex.size() ; i++) {
            if (uvc_desc.formatIndex.get(i).videoformat == UVC_Descriptor.FormatIndex.Videoformat.mjpeg)
                mJpegFormatIndex = uvc_desc.getFormatIndex(i);
            else yUvFormatIndex = uvc_desc.getFormatIndex(i);
        }
        if (mJpegFormatIndex != null) {
            mJpegResolutions = new int [mJpegFormatIndex.frameIndex.size()] [2];
            arrayToResolutionFrameInterValArrayMjpeg = new int [mJpegFormatIndex.frameIndex.size()] [];
            for (int i = 0; i < mJpegFormatIndex.frameIndex.size() ; i++) {
                mJpegResolutions [i] [0] = mJpegFormatIndex.getFrameIndex(i).wWidth;
                mJpegResolutions [i] [1] = mJpegFormatIndex.getFrameIndex(i).wHeight;
                arrayToResolutionFrameInterValArrayMjpeg [i] = mJpegFormatIndex.getFrameIndex(i).dwFrameInterval;
            }
        }
        if (yUvFormatIndex != null) {
            yuvResolutions = new int [yUvFormatIndex.frameIndex.size()] [2];
            arrayToResolutionFrameInterValArrayYuv = new int [yUvFormatIndex.frameIndex.size()] [];

            for (int i = 0; i < yUvFormatIndex.frameIndex.size() ; i++) {
                yuvResolutions [i] [0] = yUvFormatIndex.getFrameIndex(i).wWidth;
                yuvResolutions [i] [1] = yUvFormatIndex.getFrameIndex(i).wHeight;
                arrayToResolutionFrameInterValArrayYuv[i] = yUvFormatIndex.getFrameIndex(i).dwFrameInterval;
            }
        }


        log("arrayToResolutionFrameInterValArrayMjpeg.length = " + arrayToResolutionFrameInterValArrayMjpeg.length);
        log("arrayToResolutionFrameInterValArrayMjpeg[0].length = " + arrayToResolutionFrameInterValArrayMjpeg[0].length);
        log("arrayToResolutionFrameInterValArrayMjpeg[1].length = " + arrayToResolutionFrameInterValArrayMjpeg[1].length);



    }

    public UVC_Initializer (int [] [] mJpegResolutions, int [] [] arrayToResolutionFrameInterValArrayMjpeg,
                            int [] [] yuvResolutions, int [] [] arrayToResolutionFrameInterValArrayYuv) {
        this.mJpegResolutions = mJpegResolutions;
        this.arrayToResolutionFrameInterValArrayMjpeg = arrayToResolutionFrameInterValArrayMjpeg;
        this.yuvResolutions = yuvResolutions;
        this.arrayToResolutionFrameInterValArrayYuv = arrayToResolutionFrameInterValArrayYuv;
    }

    @Override
    public int[] []findDifferentResolutions(boolean Mjpeg) {
        if (Mjpeg) {
            return mJpegResolutions;
        } else {
            return yuvResolutions;
        }
    }

    @Override
    public int [] findDifferentFrameIntervals(boolean Mjpeg, int[] widthHight) {
        if (Mjpeg) {
            Log.d("Initializer", "arrayToResolutionFrameInterValArrayMjpeg[0].length: " + arrayToResolutionFrameInterValArrayMjpeg[0].length);
            Log.d("Initializer", "arrayToResolutionFrameInterValArrayMjpeg[].length: " + arrayToResolutionFrameInterValArrayMjpeg[1].length);

            for (int i = 0; i < mJpegResolutions[0].length ; i++) {
                if (arrayToResolutionFrameInterValArrayMjpeg[i][0] == widthHight[0] &&
                        arrayToResolutionFrameInterValArrayMjpeg[i][1] == widthHight[1] )
                    return new int [] {arrayToResolutionFrameInterValArrayMjpeg[i][0], arrayToResolutionFrameInterValArrayMjpeg[i][1]};
            }
        } else {
            for (int i = 0; i < yuvResolutions[0].length ; i++) {
                if (arrayToResolutionFrameInterValArrayYuv[i][0] == widthHight[0] &&
                        arrayToResolutionFrameInterValArrayYuv[i][1] == widthHight[1] )
                    return new int [] {arrayToResolutionFrameInterValArrayYuv[i][0], arrayToResolutionFrameInterValArrayYuv[i][1]};
            }
        }
        return null;
    }







}
