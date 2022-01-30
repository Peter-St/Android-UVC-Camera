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

package humer.UvcCamera;

import android.app.Activity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.example.androidthings.videortc.WebRtc_MainActivity;
import com.sample.timelapse.MJPEGGenerator ;

import humer.UvcCamera.JNA_I_LibUsb.JNA_I_LibUsb;
import humer.UvcCamera.UVC_Descriptor.IUVC_Descriptor;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;
import humer.UvcCamera.UVC_Descriptor.UVC_Initializer;
import humer.UvcCamera.UsbIso64.USBIso;
import humer.UvcCamera.UsbIso64.usbdevice_fs_util;
import io.github.yavski.fabspeeddial.FabSpeedDial;
import io.github.yavski.fabspeeddial.SimpleMenuListenerAdapter;

import static java.lang.Integer.parseInt;

public class StartIsoStreamActivityUsbIso extends Activity {

    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";
    // USB codes:
// Request types (bmRequestType):
    private static final int RT_STANDARD_INTERFACE_SET = 0x01;
    private static final int RT_CLASS_INTERFACE_SET = 0x21;
    private static final int RT_CLASS_INTERFACE_GET = 0xA1;
    // Video interface subclass codes:
    private static final int SC_VIDEOCONTROL = 0x01;
    private static final int SC_VIDEOSTREAMING = 0x02;
    // Standard request codes:
    private static final int SET_INTERFACE = 0x0b;
    // Video class-specific request codes:
    private static final int SET_CUR = 0x01;
    private static final int GET_CUR = 0x81;
    private static final int GET_MIN = 0x82;
    private static final int GET_MAX = 0x83;
    private static final int GET_RES = 0x84;
    // VideoControl interface control selectors (CS):
    private static final int VC_REQUEST_ERROR_CODE_CONTROL = 0x02;
    // VideoStreaming interface control selectors (CS):
    private static final int VS_PROBE_CONTROL = 0x01;
    private static final int VS_COMMIT_CONTROL = 0x02;
    private static final int VS_STILL_PROBE_CONTROL = 0x03;
    private static final int VS_STILL_COMMIT_CONTROL = 0x04;
    private static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    private static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;

    // Android USB Classes
    private UsbManager usbManager;
    private UsbDevice camDevice = null;
    private UsbDeviceConnection camDeviceConnection;
    private UsbInterface camControlInterface;
    private UsbInterface camStreamingInterface;
    private UsbEndpoint camStreamingEndpoint;
    private PendingIntent mPermissionIntent;

    // Camera Values
    public static int       camStreamingAltSetting;
    public static int       camFormatIndex;
    public int              camFrameIndex;
    public static int       camFrameInterval;
    public static int       packetsPerRequest;
    public static int       maxPacketSize;
    public int              imageWidth;
    public int              imageHeight;
    public static int       activeUrbs;
    public static String    videoformat;
    public static boolean   camIsOpen;
    public static byte      bUnitID;
    public static byte      bTerminalID;
    public static byte      bStillCaptureMethod;
    public static byte[]    bNumControlTerminal;
    public static byte[]    bNumControlUnit;
    public static byte[]    bcdUVC;
    public static byte[]    bcdUSB;
    public static boolean   LIBUSB;
    public static boolean   moveToNative;
    public static boolean   bulkMode;


    // Vales for debuging the camera
    private boolean imageCapture = false;
    private boolean videorecord = false;
    private boolean videorecordApiJellyBeanNup = false;
    private boolean stopKamera = false;
    private boolean pauseCamera = false;
    private boolean longclickVideoRecord = false;
    private boolean stillImageAufnahme = false;
    private boolean saveStillImage = false;
    private String controlltransfer;
    private boolean exit = false;
    public StringBuilder stringBuilder;
    private int [] convertedMaxPacketSize;
    private boolean lowerResolution;
    public static enum Videoformat {YUV, MJPEG, YUY2, YV12, YUV_422_888, YUV_420_888, NV21, UYVY}

    // Buttons & Views
    protected ImageView imageView;
    protected Button startStream;
    protected Button menu;
    protected Button stopStreamButton;
    protected ImageButton photoButton;
    protected ToggleButton videoButton;
    private TextView tv;
    private Date date;
    private SimpleDateFormat dateFormat;
    private File file;

    // Time Values
    int lastPicture = 0; // Current picture counter
    int lastVideo = 0; // Current video file counter
    long startTime;
    long currentTime;

    // Other Classes
    private MJPEGGenerator generator;
    private BitmapToVideoEncoder bitmapToVideoEncoder;
    private volatile StartIsoStreamActivityUsbIso.IsochronousStream runningStream;
    private volatile IsochronousStreamLibUsb runningStreamLibUsb;
    private SeekBar simpleSeekBar;
    private Button defaultButton;
    private Switch switchAuto;

    // Camera Configuration Values to adjust Values over Controltransfers
    private boolean focusAutoState;
    private boolean exposureAutoState;
    float discrete=0;
    static float start;
    static float end;
    float start_pos;
    int start_position=0;
    private int framecount = 0;

    // UVC Interface
    private static IUVC_Descriptor iuvc_descriptor;

    private int [] differentFrameSizes;
    private int [] lastThreeFrames;
    private int whichFrame = 0;
    private String msg;
    private int rotate = 0;
    private boolean horizontalFlip;
    private boolean verticalFlip;

    // NEW LIBUSB VALUES
    private static int fd;
    private static int productID;
    private static int vendorID;
    private static String adress;
    private static int camStreamingEndpointAdress;
    private static String mUsbFs;
    private static int busnum;
    private static int devaddr;
    private volatile boolean libusb_is_initialized;
    private static final String DEFAULT_USBFS = "/dev/bus/usb";

    private Surface mPreviewSurface;
    private SurfaceView mUVCCameraView;

