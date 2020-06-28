package humer.UvcCamera;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface Libyuv extends Library {

    public static final Libyuv INSTANCE = Native.load("yuv_core", Libyuv.class);

    void convertNV21ToI420(byte[] data, int len, int rot);

    void convertYUY2ToI420(Pointer data, int len);

    void initialize(int src_w, int src_h, int dst_w, int dst_h, String url);

    void initialize_orig(int src_w, int src_h, int dst_w, int dst_h, String url_orig, String url);

    void release();

    void saveToUrl(Pointer data, int len);

    void YUY2toYV12 (Pointer YUY2Source, int len);

    Pointer convertYUY2ToNV21(Pointer data, int len);





}
