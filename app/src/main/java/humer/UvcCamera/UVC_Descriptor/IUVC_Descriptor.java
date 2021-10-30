package humer.UvcCamera.UVC_Descriptor;

public interface IUVC_Descriptor {

    public int[] [] findDifferentResolutions(boolean Mjpeg);

    public int [] findDifferentFrameIntervals(boolean Mjpeg, int[] widthHight);

}
