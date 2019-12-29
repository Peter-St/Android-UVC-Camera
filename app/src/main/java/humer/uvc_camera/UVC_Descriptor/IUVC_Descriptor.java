package humer.uvc_camera.UVC_Descriptor;

import java.nio.ByteBuffer;

public interface IUVC_Descriptor {

    public int[] [] findDifferentResolutions(boolean Mjpeg);

    public int [] findDifferentFrameIntervals(boolean Mjpeg, int[] widthHight);

}