    private static StartIsoStreamActivityUsbIso instance;

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    camDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(camDevice != null){
                            log("camDevice from BraudcastReceiver");
                        }
                    }
                    else {
                        log( "permission denied for device " + camDevice);
                        displayMessage("permission denied for device " + camDevice);
                    }
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("STATE", "onStart() is called");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            unregisterReceiver(mUsbReceiver);
        } catch (Exception e) {
            log("Exception = " + e);
        }

        /*
        try {
            unbindService(mConnection);
        } catch (Exception e) {
            log("Exception = " + e);
        }
        */
    }

    @Override
    protected void onPause() {
        super.onPause();
        beenden(false);
    }



    public static StartIsoStreamActivityUsbIso getInstance() {
        if (instance == null) {
            instance = new StartIsoStreamActivityUsbIso();
        }
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.iso_stream_layout);
        imageView = (ImageView) findViewById(R.id.imageView);
        // Start onClick Listener method
        startStream = (Button) findViewById(R.id.startStream);
        startStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (camDevice == null) {
                    try {
                        findCam();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(StartIsoStreamActivityUsbIso.this, startStream);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.iso_stream_start_stream_menu, popup.getMenu());
                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        // Toast.makeText(Main.this,"Auswahl von: " + item.getTitle(),Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });
                popup.show();//showing popup menu
            }
        });//closing the setOnClickListener method
        startStream.getBackground().setAlpha(180);  // 25% transparent
        // Settings Button
        FabSpeedDial fabSpeedDial = (FabSpeedDial) findViewById(R.id.settingsButton);
        fabSpeedDial.setMenuListener(new SimpleMenuListenerAdapter() {
            @Override
            public boolean onMenuItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.selectTerminalUnit:
                        if (LIBUSB) return true;
                        selectUnitTerminal(findViewById(R.id.settingsButton));
                        fabSpeedDial.closeMenu();
                        return false;
                    case R.id.flipImage:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        ImageButton flip = (ImageButton) findViewById(R.id.flipLeftButton); flip.setEnabled(false); flip.setBackgroundDrawable(null);
                                        flip = (ImageButton) findViewById(R.id.flipRightButton); flip.setEnabled(false); flip.setBackgroundDrawable(null);
                                        ToggleButton flip2 = (ToggleButton) findViewById(R.id.flipHorizontalButton); flip2.setEnabled(false); flip2.setBackgroundDrawable(null);
                                        flip2 = (ToggleButton) findViewById(R.id.flipVerticalButton); flip2.setEnabled(false); flip2.setBackgroundDrawable(null);
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;
                                ImageButton flip = (ImageButton) findViewById(R.id.flipLeftButton); flip.setEnabled(true); flip.setBackgroundResource(R.drawable.bg_button_flip_left);
                                flip.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        flipLeft(null);
                                    }
                                });
                                flip = (ImageButton) findViewById(R.id.flipRightButton); flip.setEnabled(true); flip.setBackgroundResource(R.drawable.bg_button_flip_right);
                                flip.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        flipRight(null);
                                    }
                                });
                                ToggleButton flip2 = (ToggleButton) findViewById(R.id.flipHorizontalButton); flip2.setEnabled(true); flip2.setBackgroundResource(R.drawable.bg_toggle_button_flip_horizontal);
                                flip2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        flipHorizontal(null);
                                    }
                                });
                                flip2 = (ToggleButton) findViewById(R.id.flipVerticalButton); flip2.setEnabled(true); flip2.setBackgroundResource(R.drawable.bg_toggle_button_flip_vertical);
                                flip2.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        flipVertical(null);
                                    }
                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.resolutionFrameInterval:
                        if (LIBUSB) return true;
                        changeResolutionFrameInterval(findViewById(R.id.settingsButton));
                        return false;
                    case R.id.webRTC:
                        startWebRTC();
                        return false;
                    case R.id.returnToConfigScreen:
                        returnToConfigScreen();
                        return false;
                    case R.id.beenden:
                        beenden(true);
                        return false;
                    default:
                        break;
                }
                return false;
            }
            @Override
            public void onMenuClosed() {
                log("Menu closed");
            }
        });
        FrameLayout layout = (FrameLayout)findViewById(R.id.switch_view);
        layout.setVisibility(View.GONE);
        photoButton = (ImageButton) findViewById(R.id.Bildaufnahme);
        final MediaPlayer mp2 = MediaPlayer.create(StartIsoStreamActivityUsbIso.this, R.raw.sound2);
        final MediaPlayer mp1 = MediaPlayer.create(StartIsoStreamActivityUsbIso.this, R.raw.sound1);
        photoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mp2.start();
                if (LIBUSB) JNA_I_LibUsb.INSTANCE.setImageCaptureLongClick();
                if (bStillCaptureMethod == 2)  stillImageAufnahme = true;
                return true;
            }
        });
        photoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp1.start();
                BildaufnahmeButtonClickEvent();
            }
        });
        photoButton.setEnabled(false);
        photoButton.setBackgroundResource(R.drawable.photo_clear);
        stopStreamButton = (Button) findViewById(R.id.stopKameraknopf);
        stopStreamButton.getBackground().setAlpha(20);  // 95% transparent
        stopStreamButton.setEnabled(false);
        videoButton = (ToggleButton) findViewById(R.id.videoaufnahme);
        videoButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Button Checked // Short Click
                    log("long click return; longclick = " + longclickVideoRecord);

                    // Prevent the double Start with longClick
                    if (longclickVideoRecord) {
                        return;
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        videorecordApiJellyBeanNup = true;
                        if (LIBUSB) JNA_I_LibUsb.INSTANCE.startVideoCapture();
                        lastVideo++;
                        bitmapToVideoEncoder = new BitmapToVideoEncoder(new BitmapToVideoEncoder.IBitmapToVideoEncoderCallback() {
                            @Override
                            public void onEncodingComplete(File outputFile) {
                                displayMessage("Encoding complete!");
                            }
                        });
                        bitmapToVideoEncoder.setFrameRate((10000000 / camFrameInterval )/ 2);
                        log("Framerate = "+ ((10000000 / camFrameInterval )/ 2));
                        date = new Date() ;
                        dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;
                        String dirname = "Video";


                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                            String videoFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                            File videoFolder = new File(videoFolderStr);
                            if (!videoFolder.exists()) {
                                videoFolder.mkdirs();
                            }
                            log ("bitmapToVideoEncoder.startEncoding");
                            bitmapToVideoEncoder.startEncoding(imageWidth, imageHeight, new File(videoFolder,"usbVideo" + lastVideo +"-" + dateFormat.format(date) + ".mp4"));
                        } else {
                            Context context = getApplicationContext();
                            File directory = context.getFilesDir();
                            File videoDir = new File(directory, "video");
                            if (!videoDir.exists()) {
                                if (!videoDir.mkdirs()) {
                                    Log.e("TravellerLog :: ", "Problem creating video folder");
                                }
                            }
                            log ("bitmapToVideoEncoder.startEncoding");
                            bitmapToVideoEncoder.startEncoding(imageWidth, imageHeight, new File(videoDir,"usbVideo" + lastVideo +"-" + dateFormat.format(date) + ".mp4"));
                        }

                        File sdPath = new File(getExternalFilesDir(null),dirname);
                        if (!sdPath.exists()) {
                            sdPath.mkdirs();
                        }
                    } else {
                        // Button Checked // Skip to Long Click

                        // The toggle is enabled
                        lastPicture = 0;
                        videorecord = true;
                        if (LIBUSB) JNA_I_LibUsb.INSTANCE.startVideoCapture();

                        String dirname = "Video";


                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                            String videoFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                            File videoFolder = new File(videoFolderStr);
                            if (!videoFolder.exists()) {
                                videoFolder.mkdirs();
                            }
                            File recFolder=new File(videoFolder, "rec");
                            if (!recFolder.exists()) {
                                recFolder.mkdirs();
                            }
                            if (recFolder.isDirectory()) {
                                String[] children = recFolder.list();
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].endsWith(".JPG"))
                                        new File(recFolder, children[i]).delete();
                                }
                            }
                            recFolder.delete();
                            displayMessage("Record started");
                            startTime = System.currentTimeMillis();
                            currentTime = System.currentTimeMillis();
                            log ("file saved");
                        } else {
                            Context context = getApplicationContext();
                            File directory = context.getFilesDir();
                            File videoDir = new File(directory, "Video");
                            if (!videoDir.exists()) {
                                if (!videoDir.mkdirs()) {
                                    Log.e("TravellerLog :: ", "Problem creating Video folder");
                                }
                            }
                            File recFolder=new File(videoDir, "rec");
                            if (!recFolder.exists()) {
                                recFolder.mkdirs();
                            }
                            //recFolder.delete();
                            displayMessage("Record started");
                            startTime = System.currentTimeMillis();
                            currentTime = System.currentTimeMillis();
                        }
                    }
                } else {
                    // Button not checked // Long Click
                    if (longclickVideoRecord) {
                        longclickVideoRecord = false;
                        // The toggle is disabled
                        pauseCamera = true;
                        videorecord = false;
                        if (LIBUSB) JNA_I_LibUsb.INSTANCE.stopVideoCaptureLongClick();
                        lastVideo ++;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        long videotime = (System.currentTimeMillis() - startTime) / 1000;
                        log ("long videotime = " + videotime);
                        double a = (double) ( lastPicture / (double) videotime);
                        log ("Double a = " + a);
                        int fps = round(a);
                        log("fps ( Frame per Secound ) = " + fps);
                        log ( "lastPicture = " + lastPicture);
                        date = new Date() ;
                        dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {

                            String videoFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                            File videoFolder = new File(videoFolderStr);
                            if (!videoFolder.exists()) {
                                videoFolder.mkdirs();
                            }

                            File recFolder=new File(videoFolder, "rec");
                            if (!recFolder.exists()) {
                                recFolder.mkdirs();
                            }
                            File fileVideo = new File(videoFolder, "usbVideo" + lastVideo + "_" + dateFormat.format(date) + ".avi");
                            try {
                                generator = new MJPEGGenerator(fileVideo, imageWidth, imageHeight, fps, lastPicture);

                                for (int addpic = 1; addpic <= lastPicture; addpic++) {
                                    String curjpg = new File(recFolder, addpic + ".JPG").getPath();
                                    final Bitmap bitmap = BitmapFactory.decodeFile(curjpg);
                                    generator.addImage(bitmap);
                                }
                                generator.finishAVI();
                            } catch (Exception e) {
                                displayMessage("Error: " + e);
                                e.printStackTrace();
                            }
                            if (recFolder.isDirectory()) {
                                String[] children = recFolder.list();
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].endsWith(".JPG"))
                                        new File(recFolder, children[i]).delete();
                                }
                            }
                            recFolder.delete();
                            displayMessage("Record stopped");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            pauseCamera = false;
                            generator = null;
                        } else {
                            Context context = getApplicationContext();
                            File directory = context.getFilesDir();
                            File videoFolder = new File(directory, "Video");
                            if (!videoFolder.exists()) {
                                if (!videoFolder.mkdirs()) {
                                    Log.e("TravellerLog :: ", "Problem creating Video folder");
                                }
                            }
                            File recFolder=new File(videoFolder, "rec");
                            if (!recFolder.exists()) {
                                recFolder.mkdirs();
                            }
                            File fileVideo = new File(videoFolder, "usbVideo" + lastVideo + "_" + dateFormat.format(date) + ".avi");
                            try {
                                generator = new MJPEGGenerator(fileVideo, imageWidth, imageHeight, fps, lastPicture);

                                for (int addpic = 1; addpic <= lastPicture; addpic++) {
                                    String curjpg = new File(recFolder, addpic + ".JPG").getPath();
                                    final Bitmap bitmap = BitmapFactory.decodeFile(curjpg);
                                    generator.addImage(bitmap);
                                }
                                generator.finishAVI();
                            } catch (Exception e) {
                                displayMessage("Error: " + e);
                                e.printStackTrace();
                            }
                            if (recFolder.isDirectory()) {
                                String[] children = recFolder.list();
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].endsWith(".JPG"))
                                        new File(recFolder, children[i]).delete();
                                }
                            }
                            recFolder.delete();
                            displayMessage("Record stopped");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            pauseCamera = false;
                            generator = null;
                        }
                    }
                    // Button not checked // Short Click
                    else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            pauseCamera = true;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            videorecordApiJellyBeanNup = false;
                            if (LIBUSB) JNA_I_LibUsb.INSTANCE.stopVideoCapture();
                            bitmapToVideoEncoder.stopEncoding();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            bitmapToVideoEncoder = null;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            pauseCamera = false;

                        } else {
                            // The toggle is disabled
                            pauseCamera = true;
                            videorecord = false;
                            if (LIBUSB) JNA_I_LibUsb.INSTANCE.stopVideoCapture();
                            lastVideo ++;
                            String dirname = "Video";

                            String videoFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                            File videoFolder = new File(videoFolderStr);
                            if (!videoFolder.exists()) {
                                videoFolder.mkdirs();
                            }


                            long videotime = (System.currentTimeMillis() - startTime) / 1000;
                            log ("long videotime = " + videotime);
                            double a = (double) ( lastPicture / (double) videotime);
                            log ("Double a = " + a);
                            int fps = round(a);
                            log("fps ( Frame per Secound ) = " + fps);
                            log ( "lastPicture = " + lastPicture);
                            date = new Date() ;
                            dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;
                            File fileVideo = new File(videoFolder,"output-" + lastVideo +"-" + dateFormat.format(date) + ".avi");
                            try {
                                generator = new MJPEGGenerator(fileVideo, imageWidth, imageHeight, fps, lastPicture);
                                for (int addpic = 1; addpic <= lastPicture; addpic++) {
                                    File sub_dir=new File(videoFolder, "rec");
                                    if (!sub_dir.exists()) {
                                        sub_dir.mkdirs();
                                    }
                                    String curjpg = new File(sub_dir,  addpic + ".JPG" ).getPath();
                                    final Bitmap bitmap = BitmapFactory.decodeFile(curjpg);
                                    generator.addImage(bitmap);
                                }
                                generator.finishAVI();
                            } catch (Exception e) {
                                displayMessage("Error: " + e);
                                e.printStackTrace();
                            }
                            File saveDir=new File(videoFolder, "rec");
                            if (!saveDir.exists()) {
                                saveDir.mkdirs();
                            }
                            if (saveDir.isDirectory()) {
                                String[] children = saveDir.list();
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].endsWith(".JPG"))
                                        new File(saveDir, children[i]).delete();
                                }}
                            saveDir.delete();
                            displayMessage("Record stopped");

                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            pauseCamera = false;
                            generator = null;
                        } } } }});
        videoButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // TODO Auto-generated method stub

                if (videoButton.isChecked()==(false))
                {
                    // Needs to stand first to prevent double Start with on Ceck Changed listener
                    longclickVideoRecord = true;
                    // button is unchecked
                    displayMessage("Long Click - Video starts");
                    log("Long Click - Video starts");
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                        String videoFolderStr = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                        File videoFolder = new File(videoFolderStr);
                        if (!videoFolder.exists()) {
                            videoFolder.mkdirs();
                        }
                        videoButton.setChecked(true);
                        if (LIBUSB) JNA_I_LibUsb.INSTANCE.startVideoCaptureLongClick();
                        lastPicture = 0;
                        videorecord = true;
                        File saveDir=new File(videoFolder, "rec");
                        if (!saveDir.exists()) {
                            saveDir.mkdirs();
                        }
                        if (saveDir.isDirectory()) {
                            String[] children = saveDir.list();
                            for (int i = 0; i < children.length; i++) {
                                if (children[i].endsWith(".JPG"))
                                    new File(saveDir, children[i]).delete();
                            }
                        }
                        //saveDir.delete();
                        displayMessage("Record started");
                        startTime = System.currentTimeMillis();
                        currentTime = System.currentTimeMillis();
                        log("long click method Started!");
                    } else {
                        // button is unchecked
                        videoButton.setChecked(true);
                        if (LIBUSB) JNA_I_LibUsb.INSTANCE.startVideoCaptureLongClick();
                        lastPicture = 0;
                        videorecord = true;
                        Context context = getApplicationContext();
                        File directory = context.getFilesDir();
                        File videoFolder = new File(directory, "Video");
                        if (!videoFolder.exists()) {
                            if (!videoFolder.mkdirs()) {
                                Log.e("TravellerLog :: ", "Problem creating Video folder");
                            }
                        }
                        File saveDir=new File(videoFolder, "rec");
                        if (!saveDir.exists()) {
                            saveDir.mkdirs();
                        }
                        if (saveDir.isDirectory()) {
                            String[] children = saveDir.list();
                            for (int i = 0; i < children.length; i++) {
                                if (children[i].endsWith(".JPG"))
                                    new File(saveDir, children[i]).delete();
                            }
                        }
                        //saveDir.delete();
                        displayMessage("Record started");
                        startTime = System.currentTimeMillis();
                        currentTime = System.currentTimeMillis();
                    }
                }
                return true;
            }});
        videoButton.setEnabled(false);
        videoButton.setAlpha(0); // 100% transparent
        fetchTheValues();
        log("packetsPerRequest = " + packetsPerRequest);
        log("activeUrbs = " + activeUrbs);
        simpleSeekBar = (SeekBar) findViewById(R.id.simpleSeekBar); simpleSeekBar.setEnabled(false); simpleSeekBar.setAlpha(0); simpleSeekBar = null;
        defaultButton = (Button) findViewById(R.id.defaultButton); defaultButton.setEnabled(false); defaultButton.setAlpha(0); defaultButton = null;
        switchAuto = (Switch) findViewById(R.id.switchAuto); switchAuto.setEnabled(false); switchAuto.setVisibility(View.GONE); switchAuto = null;
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        ImageButton flip = (ImageButton) findViewById(R.id.flipLeftButton); flip.setEnabled(false); flip.setBackgroundDrawable(null);
        flip = (ImageButton) findViewById(R.id.flipRightButton); flip.setEnabled(false); flip.setBackgroundDrawable(null);
        ToggleButton flip2 = (ToggleButton) findViewById(R.id.flipHorizontalButton); flip2.setEnabled(false); flip2.setBackgroundDrawable(null);
        flip2 = (ToggleButton) findViewById(R.id.flipVerticalButton); flip2.setEnabled(false); flip2.setBackgroundDrawable(null);
        //LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(StartIsoStreamService.NOTIFICATION));
        if (LIBUSB) {
            mUVCCameraView = (SurfaceView)findViewById(R.id.surfaceView);
            mPreviewSurface = mUVCCameraView.getHolder().getSurface();
            mUVCCameraView.getHolder().addCallback(mSurfaceViewCallback);
        } else {
            mUVCCameraView = (SurfaceView) findViewById(R.id.surfaceView);
            mUVCCameraView.setVisibility(View.GONE);
            mUVCCameraView.setVisibility(View.INVISIBLE);
        }
        //JNA_I_LibUsb.INSTANCE.native_uvc_unref_device();
    }
