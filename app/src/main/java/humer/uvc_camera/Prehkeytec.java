package humer.uvc_camera;

// TODO:
// - Explizit einen isochronousen oder bulk Endpoint auswählen.
// - Alt-Interface automatisch suchen aufgrund von maxPacketSize und
// - Sauberen Close/Open programmieren. econ 5MP USB3 läuft nur nach re-open.


public class Prehkeytec {

    public void kameraEinstellungen(int kamera, int mjpegYuv) {


        switch (kamera) {                           // temporary solution
            case 0:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 2000000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 3;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 24;
                Main.activeUrbs = 2;
                Main.kameramodel = 'p';
                break;
            case 1:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 2000000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 3;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 8;
                Main.activeUrbs = 2;
                Main.kameramodel = 'p';
                break;
            case 2:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 500000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 2;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 12;
                Main.activeUrbs = 3;
                Main.kameramodel = 'p';
                break;
            case 3:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 4000000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 2;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 9;
                Main.activeUrbs = 3;
                Main.kameramodel = 'p';
                break;
            case 4:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 2000000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 16;
                Main.activeUrbs = 2;
                Main.kameramodel = 'p';
                break;
            case 5:
                Main.camStreamingAltSetting = 4;               // 1 = 3x1024 bytes packet size // 2 = 2x 1024 // 3 = 1x 1024 e // 4 = 1x 512
                Main.maxPacketSize = 1*512;
                Main.camFormatIndex = 1;                    // bFormatIndex: 1 = MJPEG
                Main.camFrameInterval = 2000000;            // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                Main.camFrameIndex = 1;                          // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                switch (Main.camFrameIndex) {
                    case 1: Main.imageWidth = 1920;
                        Main.imageHeight = 1080;
                        break;
                    case 2: Main.imageWidth = 1280;
                        Main.imageHeight = 1024;
                        break;
                    case 3: Main.imageWidth = 1280;
                        Main.imageHeight = 720;
                        break;
                }
                Main.packetsPerRequest = 12;
                Main.activeUrbs = 2;
                Main.kameramodel = 'p';
                break;



            default:
               break;
        }
    }
}
/*

switch (cameraType) {                           // temporary solution
            case prehkeytec: // für Acer
                camStreamingAltSetting = 4;              // 4 = 1x 512 bytes packet size
                maxPacketSize = 512;
                camFormatIndex = 1;                       // bFormatIndex: 1 = MJPEG
                camFrameIndex = 1;                        // bFrameIndex: 1 = 1920 x 1080, 2 = 1280 x 1024, 3 = 1280 x 720
                camFrameInterval = 2000000;               // dwFrameInterval: 333333 =  30 fps // 400000 = 25 fps // 500000 = 20 fps   // 666666 = 15 fps // 1000000 = 10 fps   2000000 = 5 fps
                // camFrameInterval = 2000000;
                packetsPerRequest = 4;             //128 für XGo
                activeUrbs = 4;                     //128 für XGo
                kameramodel = 'p';
                break;
 */