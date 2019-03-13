package humer.uvc_camera;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Formatter;

public class UVC_Descriptor {


    //Video Interface Class Code
    private final static byte CC_VIDEO = 0x0E;

    // Video_Subclass Code  Value
    private final static byte SC_UNDEFINED                  = 0x00;
    private final static byte SC_VIDEOCONTROL               = 0x01;
    private final static byte SC_VIDEOSTREAMING             = 0x02;
    private final static byte SC_VIDEO_INTERFACE_COLLECTION = 0x03;



    // VS Interface Descriptor Subtypes
    private final static byte VS_UNDEFINED = 0x00;
    private final static byte VS_input_header = 0x01;
    private final static byte VS_still_image_frame = 0x03;
    private final static byte VS_format_uncompressed = 0x04;
    private final static byte VS_frame_uncompressed = 0x05;
    private final static byte VS_format_mjpeg = 0x06;
    private final static byte VS_frame_mjpeg = 0x07;
    private final static byte VS_colour_format = 0x0D;
    public final ArrayList<UVC_Descriptor.FormatIndex> formatIndex = new ArrayList<>();

    private Main uvc_camera;
    private Context mContext;
    private Activity activity;
    private ByteBuffer uvcData;

    private TextView tv;
    private Button settingsButton;




    public UVC_Descriptor(ByteBuffer data) {  //convertedMaxPacketSize
        this.uvcData = ByteBuffer.allocate(data.limit());
        this.uvcData = data.duplicate();
    }

    public int phraseUvcData() {
        try {
            boolean videoStreamInterfaceDescriptor = false;
            ArrayList<byte []> frameData = new ArrayList<>();
            byte[] formatData = null;
            int positionAbsolute = 0;
            do  {

                int pos = uvcData.position();
                byte descSize = uvcData.get(pos);
                byte descType = uvcData.get(pos +1);
                byte descSubType = uvcData.get(pos + 2);

                //search for the VideoStreamInterface
                if (descSize == 0x09 && uvcData.get(pos +5) == CC_VIDEO &&  uvcData.get(pos +6) == SC_VIDEOSTREAMING) {
                    videoStreamInterfaceDescriptor = true;
                }

                if (videoStreamInterfaceDescriptor) {
                    if (descSubType == VS_format_uncompressed) {
                        formatData = new byte [descSize];
                        uvcData.get(formatData, 0 ,descSize);
                        frameData = new ArrayList<>();
                        printData(formatData);
                    }
                    else if (descSubType == VS_frame_uncompressed) {
                        byte [] uncompressedFrameData = new byte [descSize];
                        uvcData.get(uncompressedFrameData, 0 ,descSize);
                        frameData.add(uncompressedFrameData);
                        if (uvcData.get(pos + descSize + 2) != VS_frame_uncompressed) {
                            FormatIndex formatUncomprIndex = new FormatIndex(formatData, frameData);
                            formatUncomprIndex.init();
                            formatIndex.add(formatUncomprIndex);
                        }
                    }
                    if (descSubType == VS_format_mjpeg) {
                        formatData = new byte [descSize];
                        uvcData.get(formatData, 0 ,descSize);
                        frameData = new ArrayList<>();
                        printData(formatData);
                    }
                    else if (descSubType == VS_frame_mjpeg) {
                        byte [] mjpegFrameData = new byte [descSize];
                        uvcData.get(mjpegFrameData, 0 ,descSize);
                        frameData.add(mjpegFrameData);
                        if (uvcData.get(pos + descSize + 2) != VS_frame_mjpeg) {
                            FormatIndex formatUncomprIndex = new FormatIndex(formatData, frameData);
                            formatUncomprIndex.init();
                            formatIndex.add(formatUncomprIndex);
                        }
                    }
                }
                positionAbsolute += descSize;
                uvcData.position(positionAbsolute);
            } while (uvcData.limit() > positionAbsolute);
            System.out.println("UvcDescriptor finished.");
            return 0;

        } catch ( Exception e ) {e.printStackTrace(); }

        return -1;
    }

    public FormatIndex getFormatIndex(int n) {
        return formatIndex.get(n);
    }


    private static void printData (byte [] formatData) {

        Formatter formatter = new Formatter();
        for (byte b : formatData) {
            formatter.format("0x%02x ", b);
        }
        String hex = formatter.toString();

        System.out.println("hex " + hex);
    }




    public static class FormatIndex {

        public final ArrayList<FrameIndex> frameIndex = new ArrayList<>();
        public final byte[] formatData;
        public final ArrayList<byte []> frameData;
        public int formatIndexNumber;
        public int numberOfFrameDescriptors;
        public enum Videoformat {yuy2, mjpeg}
        public Videoformat videoformat;
        public String guidFormat = new String();


        public FormatIndex(byte[] format, ArrayList<byte []> frame){
            this.formatData = format;
            this.frameData = frame;
        }

        public void init() {
            // add more formats later ..


            formatIndexNumber = formatData[3];
            numberOfFrameDescriptors = formatData[4];
            System.out.println("(FormatData) formatIndexNumber = " + formatIndexNumber);
            System.out.println("(FormatData) formatData[2] = " + formatData[2]);
            if (formatData[2] ==  VS_format_uncompressed ) {
                // Guid Data
                Formatter formatter = new Formatter();

                for (int b=0; b<16 ; b++) {
                    formatter.format("%02x", formatData[(b + 5) & 0xFF]);
                }
                guidFormat = formatter.toString();
                System.out.println("guidFormat = " + guidFormat);
                if (guidFormat.equals("5955593200001000800000aa00389b71") ) {
                    videoformat = Videoformat.yuy2;
                    System.out.println("videoformat = Videoformat.yuy2");
                }
                else guidFormat = "unknown";
            }
            else if (formatData[2] ==  VS_format_mjpeg ) {
                videoformat = Videoformat.mjpeg;
            }

            for (int i = 0; i < frameData.size(); i++) {
                byte[] buf = new byte [frameData.get(i).length];
                buf = frameData.get(i);
                int index = buf[3];
                int pos = 5;
                int width = ((buf[pos+1] & 0xFF) << 8) | (buf[pos] & 0xFF);
                //int width = (buf[7]  << 8)  |  buf[6] & 0xFF ;
                int height = ((buf[pos+3] & 0xFF) << 8)  |  (buf[pos+2] & 0xFF) ;
                log ("width = " + width +  "  /  height = " + height);
                int [] frameintervall = new int[(buf.length - 26) /4];
                pos = 26;
                int x = 0;
                do {
                    frameintervall[x] = (buf[pos + 3] << 24) | ((buf[pos + 2] & 0xFF) << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
                    System.out.println("frameintervall[x] = " + frameintervall[x]);
                    x++;
                    pos += 4;
                } while (buf.length > pos);
                FrameIndex frameIndexData = new FrameIndex(index, width, height, frameintervall);
                frameIndex.add(frameIndexData);
            }
        }

        public FrameIndex getFrameIndex(int n) {
            return frameIndex.get(n);
        }

        public static class FrameIndex{

            int frameIndex;
            int [] dwFrameInterval;
            int wWidth;
            int wHeight;

            public FrameIndex(int index, int width, int height, int[] frameInterval){
                this.frameIndex = index;
                this.wWidth = width;
                this.wHeight = height;
                this.dwFrameInterval = frameInterval;

            }
        }
    }

    public static void log(String msg) {
        Log.i("UVC_Camera", msg);
    }


}