/*
    public void restartIntent() {
        Intent intent = new Intent(this, StartIsoStreamService.class);
        intent.putExtra(StartIsoStreamService.ACCESS_LIBUSB, "Resume");
        startService(intent);
    }
*/
    public void selectUnitTerminal(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.iso_stream_select_terminal_unit);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.adjustValuesUnit:
                        showAdjustValuesUnitMenu(findViewById(R.id.settingsButton));
                        return true;
                    case R.id.adjustValuesTerminal:
                        showAdjustValuesTerminalMenu(findViewById(R.id.settingsButton));
                        return true;
                }
                return false;
            }
        });
        popup.show();//showing popup menu
    }

    public void changeResolutionFrameInterval (View v) {
        if (iuvc_descriptor == null)  {
            //alertDialog.dismiss();
            displayMessage("Start the Camera Stream first !");
            return;
        }
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.iso_stream_resolution_frameinterval);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                CFAlertDialog.Builder builder;
                switch (item.getItemId()) {
                    case R.id.resolution:
                        builder = new CFAlertDialog.Builder(StartIsoStreamActivityUsbIso.this);
                        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
                        builder.setTitle("Select your Resolution");
                        builder.setMessage("Current Resolution: " + imageWidth + "x" + imageHeight);
                        int [] [] resolutions;
                        if (videoformat.equals("MJPEG")) resolutions = iuvc_descriptor.findDifferentResolutions(true);
                        else resolutions = iuvc_descriptor.findDifferentResolutions(false);
                        log("resolutions.length = " + resolutions.length);
                        String [] resString = new String [resolutions.length];
                        for (int a = 0; a < resolutions.length; a++) resString[a] = Arrays.toString(resolutions[a]);
                        for (int a = 0; a < resolutions.length; a++) log("Arrays.toString(resolutions[" + a + "]  =  " + Arrays.toString(resolutions[a]));
                        builder.setItems(resString , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int index) {
                                log("resolutions[index][0] = " + resolutions[index][0]);
                                log("resolutions[index][1] = " + resolutions[index][1]);
                                imageWidth = resolutions[index][0];
                                imageHeight = resolutions[index][1];
                                camFrameIndex = index + 1;
                                displayMessage("Resolution selected.\nPlease restart the camera stream!");
                                if (runningStream != null) {
                                    View v = null;
                                    stopTheCameraStreamClickEvent(v);
                                }
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();
                        showAdjustValuesUnitMenu(findViewById(R.id.settingsButton));
                        return true;
                    case R.id.frameinterval:
                        if (iuvc_descriptor == null) {
                            displayMessage("Start the Camera Stream first !");
                        }
                        int [] intervals;
                        if (videoformat.equals("MJPEG")) intervals = iuvc_descriptor.findDifferentFrameIntervals(true, new int [] {imageWidth, imageHeight});
                        else intervals = iuvc_descriptor.findDifferentFrameIntervals(false, new int [] {imageWidth, imageHeight});
                        builder = new CFAlertDialog.Builder(StartIsoStreamActivityUsbIso.this);
                        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
                        builder.setTitle("Select your FrameInterval");
                        builder.setMessage("Current Interval: " + (10000000 / camFrameInterval) + " Frames per Second");
                        String [] intervalString = new String [intervals.length];
                        for (int a = 0; a < intervalString.length; a++) intervalString[a] = Integer.toString((10000000 / intervals[a])) + "   FPS";
                        builder.setItems(intervalString , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int index) {
                                log("intervals[index] = " + intervals[index]);
                                camFrameInterval = intervals[index];
                                displayMessage("FrameInterval selected.\nPlease restart the camera stream!");
                                if (runningStream != null) {
                                    View v = null;
                                    stopTheCameraStreamClickEvent(v);
                                }
                                dialogInterface.dismiss();
                            }
                        });
                        builder.show();
                        return true;
                }
                return false;
            }
        });
        popup.show();//showing popup menu
    }

    public void showAdjustValuesUnitMenu(View v) {
        //Context wrapper = new ContextThemeWrapper(this, R.style.YOURSTYLE);
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.iso_stream_adjust_values_unit);
        LockCameraVariables lockVariables = new LockCameraVariables(bNumControlTerminal, bNumControlUnit);
        lockVariables.initUnit() ;
        if(lockVariables.Brightness == true)  popup.getMenu().findItem(R.id.brightness).setVisible(true);
        else  popup.getMenu().findItem(R.id.brightness).setVisible(false);
        if(lockVariables.Contrast == true)  popup.getMenu().findItem(R.id.contrast).setVisible(true);
        else  popup.getMenu().findItem(R.id.contrast).setVisible(false);
        if(lockVariables.Hue == true)  popup.getMenu().findItem(R.id.hue).setVisible(true);
        else  popup.getMenu().findItem(R.id.hue).setVisible(false);
        if(lockVariables.Saturation == true)  popup.getMenu().findItem(R.id.saturation).setVisible(true);
        else  popup.getMenu().findItem(R.id.saturation).setVisible(false);
        if(lockVariables.Sharpness == true)  popup.getMenu().findItem(R.id.sharpness).setVisible(true);
        else  popup.getMenu().findItem(R.id.sharpness).setVisible(false);
        if(lockVariables.Gamma == true)  popup.getMenu().findItem(R.id.gamma).setVisible(true);
        else  popup.getMenu().findItem(R.id.gamma).setVisible(false);
        if(lockVariables.White_Balance_Temperature == true)  popup.getMenu().findItem(R.id.white_balance_temperature).setVisible(true);
        else  popup.getMenu().findItem(R.id.white_balance_temperature).setVisible(false);
        if(lockVariables.White_Balance_Component == true)  popup.getMenu().findItem(R.id.white_balance_component).setVisible(true);
        else  popup.getMenu().findItem(R.id.white_balance_component).setVisible(false);
        if(lockVariables.Backlight_Compensation == true)  popup.getMenu().findItem(R.id.backlight_compensation).setVisible(true);
        else  popup.getMenu().findItem(R.id.backlight_compensation).setVisible(false);
        if(lockVariables.Gain == true)  popup.getMenu().findItem(R.id.gain).setVisible(true);
        else  popup.getMenu().findItem(R.id.gain).setVisible(false);
        if(lockVariables.Power_Line_Frequency == true)  popup.getMenu().findItem(R.id.power_line_frequency).setVisible(true);
        else  popup.getMenu().findItem(R.id.power_line_frequency).setVisible(false);
        if(lockVariables.Hue_Auto == true)  popup.getMenu().findItem(R.id.hue_auto).setVisible(true);
        else  popup.getMenu().findItem(R.id.hue_auto).setVisible(false);
        if(lockVariables.White_Balance_Temperature_Auto == true)  popup.getMenu().findItem(R.id.white_balance_temperature_auto).setVisible(true);
        else  popup.getMenu().findItem(R.id.white_balance_temperature_auto).setVisible(false);
        if(lockVariables.White_Balance_Component_Auto == true)  popup.getMenu().findItem(R.id.white_balance_component_auto).setVisible(true);
        else  popup.getMenu().findItem(R.id.white_balance_component_auto).setVisible(false);
        if(lockVariables.Digital_Multiplier == true)  popup.getMenu().findItem(R.id.digital_multiplier).setVisible(true);
        else  popup.getMenu().findItem(R.id.digital_multiplier).setVisible(false);
        if(lockVariables.Digital_Multiplier_Limit == true)  popup.getMenu().findItem(R.id.digital_multiplier_limit).setVisible(true);
        else  popup.getMenu().findItem(R.id.digital_multiplier_limit).setVisible(false);
        if(lockVariables.Analog_Video_Standard == true)  popup.getMenu().findItem(R.id.analog_video_standard).setVisible(true);
        else  popup.getMenu().findItem(R.id.analog_video_standard).setVisible(false);
        if(lockVariables.Analog_Video_Lock_Status == true)  popup.getMenu().findItem(R.id.analog_video_lock_status).setVisible(true);
        else  popup.getMenu().findItem(R.id.analog_video_lock_status).setVisible(false);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (camDevice == null || runningStream == null) return false;
                switch (item.getItemId()) {
                    case R.id.brightness:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        simpleSeekBar.setEnabled(false); simpleSeekBar.setAlpha(0); simpleSeekBar = null;
                                        defaultButton.setEnabled(false); defaultButton.setAlpha(0); defaultButton = null;
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;
                                SetCameraVariables setBright = new SetCameraVariables(camDeviceConnection, SetCameraVariables.CameraFunction.brightness,
                                        false, bUnitID, bTerminalID);
                                start = setBright.minValue ;
                                end = setBright.maxValue;
                                start_pos = setBright.currentValue;
                                start_position=(int) (((start_pos-start)/(end-start))*100);
                                discrete=start_pos;

                                simpleSeekBar = (SeekBar) findViewById(R.id.simpleSeekBar); simpleSeekBar.setEnabled(true); simpleSeekBar.setAlpha(1);
                                simpleSeekBar.setProgress(start_position);
                                simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    int progressChangedValue = 0;
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        float temp=progress;
                                        float dis=end-start;
                                        discrete=(start+((temp/100)*dis));
                                    }
                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                        // TODO Auto-generated method stub
                                    }
                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        log("setBright.currentValue = " + setBright.currentValue);
                                        setBright.currentValue = Math.round(discrete);
                                        log("setBright.currentValue = " + setBright.currentValue);
                                        log("");
                                        setBright.adjustValue(SetCameraVariables.CameraFunctionSetting.adjust);
                                    }
                                });
                                defaultButton = (Button) findViewById(R.id.defaultButton); defaultButton.setEnabled(true); defaultButton.setAlpha(1);
                                defaultButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        setBright.adjustValue(SetCameraVariables.CameraFunctionSetting.defaultAdjust);
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        start_pos = setBright.currentValue;
                                        start_position=(int) (((start_pos-start)/(end-start))*100);
                                        discrete=start_pos;
                                        simpleSeekBar.setProgress(start_position);
                                    }
                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.contrast:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        simpleSeekBar.setEnabled(false); simpleSeekBar.setAlpha(0); simpleSeekBar = null;
                                        defaultButton.setEnabled(false); defaultButton.setAlpha(0); defaultButton = null;
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;
                                SetCameraVariables setValue = new SetCameraVariables(camDeviceConnection, SetCameraVariables.CameraFunction.contrast,
                                        false, bUnitID, bTerminalID);
                                start = setValue.minValue ;
                                end = setValue.maxValue;
                                start_pos = setValue.currentValue;
                                start_position=(int) (((start_pos-start)/(end-start))*100);
                                discrete=start_pos;
                                simpleSeekBar = (SeekBar) findViewById(R.id.simpleSeekBar); simpleSeekBar.setEnabled(true); simpleSeekBar.setAlpha(1);
                                simpleSeekBar.setProgress(start_position);
                                simpleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                        float temp=progress;
                                        float dis=end-start;
                                        discrete=(start+((temp/100)*dis));
                                    }
                                    public void onStartTrackingTouch(SeekBar seekBar) {
                                        // TODO Auto-generated method stub
                                    }
                                    public void onStopTrackingTouch(SeekBar seekBar) {
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        setValue.currentValue = Math.round(discrete);
                                        setValue.adjustValue(SetCameraVariables.CameraFunctionSetting.adjust);
                                    }
                                });
                                defaultButton = (Button) findViewById(R.id.defaultButton); defaultButton.setEnabled(true); defaultButton.setAlpha(1);
                                defaultButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        setValue.adjustValue(SetCameraVariables.CameraFunctionSetting.defaultAdjust);
                                        myHandler.removeCallbacks(myRunnable);
                                        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                                        start_pos = setValue.currentValue;
                                        start_position=(int) (((start_pos-start)/(end-start))*100);
                                        discrete=start_pos;
                                        simpleSeekBar.setProgress(start_position);
                                    }
                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.hue:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.saturation:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.sharpness:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.gamma:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.white_balance_temperature:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.white_balance_component:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.backlight_compensation:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.gain:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.power_line_frequency:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.hue_auto:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.white_balance_temperature_auto:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.white_balance_component_auto:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.digital_multiplier:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.digital_multiplier_limit:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.analog_video_standard:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.analog_video_lock_status:
                        displayMessage("Not supported up to now...");
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    public void showAdjustValuesTerminalMenu(View v) {
        //Context wrapper = new ContextThemeWrapper(this, R.style.YOURSTYLE);
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.iso_stream_adjust_values_terminal);
        LockCameraVariables lockVariables = new LockCameraVariables(bNumControlTerminal, bNumControlUnit);
        lockVariables.initTerminal();
        if(lockVariables.Scanning_Mode == true)  popup.getMenu().findItem(R.id.scanning_Mode).setVisible(true);
        else  popup.getMenu().findItem(R.id.scanning_Mode).setVisible(false);
        if(lockVariables.Auto_Exposure_Mode == true)  popup.getMenu().findItem(R.id.auto_exposure_mode).setVisible(true);
        else  popup.getMenu().findItem(R.id.auto_exposure_mode).setVisible(false);
        if(lockVariables.Auto_Exposure_Priority == true)  popup.getMenu().findItem(R.id.Auto_Exposure_Priority).setVisible(true);
        else  popup.getMenu().findItem(R.id.Auto_Exposure_Priority).setVisible(false);
        if(lockVariables.Exposure_Time_Absolute == true)  popup.getMenu().findItem(R.id.Exposure_Time_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.Exposure_Time_Absolute).setVisible(false);
        if(lockVariables.Exposure_Time_Relative == true)  popup.getMenu().findItem(R.id.Exposure_Time_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.Exposure_Time_Relative).setVisible(false);
        if(lockVariables.Focus_Absolute == true)  popup.getMenu().findItem(R.id.Focus_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.Focus_Absolute).setVisible(false);
        if(lockVariables.Focus_Relative == true)  popup.getMenu().findItem(R.id.Focus_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.Focus_Relative).setVisible(false);
        if(lockVariables.Iris_Absolute == true)  popup.getMenu().findItem(R.id.Iris_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.Iris_Absolute).setVisible(false);
        if(lockVariables.Iris_Relative == true)  popup.getMenu().findItem(R.id.Iris_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.Iris_Relative).setVisible(false);
        if(lockVariables.Zoom_Absolute == true)  popup.getMenu().findItem(R.id.Zoom_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.Zoom_Absolute).setVisible(false);
        if(lockVariables.Zoom_Relative == true)  popup.getMenu().findItem(R.id.Zoom_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.Zoom_Relative).setVisible(false);
        if(lockVariables.PanTilt_Absolute == true)  popup.getMenu().findItem(R.id.PanTilt_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.PanTilt_Absolute).setVisible(false);
        if(lockVariables.PanTilt_Relative == true)  popup.getMenu().findItem(R.id.PanTilt_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.PanTilt_Relative).setVisible(false);
        if(lockVariables.Roll_Absolute == true)  popup.getMenu().findItem(R.id.Roll_Absolute).setVisible(true);
        else  popup.getMenu().findItem(R.id.Roll_Absolute).setVisible(false);
        if(lockVariables.Roll_Relative == true)  popup.getMenu().findItem(R.id.Roll_Relative).setVisible(true);
        else  popup.getMenu().findItem(R.id.Roll_Relative).setVisible(false);
        if(lockVariables.Reserved_one == true)  popup.getMenu().findItem(R.id.Reserved1).setVisible(true);
        else  popup.getMenu().findItem(R.id.Reserved1).setVisible(false);
        if(lockVariables.Reserved_two == true)  popup.getMenu().findItem(R.id.Reserved2).setVisible(true);
        else  popup.getMenu().findItem(R.id.Reserved2).setVisible(false);
        if(lockVariables.Focus_Auto == true)  popup.getMenu().findItem(R.id.focusAuto).setVisible(true);
        else  popup.getMenu().findItem(R.id.focusAuto).setVisible(false);
        if(lockVariables.Privacy == true)  popup.getMenu().findItem(R.id.Privacy).setVisible(true);
        else  popup.getMenu().findItem(R.id.Privacy).setVisible(false);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (runningStream == null) return false;
                switch (item.getItemId()) {
                    case R.id.scanning_Mode:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.auto_exposure_mode:
                        if (camDevice == null) return false;
                        //exposureAutoState = true;
                        displayMessage("Auto_Exposure_Mode should not be disabled. ..");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        switchAuto.setEnabled(false);
                                        switchAuto.setVisibility(View.GONE);
                                        switchAuto = null;
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;
                                SetCameraVariables setAutoExposure = new SetCameraVariables(camDeviceConnection, SetCameraVariables.CameraFunction.auto_exposure_mode,
                                        exposureAutoState, bUnitID, bTerminalID);
                                exposureAutoState = setAutoExposure.autoEnabled;
                                switchAuto = (Switch) findViewById(R.id.switchAuto);
                                switchAuto.setEnabled(true);
                                switchAuto.setVisibility(View.VISIBLE);
                                if (exposureAutoState) switchAuto.setChecked(true);
                                else switchAuto.setChecked(false);

                                switchAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                                        Log.v("Switch State=", ""+isChecked);
                                        if (isChecked) {
                                            exposureAutoState = true;
                                            setAutoExposure.autoEnabled = true;
                                            setAutoExposure.adjustValue(SetCameraVariables.CameraFunctionSetting.auto);
                                        } else {
                                            exposureAutoState = false;
                                            setAutoExposure.autoEnabled = false;
                                            setAutoExposure.adjustValue(SetCameraVariables.CameraFunctionSetting.auto);
                                        }
                                    }
                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.Auto_Exposure_Priority:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Exposure_Time_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Exposure_Time_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Focus_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Focus_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Iris_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Iris_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Zoom_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Zoom_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.PanTilt_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.PanTilt_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Roll_Absolute:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Roll_Relative:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Reserved1:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.Reserved2:
                        displayMessage("Not supported up to now...");
                        return true;
                    case R.id.focusAuto:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        switchAuto.setEnabled(false);
                                        switchAuto.setVisibility(View.GONE);
                                        switchAuto = null;
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;
                                SetCameraVariables setFocus = new SetCameraVariables(camDeviceConnection, SetCameraVariables.CameraFunction.autofocus, focusAutoState,
                                        bUnitID, bTerminalID);
                                focusAutoState = setFocus.autoEnabled;
                                switchAuto = (Switch) findViewById(R.id.switchAuto);
                                switchAuto.setEnabled(true);
                                switchAuto.setVisibility(View.VISIBLE);
                                if (focusAutoState) switchAuto.setChecked(true);
                                else switchAuto.setChecked(false);

                                switchAuto.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                                    @Override
                                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                                        Log.v("Switch State=", ""+isChecked);
                                        if (isChecked) {
                                            focusAutoState = true;
                                            setFocus.autoEnabled = true;
                                            setFocus.adjustValue(SetCameraVariables.CameraFunctionSetting.auto);
                                        } else {
                                            focusAutoState = false;
                                            setFocus.autoEnabled = false;
                                            setFocus.adjustValue(SetCameraVariables.CameraFunctionSetting.auto);
                                        }
                                    }

                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.Privacy:
                        displayMessage("Not supported up to now...");
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    private void startWebRTC() {
        if (camDevice == null) {
            try {
                findCam();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!usbManager.hasPermission(camDevice)) {
            usbManager.requestPermission(camDevice, mPermissionIntent);
            return;
        }
        if (runningStream != null) stopTheCameraStreamClickEvent(null);
        Intent intent = new Intent(this, WebRtc_MainActivity.class);
        Bundle bundle=new Bundle();
        bundle.putBoolean("edit", true);
        bundle.putInt("camStreamingAltSetting",camStreamingAltSetting);
        bundle.putString("videoformat",videoformat);
        bundle.putInt("camFormatIndex",camFormatIndex);
        bundle.putInt("imageWidth",imageWidth);
        bundle.putInt("imageHeight",imageHeight);
        bundle.putInt("camFrameIndex",camFrameIndex);
        bundle.putInt("camFrameInterval",camFrameInterval);
        bundle.putInt("packetsPerRequest",packetsPerRequest);
        bundle.putInt("maxPacketSize",maxPacketSize);
        bundle.putInt("activeUrbs",activeUrbs);
        bundle.putByte("bUnitID",bUnitID);
        bundle.putByte("bTerminalID",bTerminalID);
        bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
        bundle.putByteArray("bNumControlUnit", bNumControlUnit);
        bundle.putByteArray("bcdUVC", bcdUVC);
        bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
        bundle.putBoolean("LIBUSB", LIBUSB);
        bundle.putBoolean("moveToNative", moveToNative);
        bundle.putBoolean("bulkMode", bulkMode);
        intent.putExtra("bun",bundle);
        startActivity(intent);
        StartIsoStreamActivityUsbIso.this.finish();
    }

    public void returnToConfigScreen() {
        stopKamera = true;
        runningStream = null;
        //imageView = (ImageView) findViewById(R.id.imageView);
        onBackPressed();
    }

    public void beenden(boolean exit) {
        if (LIBUSB) {
            if(libusb_is_initialized) {
                JNA_I_LibUsb.INSTANCE.stopStreaming();
            }
            //I_LibUsb.INSTANCE.closeLibUsb();
            //I_LibUsb.INSTANCE.exit();
            camDevice = null;
            Intent resultIntent = new Intent();
            if (exit == true) resultIntent.putExtra("closeProgram", true);
            setResult(Activity.RESULT_OK, resultIntent);
            //mService.streamCanBeResumed = false;
            finish();
        } else {
            if (camIsOpen) {
                closeCameraDevice();
            }
            else if (camDeviceConnection != null) {
                camDeviceConnection.releaseInterface(camControlInterface);
                camDeviceConnection.releaseInterface(camStreamingInterface);
                camDeviceConnection.close();
            }
            Intent resultIntent = new Intent();
            if (exit == true) resultIntent.putExtra("closeProgram", true);
            setResult(Activity.RESULT_OK, resultIntent);
            //mService.streamCanBeResumed = false;
            finish();
        }
    }

    // Start the Stream
    public void isoStream(MenuItem Item) {
        if (camDevice == null) {
            if(moveToNative) {
                try {
                    findCam();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            displayMessage("No Camera connected\nPlease connect a camera");
            return;
        }
        else {
            ((Button) findViewById(R.id.startStream)).setEnabled(false);
            stopStreamButton.getBackground().setAlpha(180);  // 25% transparent
            stopStreamButton.setEnabled(true);
            startStream.getBackground().setAlpha(20);  // 95% transparent
            photoButton.setEnabled(true);
            photoButton.setBackgroundResource(R.drawable.bg_button_bildaufnahme);
            videoButton.setEnabled(true);
            videoButton.setAlpha(1); // 100% transparent
            stopKamera = false;

            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            initStillImageParms();
            if (camIsOpen) {
                if (runningStream != null) {
                    return;
                }
                runningStream = new StartIsoStreamActivityUsbIso.IsochronousStream(this);
                runningStream.start();
                byte[] a = camDeviceConnection.getRawDescriptors();
                ByteBuffer uvcData = ByteBuffer.wrap(a);
                UVC_Descriptor uvc_descriptor = new UVC_Descriptor(uvcData);
                if (uvc_descriptor.phraseUvcData() == 0) {
                    iuvc_descriptor = new UVC_Initializer(uvc_descriptor);
                    log("videoformat = " + videoformat);
                    if (videoformat.equals("MJPEG") ) {
                        log ("Arrays.deepToString(iuvc_descriptor.findDifferentResolutions(false)) = " + Arrays.deepToString(iuvc_descriptor.findDifferentResolutions(true)));
                        log ("");
                        log ("iuvc_descriptor.findDifferentFrameIntervals( = " + Arrays.toString(iuvc_descriptor.findDifferentFrameIntervals(true, new int[] {imageWidth, imageHeight})));
                        log ("");
                    } else {
                        log ("Arrays.deepToString(iuvc_descriptor.findDifferentResolutions(false)) = " + Arrays.deepToString(iuvc_descriptor.findDifferentResolutions(false)));
                        log ("");
                        log ("iuvc_descriptor.findDifferentFrameIntervals( = " + Arrays.toString(iuvc_descriptor.findDifferentFrameIntervals(false, new int[] {imageWidth, imageHeight})));
                        log ("");
                    }
                } else displayMessage("Interface initialization for the Descriptor failed.");
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (msg != null) displayMessage(msg);
                        else displayMessage("Failed to start the Camera Stream");
                        msg = null;
                    }
                });
            }
        }

    }

    private void findCam() throws Exception {
        camDevice = findCameraDevice();
        if (camDevice == null) {
            camDevice = checkDeviceVideoClass();
            if (camDevice == null)  throw new Exception("No USB camera device found.");
        }
        if (!usbManager.hasPermission(camDevice)) {
            log("Asking for Permissions");
            usbManager.requestPermission(camDevice, mPermissionIntent);
        } else usbManager.requestPermission (camDevice, mPermissionIntent);
    }

    private UsbDevice checkDeviceVideoClass() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (usbDevice.getDeviceClass() == 14 && usbDevice.getDeviceSubclass() == 2) {
                moveToNative = true;
                return usbDevice;
            } else if (usbDevice.getDeviceClass() == 239 && usbDevice.getDeviceSubclass() == 2) {
                moveToNative = true;
                return usbDevice;
            }
            if (checkDeviceHasVideoControlInterface(usbDevice)) {
                return usbDevice;
            }
        }
        return null;
    }

    private UsbDevice findCameraDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            if(moveToNative) {
                if (checkDeviceHasVideoControlInterface(usbDevice)) {
                    return usbDevice;
                }
            } else {
                log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
                if (checkDeviceHasVideoStreamingInterface(usbDevice)) {
                    return usbDevice;
                }
            }
        }
        return null;
    }

    private boolean checkDeviceHasVideoStreamingInterface(UsbDevice usbDevice) {
        return getVideoStreamingInterface(usbDevice) != null;
    }

    private boolean checkDeviceHasVideoControlInterface(UsbDevice usbDevice) {
        return getVideoControlInterface(usbDevice) != null;
    }

    private UsbInterface getVideoControlInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOCONTROL, false);
    }

    private UsbInterface getVideoStreamingInterface(UsbDevice usbDevice) {
        return findInterface(usbDevice, UsbConstants.USB_CLASS_VIDEO, SC_VIDEOSTREAMING, true);
    }

    private UsbInterface findInterface(UsbDevice usbDevice, int interfaceClass, int interfaceSubclass, boolean withEndpoint) {
        int interfaces = usbDevice.getInterfaceCount();
        for (int i = 0; i < interfaces; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            if (usbInterface.getInterfaceClass() == interfaceClass && usbInterface.getInterfaceSubclass() == interfaceSubclass && (!withEndpoint || usbInterface.getEndpointCount() > 0)) {
                return usbInterface;
            }
        }
        return null;
    }

    private void openCam(boolean init) throws Exception {
        openCameraDevice(init);
        if (init) {
            initCamera();
            camIsOpen = true;
        }
        log("Camera opened sucessfully");
    }

    private void openCameraDevice(boolean init) throws Exception {
        if (moveToNative) {
            camDeviceConnection = usbManager.openDevice(camDevice);
            int FD = camDeviceConnection.getFileDescriptor();
            if(camStreamingEndpointAdress == 0) {
                camStreamingEndpointAdress = JNA_I_LibUsb.INSTANCE.fetchTheCamStreamingEndpointAdress(camDeviceConnection.getFileDescriptor());
                //mService.libusb_wrapped = true;
                //mService.libusb_InterfacesClaimed = true;
            }
            int bcdUVC_int = 0;
            if(mUsbFs==null) mUsbFs =  getUSBFSName(camDevice);
            bcdUVC_int = ((bcdUVC[1] & 0xFF) << 8) | (bcdUVC[0] & 0xFF);
            int lowAndroid = 0;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                lowAndroid = 1;
            }
            JNA_I_LibUsb.INSTANCE.set_the_native_Values(fd, packetsPerRequest, maxPacketSize, activeUrbs, camStreamingAltSetting, camFormatIndex,
                    camFrameIndex,  camFrameInterval,  imageWidth,  imageHeight, camStreamingEndpointAdress, 1, videoformat, 0, bcdUVC_int, lowAndroid);
            //mService.native_values_set=true;
            JNA_I_LibUsb.INSTANCE.initStreamingParms(FD);
        } else {
            // (For transfer buffer sizes > 196608 the kernel file drivers/usb/core/devio.c must be patched.)
            camControlInterface = getVideoControlInterface(camDevice);
            camStreamingInterface = getVideoStreamingInterface(camDevice);
            if (camStreamingInterface.getEndpointCount() < 1) {
                throw new Exception("Streaming interface has no endpoint.");
            }
            camStreamingEndpoint = camStreamingInterface.getEndpoint(0);
            bulkMode = camStreamingEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK;
            camDeviceConnection = usbManager.openDevice(camDevice);
            if (camDeviceConnection == null) {
                log("Failed to open the device");
                throw new Exception("Unable to open camera device connection.");
            }
            if (!LIBUSB) {
                if (!camDeviceConnection.claimInterface(camControlInterface, true)) {
                    log("Failed to claim camControlInterface");
                    throw new Exception("Unable to claim camera control interface.");
                }
                if (!camDeviceConnection.claimInterface(camStreamingInterface, true)) {
                    log("Failed to claim camStreamingInterface");
                    throw new Exception("Unable to claim camera streaming interface.");
                }
            }
        }
    }

    public void closeCameraDevice() {

        if (moveToNative) {
            camDeviceConnection = null;
        }
        else if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
            camDeviceConnection = null;
        }
        runningStream = null;
    }

    private void initCamera() throws Exception {
        try {
            getVideoControlErrorCode();  // to reset previous error states
        } catch (Exception e) {
            log("Warning: getVideoControlErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        try {
            enableStreaming(false);
        } catch (Exception e) {
            //displayErrorMessage(e);
            displayMessage("Warning: enable the Stream failed:\nPlease unplug and replug the camera, or reboot the device");
            log("Warning: enableStreaming(false) failed: " + e);
        }
        try {
            // to reset previous error states
            getVideoStreamErrorCode();
        } catch (Exception e) {
            log("Warning: getVideoStreamErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        initStreamingParms();
        //initBrightnessParms();
    }

    private void BildaufnahmeButtonClickEvent() {
        imageCapture = true;
        if (LIBUSB) JNA_I_LibUsb.INSTANCE.setImageCapture();
        displayMessage("Image saved");
    }

    //////////////////// Buttons  ///////////////

    public void stopTheCameraStreamClickEvent(View view) {
        startStream.getBackground().setAlpha(180);  // 25% transparent
        startStream.setEnabled(true);
        stopStreamButton.getBackground().setAlpha(20);  // 100% transparent
        stopStreamButton.setEnabled(false);
        photoButton.setEnabled(false);
        photoButton.setBackgroundResource(R.drawable.photo_clear);
        videoButton.setEnabled(false);
        videoButton.setAlpha(0); // 100% transparent
        stopKamera = true;
        if (LIBUSB) {
            JNA_I_LibUsb.INSTANCE.stopStreaming();
            //mService.streamOnPause = true;
            //mService.streamCanBeResumed = true;
        } else {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                enableStreaming(false);
            } catch (Exception e) {
                msg = "Plz unplug the camera and replug again, or reboot the device";
                displayMessage("Plz unplug the camera and replug again, or reboot the device");
                e.printStackTrace();
            }
            displayMessage("Stopped ");
            log("Stopped");
            runningStream = null;
        }
    }

    private int flipToInt (boolean value) {
        if (value) return 1;
        else return 0;
    }

    public void flipLeft (View view) {
        if (rotate == 0) rotate = 270;
        else rotate -= 90;
        if (LIBUSB) JNA_I_LibUsb.INSTANCE.setRotation(rotate, flipToInt(horizontalFlip), flipToInt(verticalFlip));
    }

    public void flipRight (View view) {
        if (rotate == 270) rotate = 0;
        else rotate += 90;
        if (LIBUSB) JNA_I_LibUsb.INSTANCE.setRotation(rotate, flipToInt(horizontalFlip), flipToInt(verticalFlip));
    }

    public void flipHorizontal (View view) {
        if (horizontalFlip == false) horizontalFlip = true;
        else horizontalFlip = false;
        if (LIBUSB) JNA_I_LibUsb.INSTANCE.setRotation(rotate, flipToInt(horizontalFlip), flipToInt(verticalFlip));
    }

    public void flipVertical (View view) {
        if (verticalFlip == false) verticalFlip = true;
        else verticalFlip = false;
        if (LIBUSB) JNA_I_LibUsb.INSTANCE.setRotation(rotate, flipToInt(horizontalFlip), flipToInt(verticalFlip));
    }

    private void writeBytesToFile(String fileName, byte[] data) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.write(data);
            fileOutputStream.flush();
        } finally {
            fileOutputStream.close();
        }
    }

    private void processReceivedVideoFrameYuv(byte[] frameData, Videoformat videoFromat) throws IOException {
        if (videoformat == null) {
            if (videoformat.equals("YUV")){
                videoFromat = Videoformat.YUV;
            } else if (videoformat.equals("YUY2")){
                videoFromat = Videoformat.YUY2;
            } else if (videoformat.equals("YUY2")){
                videoFromat = Videoformat.YV12;
            } else if (videoformat.equals("YUV_420_888")){
                videoFromat = Videoformat.YUV_420_888;
            } else if (videoformat.equals("YUV_422_888")){
                videoFromat = Videoformat.YUV_422_888;
            } else if (videoformat.equals("UYVY")){
                videoFromat = Videoformat.UYVY;
            }
        }
        Bitmap bitmap = null;
        if (videoFromat == Videoformat.UYVY) {
            // JNI APPROACH
            bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            //UYVYpixeltobmp(frameData, bitmap, imageWidth, imageHeight);
            // JNA APPROACH:
            /*
            Pointer ptr = new Memory(frameData.length);
            ptr.write(0, frameData, 0, frameData.length);

            //Pointer jpegLength = new Memory(4);

            IntByReference jpgLength = new IntByReference();
            jpgLength.setValue(-1);

            log("calling convertUYVYtoJPEG java");
            Pointer p = JNA_I_LibUsb.INSTANCE.convertUYVYtoJPEG(ptr, jpgLength, frameData.length, imageWidth, imageHeight);

            int length = 0;
            if (jpgLength != null) {
                length = jpgLength.getValue();
                log ("jpgLength = " + length);
            } else {
                log ("jpgLength == null\nSKIP!!!");
                return;
            }
            if (p != null) {
                log("data received");
                jpegByteArray = p.getByteArray(0, length);
            } else log("no Data received == NULL");

             */
        } else if (videoFromat == Videoformat.YUY2) {
            bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
            //YUY2pixeltobmp(frameData, bitmap, imageWidth, imageHeight);
        } else {
            YuvImage yuvImage = null;
            if (videoFromat == Videoformat.YUY2) yuvImage = new YuvImage(frameData, ImageFormat.YUY2, imageWidth, imageHeight, null);
            else if (videoFromat == Videoformat.YV12) yuvImage = new YuvImage(frameData, ImageFormat.YV12, imageWidth, imageHeight, null);
            else if (videoFromat == Videoformat.YUV_420_888) yuvImage = new YuvImage(frameData, ImageFormat.YUV_420_888, imageWidth, imageHeight, null);
            else if (videoFromat == Videoformat.YUV_422_888) yuvImage = new YuvImage(frameData, ImageFormat.YUV_422_888, imageWidth, imageHeight, null);
            else if (videoFromat == Videoformat.NV21) yuvImage = new YuvImage(frameData, ImageFormat.NV21, imageWidth, imageHeight, null);
            else if (videoFromat == Videoformat.UYVY) {          }
            else yuvImage = new YuvImage(frameData, ImageFormat.YUY2, imageWidth, imageHeight, null);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, os);
            byte[] jpegByteArray;
            jpegByteArray = os.toByteArray();
            bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        }

        if (imageCapture) {
            imageCapture = false;
            date = new Date() ;
            dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = new File(rootPath + dateFormat.format(date) + ".jpg").getAbsolutePath() ;
                try {
                    byte[] jpegByteArray;
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    jpegByteArray = os.toByteArray();
                    writeBytesToFile(fileName, jpegByteArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log ("file saved");
            } else {
                Context context = getApplicationContext();
                String fileName = new File(  dateFormat.format(date) + ".JPG").getPath() ;
                log("fileName = " + fileName);
                try {
                    //final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, "Usb_Camera_Picture");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log ("file saved");
            }
            /*
            imageCapture = false ;
            date = new Date() ;
            dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;
            Context context = getApplicationContext();
            String dirname = "Pictures";
            File directory = context.getFilesDir();
            File dir = new File(directory, dirname);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = new File(dir,  dateFormat.format(date) + ".JPG").getPath() ;
            log("fileName = " + fileName);
            writeBytesToFile(fileName, jpegByteArray);
            log ("file saved");
             */
        }
        if (saveStillImage) {

            saveStillImage = false;
            date = new Date() ;
            dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = new File(rootPath + dateFormat.format(date) + ".jpg").getAbsolutePath() ;
                try {
                    byte[] jpegByteArray;
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    jpegByteArray = os.toByteArray();
                    writeBytesToFile(fileName, jpegByteArray);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log ("file saved");
            } else {
                Context context = getApplicationContext();
                String fileName = new File(  dateFormat.format(date) + ".JPG").getPath() ;
                log("fileName = " + fileName);
                try {
                    //final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, "Usb_Camera_Picture");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log ("file saved");
            }

        }
        if (videorecord) {
            if (System.currentTimeMillis() - currentTime > 200) {
                currentTime = System.currentTimeMillis();
                lastPicture ++;
                String dirname = "Video";
                File dir = new File(getExternalFilesDir(null),dirname);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File sub_dir=new File(dir, "rec");
                if (!sub_dir.exists()) {
                    sub_dir.mkdirs();
                }
                String fileName = new File(sub_dir,lastPicture + ".JPG").getPath() ;
                byte[] jpegByteArray;
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                jpegByteArray = os.toByteArray();
                writeBytesToFile(fileName, jpegByteArray);
            }
        }
        if (exit == false) {
            //final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
            final Bitmap bmp = bitmap;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (horizontalFlip || verticalFlip || rotate != 0) {
                        imageView.setImageBitmap(flipImage(bmp));
                    }
                    else imageView.setImageBitmap(bmp);
                }
            });
            if (videorecordApiJellyBeanNup) {
                bitmapToVideoEncoder.queueFrame(bitmap);
            }
        }
    }

    public Bitmap flipImage(Bitmap src) {
        // create new matrix for transformation
        Matrix matrix = new Matrix();
        if (horizontalFlip) matrix.preScale(1.0f, -1.0f);
        if (verticalFlip) matrix.preScale(-1.0f, 1.0f);
        if (rotate != 0) matrix.postRotate(rotate);
        // return transformed image
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    public void processReceivedMJpegVideoFrameKamera(byte[] mjpegFrameData) throws Exception {
        byte[] jpegFrameData = convertMjpegFrameToJpegKamera(mjpegFrameData);
        if (imageCapture) {
            imageCapture = false ;
            date = new Date() ;
            dateFormat = new SimpleDateFormat("dd.MM.yyyy___HH_mm_ss") ;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = new File(rootPath + dateFormat.format(date) + ".jpg").getAbsolutePath() ;
                try {
                    writeBytesToFile(fileName, jpegFrameData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log ("file saved");
            } else {
                Context context = getApplicationContext();
                String fileName = new File(  dateFormat.format(date) + ".JPG").getPath() ;
                log("fileName = " + fileName);
                try {
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length);
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, "Usb_Camera_Picture");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log ("file saved");
            }
        }
        if (saveStillImage) {
            saveStillImage = false;
            date = new Date();
            dateFormat = new SimpleDateFormat("\"dd.MM.yyyy___HH_mm_ss") ;

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = new File(rootPath + dateFormat.format(date) + ".jpg").getAbsolutePath() ;
                try {
                    writeBytesToFile(fileName, jpegFrameData);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log("Still Image Save Complete");
            } else {
                Context context = getApplicationContext();
                String fileName = new File(  dateFormat.format(date) + ".JPG").getPath() ;
                log("fileName = " + fileName);
                try {
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length);
                    MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, fileName, "Usb_Camera_Picture");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                log("Still Image Save Complete");
            }
        }
        if (videorecord) {
            if (System.currentTimeMillis() - currentTime > 200) {
                currentTime = System.currentTimeMillis();
                lastPicture ++;
                String dirname = "Video";
                File dir = new File(getExternalFilesDir(null),dirname);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File sub_dir=new File(dir, "rec");
                if (!sub_dir.exists()) {
                    sub_dir.mkdirs();
                }
                String fileName = new File(sub_dir,lastPicture + ".JPG").getPath() ;
                writeBytesToFile(fileName, jpegFrameData);
            }
        }
        if (exit == false) {
            if (lowerResolution) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 4;
                final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length, opts);
                //Bitmap bitmap = decodeSampledBitmapFromByteArray(jpegFrameData);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
                if (videorecordApiJellyBeanNup) {
                    bitmapToVideoEncoder.queueFrame(bitmap);
                }
            } else {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (horizontalFlip || verticalFlip || rotate != 0) {
                            imageView.setImageBitmap(flipImage(bitmap));
                        } else imageView.setImageBitmap(bitmap);
                    }
                });
                if (videorecordApiJellyBeanNup) {
                    bitmapToVideoEncoder.queueFrame(bitmap);
                }
            }
        }
    }

    // see USB video class standard, USB_Video_Payload_MJPEG_1.5.pdf
    private byte[] convertMjpegFrameToJpegKamera(byte[] frameData) throws Exception {
        int frameLen = frameData.length;
        while (frameLen > 0 && frameData[frameLen - 1] == 0) {
            frameLen--;
        }
        if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9) {
            logError("Invalid MJPEG frame structure, length= " + frameData.length);
        }
        boolean hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
        exit = false;
        if (hasHuffmanTable) {
            if (frameData.length == frameLen) {
                return frameData;
            }
            return Arrays.copyOf(frameData, frameLen);
        } else {
            int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);

            try {if (segmentDaPos == -1)   exit = true;
            } catch (Exception e) {
                logError("Segment 0xDA not found in MJPEG frame data.");}
            //          throw new Exception("Segment 0xDA not found in MJPEG frame data.");
            if (exit ==false) {
                byte[] a = new byte[frameLen + mjpgHuffmanTable.length];
                System.arraycopy(frameData, 0, a, 0, segmentDaPos);
                System.arraycopy(mjpgHuffmanTable, 0, a, segmentDaPos, mjpgHuffmanTable.length);
                System.arraycopy(frameData, segmentDaPos, a, segmentDaPos + mjpgHuffmanTable.length, frameLen - segmentDaPos);
                return a;
            } else
                return null;
        }
    }

    private int findJpegSegment(byte[] a, int dataLen, int segmentType) {
        int p = 2;
        while (p <= dataLen - 6) {
            if ((a[p] & 0xff) != 0xff) {
                log("Unexpected JPEG data structure (marker expected).");
                break;
            }
            int markerCode = a[p + 1] & 0xff;
            if (markerCode == segmentType) {
                return p;
            }
            if (markerCode >= 0xD0 && markerCode <= 0xDA) {       // stop when scan data begins
                break;
            }
            int len = ((a[p + 2] & 0xff) << 8) + (a[p + 3] & 0xff);
            p += len + 2;
        }
        return -1;
    }

    private void initStreamingParms() throws Exception {
        stringBuilder = new StringBuilder();
        final int timeout = 5000;
        int usedStreamingParmsLen;
        int len;
        byte[] streamingParms = new byte[26];
        // The e-com module produces errors with 48 bytes (UVC 1.5) instead of 26 bytes (UVC 1.1) streaming parameters! We could use the USB version info to determine the size of the streaming parameters.
        streamingParms[0] = (byte) 0x01;                // (0x01: dwFrameInterval) //D0: dwFrameInterval //D1: wKeyFrameRate // D2: wPFrameRate // D3: wCompQuality // D4: wCompWindowSize
        streamingParms[2] = (byte) camFormatIndex;                // bFormatIndex
        streamingParms[3] = (byte) camFrameIndex;                 // bFrameIndex
        packUsbInt(camFrameInterval, streamingParms, 4);         // dwFrameInterval
        log("Initial streaming parms: " + dumpStreamingParms(streamingParms));
        stringBuilder.append("Initial streaming parms: \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_PROBE_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms probe set failed, len=" + len + ".");
        }
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_PROBE_CONTROL << 8, camStreamingInterface.getId(), streamingParms, streamingParms.length, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms probe get failed.");
        }
        log("Probed streaming parms: " + dumpStreamingParms(streamingParms));
        stringBuilder.append("\nProbed streaming parms:  \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        usedStreamingParmsLen = len;
        // log("Streaming parms length: " + usedStreamingParmsLen);
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, usedStreamingParmsLen, timeout);
        if (len != streamingParms.length) {
            throw new Exception("Camera initialization failed. Streaming parms commit set failed.");
        }
        // for (int i = 0; i < streamingParms.length; i++) streamingParms[i] = 99;          // temp test
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_COMMIT_CONTROL << 8, camStreamingInterface.getId(), streamingParms, usedStreamingParmsLen, timeout);
        if (len != streamingParms.length) {
            log("Final streaming parms receive failed");
            //throw new Exception("Camera initialization failed. Streaming parms commit get failed.");
        }
        log("Final streaming parms: " + dumpStreamingParms(streamingParms));
        stringBuilder.append("\nFinal streaming parms: \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        controlltransfer = new String(dumpStreamingParms(streamingParms));
    }

    private String dumpStreamingParms(byte[] p) {
        StringBuilder s = new StringBuilder(128);
        s.append("hint=0x" + Integer.toHexString(unpackUsbUInt2(p, 0)));
        s.append(" format=" + (p[2] & 0xf));
        s.append(" frame=" + (p[3] & 0xf));
        s.append(" frameInterval=" + unpackUsbInt(p, 4));
        s.append(" keyFrameRate=" + unpackUsbUInt2(p, 8));
        s.append(" pFrameRate=" + unpackUsbUInt2(p, 10));
        s.append(" compQuality=" + unpackUsbUInt2(p, 12));
        s.append(" compWindowSize=" + unpackUsbUInt2(p, 14));
        s.append(" delay=" + unpackUsbUInt2(p, 16));
        s.append(" maxVideoFrameSize=" + unpackUsbInt(p, 18));
        s.append(" maxPayloadTransferSize=" + unpackUsbInt(p, 22));
        return s.toString();
    }

    private void initStillImageParms() {
        final int timeout = 5000;
        int len;
        byte[] parms = new byte[11];
        if (moveToNative) return;
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_MIN): " + dumpStillImageParms(parms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_MAX): " + dumpStillImageParms(parms));
        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        //parms[2] = 1;

        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms (GET_CUR): " + dumpStillImageParms(parms));
        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms commit set failed.");
        }
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            log("Camera initialization failed. Still image parms commit get failed. len=" + len); }
        log("Final still image parms: " + dumpStillImageParms(parms));
    }

    private String dumpStillImageParms(byte[] p) {
        StringBuilder s = new StringBuilder(128);
        s.append("bFormatIndex=" + (p[0] & 0xff));
        s.append(" bFrameIndex=" + (p[1] & 0xff));
        s.append(" bCompressionIndex=" + (p[2] & 0xff));
        s.append(" maxVideoFrameSize=" + unpackUsbInt(p, 3));
        s.append(" maxPayloadTransferSize=" + unpackUsbInt(p, 7));
        return s.toString();
    }

    private static int unpackUsbInt(byte[] buf, int pos) {
        return unpackInt(buf, pos, false);
    }

    private static int unpackUsbUInt2(byte[] buf, int pos) {
        return ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
    }

    private static void packUsbInt(int i, byte[] buf, int pos) {
        packInt(i, buf, pos, false);
    }

    private static void packInt(int i, byte[] buf, int pos, boolean bigEndian) {
        if (bigEndian) {
            buf[pos] = (byte) ((i >>> 24) & 0xFF);
            buf[pos + 1] = (byte) ((i >>> 16) & 0xFF);
            buf[pos + 2] = (byte) ((i >>> 8) & 0xFF);
            buf[pos + 3] = (byte) (i & 0xFF);
        } else {
            buf[pos] = (byte) (i & 0xFF);
            buf[pos + 1] = (byte) ((i >>> 8) & 0xFF);
            buf[pos + 2] = (byte) ((i >>> 16) & 0xFF);
            buf[pos + 3] = (byte) ((i >>> 24) & 0xFF);
        }
    }

    private static int unpackInt(byte[] buf, int pos, boolean bigEndian) {
        if (bigEndian) {
            return (buf[pos] << 24) | ((buf[pos + 1] & 0xFF) << 16) | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
        } else {
            return (buf[pos + 3] << 24) | ((buf[pos + 2] & 0xFF) << 16) | ((buf[pos + 1] & 0xFF) << 8) | (buf[pos] & 0xFF);
        }
    }

    private void enableStreaming(boolean enabled) throws Exception {
        enableStreaming_usbFs(enabled);
    }

    private void enableStreaming_usbFs(boolean enabled) throws Exception {
        if (enabled && bulkMode) {
            // clearHalt(camStreamingEndpoint.getAddress());
        }
        int altSetting = enabled ? camStreamingAltSetting : 0;
        // For bulk endpoints, altSetting is always 0.
        log("setAltSetting");
        usbdevice_fs_util.setInterface(camDeviceConnection.getFileDescriptor(), camStreamingInterface.getId(), altSetting);
    }

    private void sendStillImageTrigger() {
        log("Sending Still Image Trigger");
        byte buf[] = new byte[1];
        buf[0] = 1;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_IMAGE_TRIGGER_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len != 1) {
            displayMessage("Still Image Controltransfer failed !!");
            log("Still Image Controltransfer failed !!");
        }
    }

    // Resets the error code after retrieving it.
    private int getVideoControlErrorCode() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 99;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VC_REQUEST_ERROR_CODE_CONTROL << 8, 0, buf, 1, 1000);
        if (len != 1) {
            throw new Exception("VC_REQUEST_ERROR_CODE_CONTROL failed, len=" + len + ".");
        }
        return buf[0];
    }

    // Does not work with Logitech C310? Always returns 0.
    private int getVideoStreamErrorCode() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 99;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STREAM_ERROR_CODE_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len == 0) {
            return 0;
        }                   // ? (Logitech C310 returns len=0)
        if (len != 1) {
            throw new Exception("VS_STREAM_ERROR_CODE_CONTROL failed, len=" + len + ".");
        }
        return buf[0];
    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(StartIsoStreamActivityUsbIso.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void log(String msg) {
        Log.i("UVC_Camera_Iso_Stream", msg);
    }

    public void logError(String msg) {
        Log.e("UVC_Camera", msg);
    }

    public void displayErrorMessage(Throwable e) {
        Log.e("UVC_Camera", "Error in MainActivity", e);
        displayMessage("Error: " + e);
    }

    // see 10918-1:1994, K.3.3.1 Specification of typical tables for DC difference coding
    private static byte[] mjpgHuffmanTable = {
            (byte) 0xff, (byte) 0xc4, (byte) 0x01, (byte) 0xa2, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x05, (byte) 0x01, (byte) 0x01,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
            (byte) 0x09, (byte) 0x0a, (byte) 0x0b, (byte) 0x10, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x03, (byte) 0x03, (byte) 0x02,
            (byte) 0x04, (byte) 0x03, (byte) 0x05, (byte) 0x05, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x7d,
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x00, (byte) 0x04, (byte) 0x11, (byte) 0x05, (byte) 0x12, (byte) 0x21, (byte) 0x31,
            (byte) 0x41, (byte) 0x06, (byte) 0x13, (byte) 0x51, (byte) 0x61, (byte) 0x07, (byte) 0x22, (byte) 0x71, (byte) 0x14, (byte) 0x32,
            (byte) 0x81, (byte) 0x91, (byte) 0xa1, (byte) 0x08, (byte) 0x23, (byte) 0x42, (byte) 0xb1, (byte) 0xc1, (byte) 0x15, (byte) 0x52,
            (byte) 0xd1, (byte) 0xf0, (byte) 0x24, (byte) 0x33, (byte) 0x62, (byte) 0x72, (byte) 0x82, (byte) 0x09, (byte) 0x0a, (byte) 0x16,
            (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x25, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a,
            (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x43, (byte) 0x44, (byte) 0x45,
            (byte) 0x46, (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57,
            (byte) 0x58, (byte) 0x59, (byte) 0x5a, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69,
            (byte) 0x6a, (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x83,
            (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a, (byte) 0x92, (byte) 0x93, (byte) 0x94,
            (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5,
            (byte) 0xa6, (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb6,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0xc6, (byte) 0xc7,
            (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4, (byte) 0xd5, (byte) 0xd6, (byte) 0xd7, (byte) 0xd8,
            (byte) 0xd9, (byte) 0xda, (byte) 0xe1, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4, (byte) 0xe5, (byte) 0xe6, (byte) 0xe7, (byte) 0xe8,
            (byte) 0xe9, (byte) 0xea, (byte) 0xf1, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0xf8,
            (byte) 0xf9, (byte) 0xfa, (byte) 0x01, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
            (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08, (byte) 0x09, (byte) 0x0a,
            (byte) 0x0b, (byte) 0x11, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x03, (byte) 0x04,
            (byte) 0x07, (byte) 0x05, (byte) 0x04, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x77, (byte) 0x00, (byte) 0x01,
            (byte) 0x02, (byte) 0x03, (byte) 0x11, (byte) 0x04, (byte) 0x05, (byte) 0x21, (byte) 0x31, (byte) 0x06, (byte) 0x12, (byte) 0x41,
            (byte) 0x51, (byte) 0x07, (byte) 0x61, (byte) 0x71, (byte) 0x13, (byte) 0x22, (byte) 0x32, (byte) 0x81, (byte) 0x08, (byte) 0x14,
            (byte) 0x42, (byte) 0x91, (byte) 0xa1, (byte) 0xb1, (byte) 0xc1, (byte) 0x09, (byte) 0x23, (byte) 0x33, (byte) 0x52, (byte) 0xf0,
            (byte) 0x15, (byte) 0x62, (byte) 0x72, (byte) 0xd1, (byte) 0x0a, (byte) 0x16, (byte) 0x24, (byte) 0x34, (byte) 0xe1, (byte) 0x25,
            (byte) 0xf1, (byte) 0x17, (byte) 0x18, (byte) 0x19, (byte) 0x1a, (byte) 0x26, (byte) 0x27, (byte) 0x28, (byte) 0x29, (byte) 0x2a,
            (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x3a, (byte) 0x43, (byte) 0x44, (byte) 0x45, (byte) 0x46,
            (byte) 0x47, (byte) 0x48, (byte) 0x49, (byte) 0x4a, (byte) 0x53, (byte) 0x54, (byte) 0x55, (byte) 0x56, (byte) 0x57, (byte) 0x58,
            (byte) 0x59, (byte) 0x5a, (byte) 0x63, (byte) 0x64, (byte) 0x65, (byte) 0x66, (byte) 0x67, (byte) 0x68, (byte) 0x69, (byte) 0x6a,
            (byte) 0x73, (byte) 0x74, (byte) 0x75, (byte) 0x76, (byte) 0x77, (byte) 0x78, (byte) 0x79, (byte) 0x7a, (byte) 0x82, (byte) 0x83,
            (byte) 0x84, (byte) 0x85, (byte) 0x86, (byte) 0x87, (byte) 0x88, (byte) 0x89, (byte) 0x8a, (byte) 0x92, (byte) 0x93, (byte) 0x94,
            (byte) 0x95, (byte) 0x96, (byte) 0x97, (byte) 0x98, (byte) 0x99, (byte) 0x9a, (byte) 0xa2, (byte) 0xa3, (byte) 0xa4, (byte) 0xa5,
            (byte) 0xa6, (byte) 0xa7, (byte) 0xa8, (byte) 0xa9, (byte) 0xaa, (byte) 0xb2, (byte) 0xb3, (byte) 0xb4, (byte) 0xb5, (byte) 0xb6,
            (byte) 0xb7, (byte) 0xb8, (byte) 0xb9, (byte) 0xba, (byte) 0xc2, (byte) 0xc3, (byte) 0xc4, (byte) 0xc5, (byte) 0xc6, (byte) 0xc7,
            (byte) 0xc8, (byte) 0xc9, (byte) 0xca, (byte) 0xd2, (byte) 0xd3, (byte) 0xd4, (byte) 0xd5, (byte) 0xd6, (byte) 0xd7, (byte) 0xd8,
            (byte) 0xd9, (byte) 0xda, (byte) 0xe2, (byte) 0xe3, (byte) 0xe4, (byte) 0xe5, (byte) 0xe6, (byte) 0xe7, (byte) 0xe8, (byte) 0xe9,
            (byte) 0xea, (byte) 0xf2, (byte) 0xf3, (byte) 0xf4, (byte) 0xf5, (byte) 0xf6, (byte) 0xf7, (byte) 0xf8, (byte) 0xf9, (byte) 0xfa};


    public class IsochronousStream extends Thread {
        private Activity activity;
        private boolean reapTheLastFrames;
        private int lastReapedFrames = 0;
        public IsochronousStream(Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
            activity = (Activity) mContext;
        }
        private boolean write = false;
        private int framecnt = 0;
        public void run() {
            try {
                USBIso usbIso64 = new USBIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize, (byte) camStreamingEndpoint.getAddress());
                usbIso64.preallocateRequests(activeUrbs);
                ByteArrayOutputStream frameData = new ByteArrayOutputStream(0x20000);
                int skipFrames = 0;
                byte[] data = new byte[maxPacketSize];
                enableStreaming(true);
                usbIso64.submitUrbs();
                while (true) {
                    if (pauseCamera) {
                        Thread.sleep(200);
                    } else {
                        USBIso.Request req = usbIso64.reapRequest(true);
                        for (int packetNo = 0; packetNo < req.getNumberOfPackets(); packetNo++) {
                            int packetStatus = req.getPacketStatus(packetNo);
                            try {if (packetStatus != 0) {
                                skipFrames = 1;}
                            } catch (Exception e){
                                log("Camera read error, packet status=" + packetStatus);
                            }
                            int packetLen = req.getPacketActualLength(packetNo);
                            if (packetLen == 0) {
                                continue;
                            }
                            if (packetLen > maxPacketSize) {
                                throw new Exception("packetLen > maxPacketSize");
                            }
                            req.getPacketData(packetNo, data, packetLen);
                            int headerLen = data[0] & 0xff;

                            try { if (headerLen < 2 || headerLen > packetLen) {
                                skipFrames = 1;
                            }
                            } catch (Exception e) {
                                log("Invalid payload header length.");
                            }
                            int headerFlags = data[1] & 0xff;
                            int dataLen = packetLen - headerLen;
                            boolean error = (headerFlags & 0x40) != 0;
                            if (error && skipFrames == 0) skipFrames = 1;
                            if (dataLen > 0 && skipFrames == 0) frameData.write(data, headerLen, dataLen);
                            if ((headerFlags & 2) != 0) {
                                if (frameData.size() <= 20000 ) skipFrames = 1;
                                // check Frame Size
                                if (checkFrameSize(frameData.size())) {
                                    skipFrames = 1;
                                    log("Checking Frame --> Skip Retruned");
                                } else if (frameData.size() > (imageWidth * imageHeight * 2)) {
                                    log("Framesize > Max YUY2 FrameSize");
                                    skipFrames = 1;
                                }
                                if (skipFrames > 0) {
                                    log("Skipping frame, len= " + frameData.size());
                                    frameData.reset();
                                    skipFrames--;
                                }
                                else {
                                    if (stillImageAufnahme) {
                                        sendStillImageTrigger();
                                        stillImageAufnahme = false;
                                    }
                                    if ((headerFlags & 0x20) != 0) {
                                        log("Still Image Bit set.\nSetting saveStillImage");
                                        saveStillImage = true;

                                    }
                                    frameData.write(data, headerLen, dataLen);
                                    if (videoformat.equals("MJPEG") ) {
                                        try {
                                            //log("Frame, len= " + frameData.size());
                                            processReceivedMJpegVideoFrameKamera(frameData.toByteArray());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else if (videoformat.equals("YUV")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.YUV);
                                    } else if (videoformat.equals("YUY2")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.YUY2);
                                    } else if (videoformat.equals("YUY2")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.YV12);
                                    } else if (videoformat.equals("YUV_420_888")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.YUV_420_888);
                                    } else if (videoformat.equals("YUV_422_888")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.YUV_422_888);
                                    }  else if (videoformat.equals("UYVY")) {
                                        processReceivedVideoFrameYuv(frameData.toByteArray(), Videoformat.UYVY);
                                    }

                                    frameData.reset();
                                }
                            }
                        }
                        if (reapTheLastFrames) {
                            if (++ lastReapedFrames == activeUrbs) break;
                        } else {
                            req.initialize();
                            req.submit();
                        }
                        if (stopKamera == true) {
                            reapTheLastFrames = true;
                            break;
                        }
                    }
                }
                log("OK");
            } catch (Exception e) {
                e.printStackTrace();
            }
            runningStream = null;
        }
    }

    public class IsochronousStreamLibUsb extends Thread {
        private Activity activity;
        private boolean reapTheLastFrames;
        private int lastReapedFrames = 0;
        private boolean write = false;
        private int framecnt = 0;
        ExecutorService mExecutorThread = Executors.newSingleThreadExecutor();
        public IsochronousStreamLibUsb(Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
            activity = (Activity) mContext;
        }
        public void run() {
            try {
                mExecutorThread.execute(new Runnable() { public void run() {
                    //JniIsoStreamActivity( 1, 1);
                }});


            } catch (Exception e) {
                e.printStackTrace();
            }
            log("OK");
            runningStreamLibUsb = null;
        }
    }

    private boolean checkFrameSize(int size) {
        if(size < 10000) return true;
        if (differentFrameSizes == null) differentFrameSizes = new int [5];
        if (lastThreeFrames == null) lastThreeFrames = new int [4];
        lastThreeFrames[3] = lastThreeFrames[2];
        lastThreeFrames[2] = lastThreeFrames[1];
        lastThreeFrames[1] = lastThreeFrames[0];
        lastThreeFrames[0] = size;
        if (++whichFrame == 5) whichFrame = 0;
        if(differentFrameSizes[whichFrame] == 0) {
            differentFrameSizes[whichFrame] = size;
            return false;
        }
        if (differentFrameSizes[whichFrame] < size) {
            differentFrameSizes[whichFrame] = size;
            return false;
        }
        int averageSize = 0;
        for (int j = 1; j < lastThreeFrames.length;j++) {
            averageSize = averageSize + lastThreeFrames[j];
        }
        if ((averageSize / 3 - 1000) < size) {
            return false;
        }
        if ((averageSize / 3 /1.3) < size) {
            log ("averageSize = " + (averageSize / 3 /1.3));
            return false;
        }
        if(size == imageWidth * imageHeight * 2) return false;
        return true;
    }

    private void fetchTheValues(){
        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("bun");
        camStreamingAltSetting=bundle.getInt("camStreamingAltSetting",0);
        videoformat=bundle.getString("videoformat");
        camFormatIndex=bundle.getInt("camFormatIndex",0);
        imageWidth=bundle.getInt("imageWidth",0);
        imageHeight=bundle.getInt("imageHeight",0);
        camFrameIndex=bundle.getInt("camFrameIndex",0);
        camFrameInterval=bundle.getInt("camFrameInterval",0);
        packetsPerRequest=bundle.getInt("packetsPerRequest",0);
        maxPacketSize=bundle.getInt("maxPacketSize",0);
        activeUrbs=bundle.getInt("activeUrbs",0);
        bUnitID = bundle.getByte("bUnitID",(byte)0);
        bTerminalID = bundle.getByte("bTerminalID",(byte)0);
        bNumControlTerminal = bundle.getByteArray("bNumControlTerminal");
        bNumControlUnit = bundle.getByteArray("bNumControlUnit");
        bcdUVC =  bundle.getByteArray("bcdUVC");
        bStillCaptureMethod = bundle.getByte("bStillCaptureMethod", (byte)0);
        LIBUSB = bundle.getBoolean("libUsb" );
        moveToNative = bundle.getBoolean("moveToNative" );
        bulkMode = bundle.getBoolean("bulkMode" );
    }

    private int round(double d){
        double dAbs = Math.abs(d);
        int i = (int) dAbs;
        double result = dAbs - (double) i;
        if(result<0.5){
            return d<0 ? -i : i;
        }else{
            return d<0 ? -(i+1) : i+1;
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static void printData (byte [] formatData) {
        Formatter formatter = new Formatter();
        for (byte b : formatData) {
            formatter.format("0x%02x ", b);
        }
        String hex = formatter.toString();
        System.out.println("hex " + hex);
    }

    private byte[] convert_Yuy2_to_NV21(byte[] YUY2Source) {
        //byte YUY2Source[imageWidth * imageHeight * 2] = /* source frame */;
        byte[] NV21Dest = new byte[imageWidth * imageHeight * 3/2];
        for (int i = 0; i != imageWidth * imageHeight; ++i) {
            NV21Dest[i] = YUY2Source[2 * i];
        }
        for (int j = 0; j != imageWidth * imageHeight / 4; ++j) {
            if(j % 2 == 0)
            {
                NV21Dest[imageWidth * imageHeight + j]       = (byte) (( YUY2Source[imageHeight*(j / imageHeight / 2    ) + 4 * j + 3]
                        + YUY2Source[imageHeight*(j / imageHeight/2 + 1) + 4 * j + 3] ) / 2);

                NV21Dest[imageWidth * imageHeight * 5/4 + j] = (byte) (( YUY2Source[imageHeight*(j / imageHeight / 2    ) + 4 * j + 1]
                        + YUY2Source[imageHeight*(j / imageHeight/2 + 1) + 4 * j + 1] ) / 2);

            } else {
                NV21Dest[imageWidth * imageHeight * 5/4 + j]       = (byte) (( YUY2Source[imageHeight*(j / imageHeight / 2    ) + 4 * j + 3]
                        + YUY2Source[imageHeight*(j / imageHeight/2 + 1) + 4 * j + 3] ) / 2);
                NV21Dest[imageWidth * imageHeight + j] = (byte) (( YUY2Source[imageHeight*(j / imageHeight / 2    ) + 4 * j + 1]
                        + YUY2Source[imageHeight*(j / imageHeight/2 + 1) + 4 * j + 1] ) / 2);
            }
        }
/*
        final int size = imageWidth * imageHeight;
        final int quarter = size / 4;
        final int vPosition = size; // This is where V starts
        final int uPosition = size + quarter; // This is where U starts
        byte [] NV21Dest = new byte [yv12Dest.length];
        System.arraycopy(NV21Dest, 0, yv12Dest, 0, size); // Y is same
        for (int i = 0; i < quarter; i++) {
            NV21Dest[size + i*2 ] = yv12Dest[vPosition + i]; // For NV21, V first
            NV21Dest[size + i*2 + 1] = yv12Dest[uPosition + i]; // For Nv21, U second
        }
*/
        return NV21Dest;
    }


    private final String getUSBFSName(final UsbDevice ctrlBlock) {
        String result = null;
        final String name = ctrlBlock.getDeviceName();
        final String[] v = !TextUtils.isEmpty(name) ? name.split("/") : null;
        if ((v != null) && (v.length > 2)) {
            final StringBuilder sb = new StringBuilder(v[0]);
            for (int i = 1; i < v.length - 2; i++)
                sb.append("/").append(v[i]);
            result = sb.toString();
        }
        if (TextUtils.isEmpty(result)) {
            log( "failed to get USBFS path, try to use default path:" + name);
            result = DEFAULT_USBFS;
        }
        return result;
    }


    public int getBus(String myString) {
        if(myString.length() > 3)
            return parseInt(myString.substring(myString.length()-7 , myString.length() - 4) ) ;
        else
            return 0;
    }

    public int getDevice(String myString) {
        if(myString.length() > 3)
            return parseInt(myString.substring(myString.length()-3)) ;
        else
            return 0;
    }

    private final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            log( "surfaceCreated:");
            Canvas canvas = mUVCCameraView.getHolder().lockCanvas();
            canvas.drawColor(Color.GRAY, PorterDuff.Mode.SRC);
            mUVCCameraView.getHolder().unlockCanvasAndPost(canvas);

        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            if ((width == 0) || (height == 0)) return;
            log( "surfaceChanged:");
            mPreviewSurface = holder.getSurface();
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
           log( "surfaceDestroyed:");
            mPreviewSurface = null;
        }
    };
}