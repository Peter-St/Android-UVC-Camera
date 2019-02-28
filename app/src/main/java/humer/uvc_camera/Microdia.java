package humer.uvc_camera;

// TODO:
// - Explizit einen isochronousen oder bulk Endpoint auswählen.
// - Alt-Interface automatisch suchen aufgrund von maxPacketSize und
// - Sauberen Close/Open programmieren. econ 5MP USB3 läuft nur nach re-open.


public class Microdia {

    public void kameraEinstellungen(int kamera, int mjpegYuv) {


        switch (kamera) {                           // temporary solution
            case 0:
                Main.camStreamingAltSetting = 2;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 256;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;

                } else {
                    Main.camFormatIndex = 1;
                    Main.camFrameInterval = 1111111;

                }
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.packetsPerRequest = 64;
                Main.activeUrbs = 8;
                Main.kameramodel = 'm';
                break;
            case 1:
                Main.camStreamingAltSetting = 2;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 256;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;

                } else {
                    Main.camFormatIndex = 1;
                    Main.camFrameInterval = 1111111;
                }
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.packetsPerRequest = 64;
                Main.activeUrbs = 4;
                Main.kameramodel = 'm';
                break;
            case 2:
                Main.camStreamingAltSetting = 2;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 256;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;

                } else {
                    Main.camFormatIndex = 1;
                    Main.camFrameInterval = 1111111;
                }
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.packetsPerRequest = 16;
                Main.activeUrbs = 16;
                Main.kameramodel = 'm';
                break;
            case 3:
                Main.camStreamingAltSetting = 1;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 128;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;               // 666666 MJPEG = 15 fps

                } else {
                    Main.camFormatIndex = 1;                          // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 1111111;            // 1111111 YUV = 9 fps --
                }
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.packetsPerRequest = 16;
                Main.activeUrbs = 16;
                Main.kameramodel = 'm';
                break;
            case 4:
                Main.camStreamingAltSetting = 2;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 256;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;               // 666666 MJPEG = 15 fps

                } else {
                    Main.camFormatIndex = 1;                          // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 1111111;            // 1111111 YUV = 9 fps --
                }
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.packetsPerRequest = 64;
                Main.activeUrbs = 8;
                Main.kameramodel = 'm';
                break;
            case 5:
                Main.camStreamingAltSetting = 3;              // 6 = 3* 1024 // 1 = 1* 128 // 2 = 1* 256 // 3 = 1* 800 // 4 = 2* 800 // 5 = 3* 800 // 6 = 3* 1024
                Main.maxPacketSize = 800;
                if (mjpegYuv == 0) {
                    Main.camFormatIndex = 2;                       // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 666666;               // 666666 MJPEG = 15 fps

                } else {
                    Main.camFormatIndex = 1;                          // bFormatIndex: 1 = uncompressed YUY2, 2 = MJPEG
                    Main.camFrameInterval = 1111111;            // 1111111 YUV = 9 fps --
                }
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1280 x 720,
                Main.imageWidth = 1280;
                Main.imageHeight = 720;
                Main.packetsPerRequest = 64;
                Main.activeUrbs = 4;
                Main.kameramodel = 'm';
                break;
            default:
               break;
        }
    }
}
