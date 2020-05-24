package humer.uvc_camera.LibYuv;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import humer.uvc_camera.UsbIso64.Libc;

public interface LibYuv extends Library {

    public static final LibYuv INSTANCE = Native.load("yuv", LibYuv.class);


    void rgbToBgrInternal(byte[] rgb, int width, int height, byte[] bgr) throws LastErrorException;
    void rgb565ToI420Internal(byte[] rgb, int width, int height, byte[] yuv) throws LastErrorException;
    void rgb565ToArgbInternal(byte[] rgb, int width, int height, byte[] argb) throws LastErrorException;
    void bgrToNV21Internal(byte[] bgr, int width, int height, byte[] yuv) throws LastErrorException;
    void bgrToNV12Internal(byte[] bgr, int width, int height, byte[] yuv) throws LastErrorException;
    void bgrToI420Internal(byte[] bgr, int width, int height, byte[] yuv) throws LastErrorException;
    void bgrToYV12Internal(byte[] bgr, int width, int height, byte[] yuv) throws LastErrorException;
    void I420ToNV21Internal(byte[] i420, int width, int height, byte[] nv21) throws LastErrorException;


}