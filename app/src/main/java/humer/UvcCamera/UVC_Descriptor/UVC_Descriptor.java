/*
Copyright 2019 Peter Stoiber

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Please contact the author if you need another license.
This Repository is provided "as is", without warranties of any kind.

*/

package humer.UvcCamera.UVC_Descriptor;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

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

    // Descriptors
    private final static byte Stand_VS_Interface_Desc_Size                           = 0x09;
    private final static byte INTERFACE_descriptor_type                              = 0x04;
    private final static byte Standard_VS_Isochronous_Video_Data_Endpoint_Descriptor = 0x07;
    private final static byte ENDPOINT_descriptor_type                               = 0x05;



    public ArrayList<UVC_Descriptor.FormatIndex> formatIndex;
    private static ByteBuffer uvcData;

    // Values for Controltransfers when setting the brightness ...
    public static byte bUnitID;
    public static byte bTerminalID;
    public static byte bStillCaptureMethod;
    public static byte[] bNumControlTerminal;
    public static byte[] bNumControlUnit;

    // Version of UVC
    public static byte[] bcdUVC;

    //MaxPacketSize
    public static List<Integer> maxPacketSizeArray = new ArrayList<Integer>();


    public UVC_Descriptor(ByteBuffer data) {  //convertedMaxPacketSize
        this.uvcData = ByteBuffer.allocate(data.limit());
        this.uvcData = data.duplicate();
    }

    public int phraseUvcData() {

        formatIndex = new ArrayList<>();
        try {
            boolean foundPROCESSING_UNIT = false;
            boolean foundINPUT_TERMINAL = false;
            boolean foundINPUT_HEADER_IN_Endpoint = false;
            boolean videoStreamInterfaceDescriptor = false;
            int number_of_Standard_VS_Interface_Descriptor = 0;




            maxPacketSizeArray = new ArrayList<Integer>();
            ArrayList<byte []> frameData = new ArrayList<>();
            byte[] formatData = null;
            int positionAbsolute = 0;
            printData(uvcData.array());
            do  {
                int pos = uvcData.position();
                byte descSize = uvcData.get(pos);
                byte descType = uvcData.get(pos +1);
                byte descSubType = uvcData.get(pos + 2);

                //Get Version of UVC (bcdUVC)
                if (descType == 0x24 && descSubType == 0x01 && !foundPROCESSING_UNIT && !foundINPUT_HEADER_IN_Endpoint) {
                    bcdUVC = new byte [2];
                    bcdUVC[0] = uvcData.get(pos + 3);
                    bcdUVC[1] = uvcData.get(pos + 4);
                }

                //Get Still Image Support
                if (descType == 0x24 && descSubType == 0x01 && foundPROCESSING_UNIT && !foundINPUT_HEADER_IN_Endpoint) {
                    foundINPUT_HEADER_IN_Endpoint = true;
                    bStillCaptureMethod = uvcData.get(pos + 9);
                }

                //Set  INPUT_TERMINAL bTerminalID, bNumControlTerminal
                if (descType == 0x24 && descSubType == 0x02) {
                    if (!foundINPUT_TERMINAL) {
                        foundINPUT_TERMINAL = true;
                        bTerminalID = uvcData.get(pos +3);
                        bNumControlTerminal = new byte[uvcData.get(pos+14)];
                        for (int i = 0; i < bNumControlTerminal.length; i++) {
                            bNumControlTerminal[i] = uvcData.get(pos + 15 + i);
                        }
                    }
                }

                // Set PROCESSING_UNIT bUnitID, bNumControlUnit
                if (descType == 0x24 && descSubType == 0x05) {
                    if (!foundPROCESSING_UNIT) {
                        foundPROCESSING_UNIT = true;
                        bUnitID = uvcData.get(pos +3);
                        bNumControlUnit = new byte[uvcData.get(pos+7)];
                        for (int i = 0; i < bNumControlUnit.length; i++) {
                            bNumControlUnit[i] = uvcData.get(pos + 8 + i);
                        }
                    }
                }


                //search for the VideoStreamInterface
                if (descSize == 0x09 && uvcData.get(pos +5) == CC_VIDEO &&  uvcData.get(pos +6) == SC_VIDEOSTREAMING) {
                    videoStreamInterfaceDescriptor = true;
                }

                if (videoStreamInterfaceDescriptor) {
                    if (descSubType == VS_format_uncompressed) {
                        formatData = new byte [descSize];
                        uvcData.get(formatData, 0 ,descSize);
                        frameData = new ArrayList<>();
                        //printData(formatData);
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
                if (descSize == Stand_VS_Interface_Desc_Size && descType == INTERFACE_descriptor_type && videoStreamInterfaceDescriptor)  number_of_Standard_VS_Interface_Descriptor ++;
                if (descSize == Standard_VS_Isochronous_Video_Data_Endpoint_Descriptor && descType == ENDPOINT_descriptor_type && videoStreamInterfaceDescriptor) {
                    int packetSize  = (((uvcData.get(pos + 5) & 0xFF) << 8) | uvcData.get(pos + 4) & 0xFF);
                    maxPacketSizeArray.add(returnConvertedValue(packetSize));
                }
                positionAbsolute += descSize;
                uvcData.position(positionAbsolute);
            } while (uvcData.limit() > positionAbsolute);
            System.out.println("UvcDescriptor finished.");
            return 0;

        } catch ( Exception e ) {e.printStackTrace(); }

        return -1;
    }

    private int returnConvertedValue(int wSize){
        String st = Integer.toBinaryString(wSize);
        StringBuilder result = new StringBuilder();
        result.append(st);
        if (result.length()<12) return Integer.parseInt(result.toString(), 2);
        else if (result.length() == 12) {
            String a = result.substring(0, 1);
            String b = result.substring(1, 12);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c+1)*d;
        } else {
            String a = result.substring(0, 2);
            String b = result.substring(2,13);
            int c = Integer.parseInt(a, 2);
            int d = Integer.parseInt(b, 2);
            return (c+1)*d;
        }
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
        public enum Videoformat {YUV, MJPEG, YUY2, YV12, YUV_422_888, YUV_420_888}
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
                // YUY2
                if (guidFormat.equals("5955593200001000800000aa00389b71") ) { videoformat = Videoformat.YUY2; System.out.println("videoformat = Videoformat.YUY2"); }
                else if (guidFormat.equals("5955593200000010800000aa00389b71") ) {videoformat = Videoformat.YUY2;System.out.println("videoformat = Videoformat.YUY2");}
                else if (guidFormat.equals("3259555900000010800000aa00389b71") ) {videoformat = Videoformat.YUY2;System.out.println("videoformat = Videoformat.YUY2");}
                // YV12
                else if (guidFormat.equals("3032344900000010800000aa00389b71") ) {videoformat = Videoformat.YV12;System.out.println("videoformat = Videoformat.YV12");}
                else if (guidFormat.equals("5655594900000010800000aa00389b71") ) {videoformat = Videoformat.YV12;System.out.println("videoformat = Videoformat.YV12");}
                else if (guidFormat.equals("3131325900000010800000aa00389b71") ) {videoformat = Videoformat.YV12;System.out.println("videoformat = Videoformat.YV12");}
                else if (guidFormat.equals("5956313200001000800000aa00389b71") ) {videoformat = Videoformat.YV12;System.out.println("videoformat = Videoformat.YV12");}
                else if (guidFormat.equals("5956313200000010800000aa00389b71") ) {videoformat = Videoformat.YV12;System.out.println("videoformat = Videoformat.YV12");}
                // YUV_420_888
                else if (guidFormat.equals("4934323000000010800000aa00389b71") ) {videoformat = Videoformat.YUV_420_888;System.out.println("videoformat = Videoformat.YUV_420_888");}
                else if (guidFormat.equals("4934323000001000800000aa00389b71") ) {videoformat = Videoformat.YUV_420_888;System.out.println("videoformat = Videoformat.YUV_420_888");}
                // YUV_422_888
                else if (guidFormat.equals("5559565900001000800000aa00389b71") ) {videoformat = Videoformat.YUV_422_888;System.out.println("videoformat = Videoformat.YUV_422_888");}
                else if (guidFormat.equals("5559565900000010800000aa00389b71") ) {videoformat = Videoformat.YUV_422_888;System.out.println("videoformat = Videoformat.YUV_422_888");}
                else guidFormat = "unknown";
            }
            else if (formatData[2] ==  VS_format_mjpeg ) {
                videoformat = Videoformat.MJPEG;
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

            public int frameIndex;
            public int [] dwFrameInterval;
            public int wWidth;
            public int wHeight;

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
