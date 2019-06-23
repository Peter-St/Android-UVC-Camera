package humer.uvc_camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.os.Environment;
import android.os.Handler;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import com.sample.timelapse.MJPEGGenerator ;
import biz.source_code.usb.UsbIso;

public class Start_Iso_StreamActivity extends Activity {

    private static final String ACTION_USB_PERMISSION = "humer.uvc_camera.USB_PERMISSION";

    protected ImageView imageView;
    protected Button startStream;
    protected Button settingsButton;
    protected Button menu;
    protected Button stopStreamButton;
    protected ImageButton iB;

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
    private static final int PU_BRIGHTNESS_CONTROL = 0x02;

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
    private boolean bulkMode;

    // Camera Values
    private static int camStreamingAltSetting;
    private static int camFormatIndex;
    private static int camFrameIndex;
    private static int camFrameInterval;
    private static int packetsPerRequest;
    private static int maxPacketSize;
    private static int imageWidth;
    private static int imageHeight;
    private static int activeUrbs;
    private static String videoformat;
    private UsbIso usbIso;
    private static boolean camIsOpen;

    // Vales for debuging the camera
    private boolean bildaufnahme = false;
    private boolean videorecord = false;
    private boolean videorecordApiJellyBean = false;
    private boolean stopKamera = false;
    private boolean pauseCamera = false;
    private boolean longclickVideoRecord = false;
    private int stillImageFrame = 0;
    private int stillImageFrameBeenden = 0;
    private boolean stillImageAufnahme = false;
    private int stillImage = 0;
    private String controlltransfer;
    private boolean exit = false;
    public StringBuilder stringBuilder;
    private int [] convertedMaxPacketSize;
    private boolean lowerResolution;

    // Buttons & Views
    protected Button settingsButtonOverview;
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
    private volatile Start_Iso_StreamActivity.IsochronousStream runningStream;
    private SeekBar simpleSeekBar;


    // Brightness Values
    private static int brightnessMax;
    private static int brightnessMin;
    private int currentBrightness;
    private boolean changeBrightness;
    private boolean brightnessChanged;
    float discrete=0;
    static float start;
    static float end;
    float start_pos;
    int start_position=0;



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
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(Start_Iso_StreamActivity.this, startStream);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

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
        settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMenu(view);
            }
        });
        iB = (ImageButton) findViewById(R.id.Bildaufnahme);
        final MediaPlayer mp2 = MediaPlayer.create(Start_Iso_StreamActivity.this, R.raw.sound2);
        final MediaPlayer mp1 = MediaPlayer.create(Start_Iso_StreamActivity.this, R.raw.sound1);
        iB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                    mp2.start();
                    hoheAuflösung();
                    return true;
                }
            });

        iB.setOnClickListener(new View.OnClickListener() {
            @Override
                public void onClick(View view) {
                    mp1.start();
                    BildaufnahmeButtonClickEvent();
                }
        });
        iB.setEnabled(false);
        iB.setAlpha(20); // 95% transparent
        stopStreamButton = (Button) findViewById(R.id.stopKameraknopf);
        stopStreamButton.getBackground().setAlpha(20);  // 95% transparent
        stopStreamButton.setEnabled(false);

        videoButton = (ToggleButton) findViewById(R.id.videoaufnahme);
        videoButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        videorecordApiJellyBean = true;
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
                        dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH..mm..ss") ;
                        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                        bitmapToVideoEncoder.startEncoding(imageWidth, imageHeight, new File(sdPath + "output-" + lastVideo +"-" + dateFormat.format(date) + ".mp4"));

                    } else {
                        // The toggle is enabled
                        lastPicture = 0;
                        videorecord = true;
                        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                        file = new File(rootPath);
                        if (!file.exists()) {
                            file.mkdirs();
                        }

                        File saveDir = new File(rootPath + "rec/");
                        if (saveDir.isDirectory()) {
                            String[] children = saveDir.list();
                            for (int i = 0; i < children.length; i++) {
                                if (children[i].endsWith(".jpg"))
                                    new File(saveDir, children[i]).delete();
                            }}
                        saveDir.delete();

                        displayMessage("Record started");
                        startTime = System.currentTimeMillis();
                        currentTime = System.currentTimeMillis();
                    }
                } else {
                    if (longclickVideoRecord) {
                        longclickVideoRecord = false;
                        // The toggle is disabled
                        pauseCamera = true;
                        videorecord = false;
                        lastVideo ++;

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                        file = new File(sdPath);
                        long videotime = (System.currentTimeMillis() - startTime) / 1000;

                        log ("long videotime = " + videotime);
                        double a = (double) ( lastPicture / (double) videotime);
                        log ("Double a = " + a);
                        int fps = round(a);
                        log("fps ( Frame per Secound ) = " + fps);
                        log ( "lastPicture = " + lastPicture);


                        date = new Date() ;
                        dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH..mm..ss") ;

                        File fileVideo = new File(sdPath + "output-" + lastVideo +"-" + dateFormat.format(date) + ".avi");
                        try {
                            generator = new MJPEGGenerator(fileVideo, imageWidth, imageHeight, fps, lastPicture);
                            for (int addpic = 1; addpic <= lastPicture; addpic++) {
                                String curjpg = sdPath + "rec/" + addpic + ".jpg";
                                final Bitmap bitmap = BitmapFactory.decodeFile(curjpg);
                                generator.addImage(bitmap);
                            }
                            generator.finishAVI();
                        } catch (Exception e) {
                            displayMessage("Error: " + e);
                            e.printStackTrace();
                        }
                        File saveDir = new File(sdPath + "rec/");
                        if (saveDir.isDirectory()) {
                            String[] children = saveDir.list();
                            for (int i = 0; i < children.length; i++) {
                                if (children[i].endsWith(".jpg"))
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
                    } else {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            pauseCamera = true;
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            videorecordApiJellyBean = false;
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
                            lastVideo ++;
                            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                            file = new File(sdPath);
                            long videotime = (System.currentTimeMillis() - startTime) / 1000;

                            log ("long videotime = " + videotime);
                            double a = (double) ( lastPicture / (double) videotime);
                            log ("Double a = " + a);
                            int fps = round(a);
                            log("fps ( Frame per Secound ) = " + fps);
                            log ( "lastPicture = " + lastPicture);


                            date = new Date() ;
                            dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH..mm..ss") ;

                            File fileVideo = new File(sdPath + "output-" + lastVideo +"-" + dateFormat.format(date) + ".avi");
                            try {
                                generator = new MJPEGGenerator(fileVideo, imageWidth, imageHeight, fps, lastPicture);
                                for (int addpic = 1; addpic <= lastPicture; addpic++) {
                                    String curjpg = sdPath + "rec/" + addpic + ".jpg";
                                    final Bitmap bitmap = BitmapFactory.decodeFile(curjpg);
                                    generator.addImage(bitmap);
                                }
                                generator.finishAVI();
                            } catch (Exception e) {
                                displayMessage("Error: " + e);
                                e.printStackTrace();
                            }
                            File saveDir = new File(sdPath + "rec/");
                            if (saveDir.isDirectory()) {
                                String[] children = saveDir.list();
                                for (int i = 0; i < children.length; i++) {
                                    if (children[i].endsWith(".jpg"))
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
                    displayMessage("Long Click - Video starts");
                    // button is unchecked
                    videoButton.setChecked(true);
                    longclickVideoRecord = true;

                    lastPicture = 0;
                    videorecord = true;
                    String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/";
                    file = new File(rootPath);
                    if (!file.exists()) {
                        file.mkdirs();
                    }

                    File saveDir = new File(rootPath + "rec/");
                    if (saveDir.isDirectory()) {
                        String[] children = saveDir.list();
                        for (int i = 0; i < children.length; i++) {
                            if (children[i].endsWith(".jpg"))
                                new File(saveDir, children[i]).delete();
                        }}
                    saveDir.delete();
                    displayMessage("Record started");
                    startTime = System.currentTimeMillis();
                    currentTime = System.currentTimeMillis();
                }
                return true;
            }});
        videoButton.setEnabled(false);
        videoButton.setAlpha(0); // 100% transparent
        fetchTheValues();
        log("packetsPerRequest = " + packetsPerRequest);
        log("activeUrbs = " + activeUrbs);
        try {
            findCam();
        } catch (Exception e) {
            e.printStackTrace();
        }
        simpleSeekBar = (SeekBar) findViewById(R.id.simpleSeekBar); // initiate the Seek bar
        simpleSeekBar.setEnabled(false);
        simpleSeekBar.setAlpha(0);
        simpleSeekBar = null;
    }

    public void showMenu(View v) {
        Context wrapper = new ContextThemeWrapper(this, R.style.YOURSTYLE);
        PopupMenu popup = new PopupMenu(wrapper, v);
        // This activity implements OnMenuItemClickListener
        popup.inflate(R.menu.iso_stream_settings_button);


        if (lowerResolution) popup.getMenu().findItem(R.id.lowerRes).setChecked(true);
        else popup.getMenu().findItem(R.id.lowerRes).setChecked(false);


        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.lowerRes:
                        lowerResolutionClickButtonEvent();
                        return true;
                    case R.id.changeBrightness:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Runnable myRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        simpleSeekBar.setEnabled(false);
                                        simpleSeekBar.setAlpha(0);
                                        simpleSeekBar = null;
                                    }
                                };
                                Handler myHandler = new Handler();
                                final int TIME_TO_WAIT = 2500;


                                start = brightnessMin;
                                end = brightnessMax;
                                start_pos = currentBrightness;
                                start_position=(int) (((start_pos-start)/(end-start))*100);
                                discrete=start_pos;

                                simpleSeekBar = (SeekBar) findViewById(R.id.simpleSeekBar); // initiate the Seek bar
                                simpleSeekBar.setEnabled(true);
                                simpleSeekBar.setAlpha(1);
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
                                        currentBrightness = Math.round(discrete);
                                        changeBrightness = true;
                                        Toast.makeText(getBaseContext(), "Brightness is : "+String.valueOf(currentBrightness), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                            }
                        });
                        return true;
                    case R.id.returnToConfigScreen:
                        returnToConfigScreen();
                        return true;
                    case R.id.beenden:
                        beenden();
                        return true;
                    default:
                        break;
                }
                return false;
            }
        });
        popup.show();
    }

    public void lowerResolutionClickButtonEvent () {
        if (lowerResolution) lowerResolution = false;
        else lowerResolution = true;
    }

    public void returnToConfigScreen() {
        stopKamera = true;
        runningStream = null;
        //imageView = (ImageView) findViewById(R.id.imageView);
        onBackPressed();
    }


    public void changePackets(MenuItem item) {
        stopKamera = true;
        runningStream = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format("PacketsPerRequest = %d (Select the number of Packets in each Request Block.", packetsPerRequest));

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty() == false)  packetsPerRequest = Integer.parseInt(input.getText().toString());

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void changeUrbs(MenuItem item) {
        stopKamera = true;
        runningStream = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(String.format("activeURBs = %d (Select the number of Requests running in paralell order.", activeUrbs));

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty() == false)  activeUrbs = Integer.parseInt(input.getText().toString());

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();



    }



    public void beenden() {
        if (camIsOpen) {
            try {
                closeCameraDevice();
            } catch (Exception e) {
                displayErrorMessage(e);
                return;
            }
        }
        else if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
        }
        finish();
    }





    public void isoStream(MenuItem Item) {

        if (camDevice == null) {
            displayMessage("No Camera connected\nPlease connect a camera");
            return;
        }

        else {


            ((Button) findViewById(R.id.startStream)).setEnabled(false);
            stopStreamButton.getBackground().setAlpha(180);  // 25% transparent
            startStream.getBackground().setAlpha(20);  // 95% transparent
            ((Button) findViewById(R.id.stopKameraknopf)).setEnabled(true);
            iB.setEnabled(true);
            iB.setAlpha(200);

            videoButton.setEnabled(true);
            videoButton.setAlpha(1); // 100% transparent


            stopKamera = false;
            try {
                openCam(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (camIsOpen) {
                if (runningStream != null) {
                    return;
                }
                runningStream = new Start_Iso_StreamActivity.IsochronousStream(this);
                runningStream.start();
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayMessage("Failed to initialize the camera device.");
                        //tv = (TextView) findViewById(R.id.textDarstellung);
                        //tv.setText("Failed to initialize the camera device.");
                    }
                });

            }
        }

    }


    private void listDevice(UsbDevice usbDevice) {
        int a = 0;
        convertedMaxPacketSize = new int [(usbDevice.getInterfaceCount()-2)];
        log ("usbDevice.getInterfaceCount()-2 = " + (usbDevice.getInterfaceCount()-2) );
        log("Interface count: " + usbDevice.getInterfaceCount());
        int interfaces = usbDevice.getInterfaceCount();
        ArrayList<String> logArray = new ArrayList<String>(512);
        stringBuilder = new StringBuilder();
        for (int i = 0; i < interfaces; i++) {
            UsbInterface usbInterface = usbDevice.getInterface(i);
            log("[ - Interface: " + usbInterface.getId()  + " class=" + usbInterface.getInterfaceClass() + " subclass=" + usbInterface.getInterfaceSubclass() );
            // UsbInterface.getAlternateSetting() has been added in Android 5.
            int endpoints = usbInterface.getEndpointCount();
            StringBuilder logEntry = new StringBuilder("[ InterfaceID " + usbInterface.getId() + " / Interfaceclass = " + usbInterface.getInterfaceClass() + " / InterfaceSubclass = " + usbInterface.getInterfaceSubclass());
            stringBuilder.append(logEntry.toString());
            stringBuilder.append("\n");
            //logArray.add(logEntry.toString());

            for (int j = 0; j < endpoints; j++) {
                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
                log("- Endpoint: addr=" + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + " maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " type=" + usbEndpoint.getType() + " ]");

                StringBuilder logEntry2 = new StringBuilder("/ addr " + String.format("0x%02x ", usbEndpoint.getAddress()).toString() + " maxPacketSize=" + returnConvertedValue(usbEndpoint.getMaxPacketSize()) + " ]");
                stringBuilder.append(logEntry2.toString());
                stringBuilder.append("\n");
                if (usbInterface.getId() == 1) {
                    convertedMaxPacketSize[a] = returnConvertedValue(usbEndpoint.getMaxPacketSize());
                    a++;
                }
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setContentView(R.layout.layout_main);
                tv = (TextView) findViewById(R.id.textDarstellung);
                tv.setSingleLine(false);
                tv.setText(stringBuilder.toString());
                settingsButtonOverview = (Button) findViewById(R.id.settingsButton);
                settingsButtonOverview.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Creating the instance of PopupMenu
                        PopupMenu popup = new PopupMenu(Start_Iso_StreamActivity.this, settingsButtonOverview);
                        //Inflating the Popup using xml file
                        popup.getMenuInflater().inflate(R.menu.camera_settings, popup.getMenu());

                        //registering popup with OnMenuItemClickListener
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                //  Toast.makeText(Main.this,"Auswahl von: " + item.getTitle(),Toast.LENGTH_SHORT).show();
                                return true; }
                        });
                        popup.show();//showing popup menu
                    }
                });//closing the setOnClickListener method
            }
        });


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





    private void findCam() throws Exception {

        camDevice = findCameraDevice();
        if (camDevice == null) {
            throw new Exception("No USB camera device found.");
        }
        if (!usbManager.hasPermission(camDevice)) {
            int a;
            PendingIntent permissionIntent2 = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
            // IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
            // registerReceiver(mUsbReceiver, filter);
            usbManager.requestPermission(camDevice, permissionIntent2);
        }
    }

    private UsbDevice findCameraDevice() {
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        log("USB devices count = " + deviceList.size());
        for (UsbDevice usbDevice : deviceList.values()) {
            log("USB device \"" + usbDevice.getDeviceName() + "\": " + usbDevice);
            if (checkDeviceHasVideoStreamingInterface(usbDevice)) {
                return usbDevice;
            }
        }
        return null;
    }

    private boolean checkDeviceHasVideoStreamingInterface(UsbDevice usbDevice) {
        return getVideoStreamingInterface(usbDevice) != null;
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
        if (!camDeviceConnection.claimInterface(camControlInterface, true)) {
            log("Failed to claim camControlInterface");
            throw new Exception("Unable to claim camera control interface.");
        }
        if (!camDeviceConnection.claimInterface(camStreamingInterface, true)) {
            log("Failed to claim camStreamingInterface");
            throw new Exception("Unable to claim camera streaming interface.");
        }

        if (init) {
            usbIso = new UsbIso(camDeviceConnection.getFileDescriptor(), packetsPerRequest, maxPacketSize);
            usbIso.preallocateRequests(activeUrbs);
        }

    }



    private void closeCameraDevice() throws IOException {
        if (usbIso != null) {
            usbIso.dispose();
            usbIso = null;
        }
        if (camDeviceConnection != null) {
            camDeviceConnection.releaseInterface(camControlInterface);
            camDeviceConnection.releaseInterface(camStreamingInterface);
            camDeviceConnection.close();
            camDeviceConnection = null;
        }
        runningStream = null;
    }

    private void initCamera() throws Exception {
        try {
            getVideoControlErrorCode();
        }                // to reset previous error states
        catch (Exception e) {
            log("Warning: getVideoControlErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        enableStreaming(false);
        try {
            getVideoStreamErrorCode();
        }                // to reset previous error states
        catch (Exception e) {
            log("Warning: getVideoStreamErrorCode() failed: " + e);
        }   // ignore error, some cameras do not support the request
        initStreamingParms();
        initBrightnessParms();
    }





    private void BildaufnahmeButtonClickEvent() {

        bildaufnahme = true;
        displayMessage("Image saved");
        log("Saving the Image ...");

    }

    private void hoheAuflösung() {

        stillImageFrame++;
        displayMessage("Still Image Frame saved");
        log("Saving the Image ....  " + "");
    }

    public void stopTheCameraStreamClickEvent(View view) {
        startStream.getBackground().setAlpha(180);  // 25% transparent
        stopStreamButton.getBackground().setAlpha(20);  // 100% transparent
        ((Button)findViewById(R.id.stopKameraknopf)).setEnabled(false);
        iB.setEnabled(false);
        iB.setAlpha(20);
        videoButton.setEnabled(false);
        videoButton.setAlpha(0); // 100% transparent



        ((Button)findViewById(R.id.startStream)).setEnabled(true);
        startStream.setEnabled(true);


        stopKamera = true;
        try {
            enableStreaming(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        displayMessage("Stopped ");
        log("Stopped");
        runningStream = null;
    }



    private void submitActiveUrbs() throws IOException {
        for (int i = 0; i < activeUrbs; i++) {
            UsbIso.Request req = usbIso.getRequest();
            req.initialize(camStreamingEndpoint.getAddress());
            req.submit();
        }
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

    private void processReceivedVideoFrameYuv(byte[] frameData) throws IOException {
        YuvImage yuvImage = new YuvImage(frameData, ImageFormat.YUY2, imageWidth, imageHeight, null);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, os);
        byte[] jpegByteArray = os.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
        if (videorecordApiJellyBean) {
            bitmapToVideoEncoder.queueFrame(bitmap);
        }

    }


    public void processReceivedMJpegVideoFrameKamera(byte[] mjpegFrameData) throws Exception {

        byte[] jpegFrameData = convertMjpegFrameToJpegKamera(mjpegFrameData);

        if (bildaufnahme) {
            bildaufnahme = false ;
            date = new Date() ;
            dateFormat = new SimpleDateFormat("dd.MM.yyyy_HH..mm..ss") ;
            Context mContext = this;
            int code = mContext.getPackageManager().checkPermission(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    mContext.getPackageName());
            if (code == PackageManager.PERMISSION_GRANTED) {
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }

                String fileName = new File(rootPath + dateFormat.format(date) + ".jpg").getPath() ;
                writeBytesToFile(fileName, jpegFrameData);
            } else displayMessage ("Storage Permission for the app were missing" );

        }

        if (stillImageAufnahme) {
            if (stillImage == 1) {
                date = new Date();
                dateFormat = new SimpleDateFormat("\"dd.MM.yyyy_HH..mm..ss") ;
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Pictures/";
                file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                //String fileName = new File(rootPath + String.valueOf(date.getTime()) + ".jpg").getPath();
                String fileName = new File(rootPath + dateFormat.format(date) + ".png").getPath() ;
                writeBytesToFile(fileName, mjpegFrameData);
            }
            stillImage++;
            if (stillImage == 2) {
                stillImageAufnahme = false;
                stillImage = 0;
            }
        }

        if (videorecord) {
            if (System.currentTimeMillis() - currentTime > 200) {
                currentTime = System.currentTimeMillis();
                lastPicture ++;
                String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/UVC_Camera/Video/rec/";
                File file = new File(rootPath);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = new File(rootPath + lastPicture + ".jpg").getPath() ;
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
                if (videorecordApiJellyBean) {
                    bitmapToVideoEncoder.queueFrame(bitmap);
                }

            } else {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegFrameData, 0, jpegFrameData.length);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
                if (videorecordApiJellyBean) {
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
        //  if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9) {
        //        throw new Exception("Invalid MJPEG frame structure, length=" + frameData.length);
        //  }
        boolean hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
        exit = false;
        if (hasHuffmanTable) {
            if (frameData.length == frameLen) {
                return frameData;
            }
            return Arrays.copyOf(frameData, frameLen);
        } else {
            int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);

            try {if (segmentDaPos == -1) {
                exit = true;
            }
            } catch (Exception e) {
                log("Segment 0xDA not found in MJPEG frame data.");}
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

    // see USB video class standard, USB_Video_Payload_MJPEG_1.5.pdf
    private byte[] convertMjpegFrameToJpeg(byte[] frameData) throws Exception {
        int frameLen = frameData.length;
        while (frameLen > 0 && frameData[frameLen - 1] == 0) {
            frameLen--;
        }

        try {
            if (frameLen < 100 || (frameData[0] & 0xff) != 0xff || (frameData[1] & 0xff) != 0xD8 || (frameData[frameLen - 2] & 0xff) != 0xff || (frameData[frameLen - 1] & 0xff) != 0xd9)
                ;
        } catch (Exception e) {
            log("Invalid MJPEG frame structure, length=" + frameData.length);
        }

        boolean hasHuffmanTable = findJpegSegment(frameData, frameLen, 0xC4) != -1;
        if (hasHuffmanTable) {
            if (frameData.length == frameLen) {
                return frameData;
            }
            return Arrays.copyOf(frameData, frameLen);
        } else {
            int segmentDaPos = findJpegSegment(frameData, frameLen, 0xDA);
            if (segmentDaPos == -1) {
                throw new Exception("Segment 0xDA not found in MJPEG frame data.");
            }
            byte[] a = new byte[frameLen + mjpgHuffmanTable.length];
            System.arraycopy(frameData, 0, a, 0, segmentDaPos);
            System.arraycopy(mjpgHuffmanTable, 0, a, segmentDaPos, mjpgHuffmanTable.length);
            System.arraycopy(frameData, segmentDaPos, a, segmentDaPos + mjpgHuffmanTable.length, frameLen - segmentDaPos);
            return a;
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
            throw new Exception("Camera initialization failed. Streaming parms commit get failed.");
        }
        log("Final streaming parms: " + dumpStreamingParms(streamingParms));
        stringBuilder.append("\nFinal streaming parms: \n");
        stringBuilder.append(dumpStreamingParms(streamingParms));
        controlltransfer = new String(dumpStreamingParms(streamingParms));
    }

    private void initBrightnessParms() throws Exception {
        final int timeout = 5000;
        int len;
        byte[] brightnessParms = new byte[2];
        // PU_BRIGHTNESS_CONTROL(0x02), GET_MIN(0x82) [UVC1.5, p. 160, 158, 96]
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MIN, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        if (len != brightnessParms.length) {
            displayMessage("Error: Durning PU_BRIGHTNESS_CONTROL");
            throw new Exception("Camera PU_BRIGHTNESS_CONTROL GET_MIN failed. len= " + len + ".");
        }
        log( "brightness min: " + unpackIntBrightness(brightnessParms));
        brightnessMin = unpackIntBrightness(brightnessParms);
        // PU_BRIGHTNESS_CONTROL(0x02), GET_MAX(0x83) [UVC1.5, p. 160, 158, 96]
        camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_MAX, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        log( "brightness max: " + unpackIntBrightness(brightnessParms));
        brightnessMax = unpackIntBrightness(brightnessParms);
        // PU_BRIGHTNESS_CONTROL(0x02), GET_RES(0x84) [UVC1.5, p. 160, 158, 96]
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_RES, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        log( "brightness res: " + unpackIntBrightness(brightnessParms));
        // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        log( "brightness cur: " + unpackIntBrightness(brightnessParms));
        currentBrightness = unpackIntBrightness(brightnessParms);
    }

    private static void packIntBrightness(int i, byte[] buf) {
        buf[0] = (byte) (i & 0xFF);
        buf[0 + 1] = (byte) ((i >>> 8) & 0xFF);
    }

    private static int unpackIntBrightness(byte[] buf) {
        return (((buf[1] ) << 8) | (buf[0] & 0xFF));

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

    private void initStillImageParms() throws Exception {
        final int timeout = 5000;
        int len;
        byte[] parms = new byte[11];
        parms[0] = (byte) camFormatIndex;
        parms[1] = (byte) camFrameIndex;
        parms[2] = 1;
//   len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, SET_CUR, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
//   if (len != parms.length) {
//      throw new Exception("Camera initialization failed. Still image parms probe set failed. len=" + len); }
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, VS_STILL_PROBE_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            throw new Exception("Camera initialization failed. Still image parms probe get failed.");
        }
        log("Probed still image parms: " + dumpStillImageParms(parms));
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
        if (len != parms.length) {
            throw new Exception("Camera initialization failed. Still image parms commit set failed.");
        }
//   len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, GET_CUR, VS_STILL_COMMIT_CONTROL << 8, camStreamingInterface.getId(), parms, parms.length, timeout);
//   if (len != parms.length) {
//      throw new Exception("Camera initialization failed. Still image parms commit get failed. len=" + len); }
//   log("Final still image parms: " + dumpStillImageParms(parms)); }
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
        log("usbIso.setInterface(camStreamingInterface.getId(), altSetting);     =    InterfaceID = "  + camStreamingInterface.getId() + "  /  altsetting ="+   altSetting);
        usbIso.setInterface(camStreamingInterface.getId(), altSetting);
        if (!enabled) {
            usbIso.flushRequests();
            if (bulkMode) {
                // clearHalt(camStreamingEndpoint.getAddress());
            }
        }
    }

// public void clearHalt (int endpointAddr) throws IOException {
//    IntByReference ep = new IntByReference(endpointAddr);
//    int rc = libc.ioctl(fileDescriptor, USBDEVFS_CLEAR_HALT, ep.getPointer());
//    if (rc != 0) {
//       throw new IOException("ioctl(USBDEVFS_CLEAR_HALT) failed, rc=" + rc + "."); }}

    private void enableStreaming_direct(boolean enabled) throws Exception {
        if (!enabled) {
            return;
        }
        // Ist unklar, wie man das Streaming disabled. AltSetting muss 0 sein damit die Video-Daten kommen.
        int len = camDeviceConnection.controlTransfer(RT_STANDARD_INTERFACE_SET, SET_INTERFACE, 0, camStreamingInterface.getId(), null, 0, 1000);
        if (len != 0) {
            throw new Exception("SET_INTERFACE (direct) failed, len=" + len + ".");
        }
    }

    private void sendStillImageTrigger() throws Exception {
        byte buf[] = new byte[1];
        buf[0] = 1;
        int len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, VS_STILL_IMAGE_TRIGGER_CONTROL << 8, camStreamingInterface.getId(), buf, 1, 1000);
        if (len != 1) {
            throw new Exception("VS_STILL_IMAGE_TRIGGER_CONTROL failed, len=" + len + ".");
        }
    }

    // Resets the error code after retrieving it.
// Does not work with the e-con camera module!
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

//------------------------------------------------------------------------------

    private static String hexDump(byte[] buf, int len) {
        StringBuilder s = new StringBuilder(len * 3);
        for (int p = 0; p < len; p++) {
            if (p > 0) {
                s.append(' ');
            }
            int v = buf[p] & 0xff;
            if (v < 16) {
                s.append('0');
            }
            s.append(Integer.toHexString(v));
        }
        return s.toString();
    }

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Start_Iso_StreamActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void log(String msg) {
        Log.i("UVC_Camera", msg);
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



    class IsochronousStream extends Thread {

        Main uvc_camera;
        Context mContext;
        Activity activity;
        StringBuilder stringBuilder;

        public IsochronousStream(Context mContext) {
            setPriority(Thread.MAX_PRIORITY);
            activity = (Activity) mContext;
        }

        public void run() {
            try {
                ByteArrayOutputStream frameData = new ByteArrayOutputStream(0x20000);
                int skipFrames = 0;
                // if (cameraType == CameraType.wellta) {
                //    skipFrames = 1; }                                // first frame may look intact but it is not always intact
                boolean frameComplete = false;
                byte[] data = new byte[maxPacketSize];
                enableStreaming(true);
                submitActiveUrbs();
                while (true) {
                    if (pauseCamera) {
                        Thread.sleep(200);
                    } else {
                        UsbIso.Request req = usbIso.reapRequest(true);
                        for (int packetNo = 0; packetNo < req.getPacketCount(); packetNo++) {
                            int packetStatus = req.getPacketStatus(packetNo);
                            try {if (packetStatus != 0) {
                                skipFrames = 1;}

                                //    throw new IOException("Camera read error, packet status=" + packetStatus);
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
                            if (error && skipFrames == 0) {
                                // throw new IOException("Error flag set in payload header.");
//                    log("Error flag detected, ignoring frame.");
                                skipFrames = 1;
                            }
                            if (dataLen > 0 && skipFrames == 0) {
                                frameData.write(data, headerLen, dataLen);
                            }
                            if ((headerFlags & 2) != 0) {
                                if (skipFrames > 0) {
                                    log("Skipping frame, len= " + frameData.size());
                                    frameData.reset();
                                    skipFrames--;
                                }
                                else {
                                    if (stillImageFrame > stillImageFrameBeenden ) {
                                        try {
                                            sendStillImageTrigger();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                        stillImageAufnahme = true;
                                    }

                                    stillImageFrameBeenden = stillImageFrame;
                                    frameData.write(data, headerLen, dataLen);
                                    if (videoformat.equals("mjpeg") ) {
                                        try {
                                            processReceivedMJpegVideoFrameKamera(frameData.toByteArray());
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }else if (videoformat.equals("yuv")){
                                        processReceivedVideoFrameYuv(frameData.toByteArray());
                                    }
                                    frameData.reset();
                                }
                            }
                        }
                        req.initialize(camStreamingEndpoint.getAddress());
                        req.submit();
                        if (stopKamera == true) {
                            break;
                        }
                    }
                    if (changeBrightness) changebright();
                }
                //enableStreaming(false);
                //processReceivedMJpegVideoFrame(frameData.toByteArray());
                //saveReceivedVideoFrame(frameData.toByteArray());
                log("OK");
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
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


    public static Bitmap decodeSampledBitmapFromByteArray(byte[] data) {



        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);


        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, imageWidth / 2, imageHeight / 2);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
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

    public void changebright () {
        final int timeout = 500;
        int len;
        byte[] brightnessParms = new byte[2];
        packIntBrightness(currentBrightness, brightnessParms);

        // PU_BRIGHTNESS_CONTROL(0x02), SET_CUR(0x01) [UVC1.5, p. 160, 158, 96]
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_SET, SET_CUR, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        if (len != brightnessParms.length) {
            displayMessage("Error: Durning PU_BRIGHTNESS_CONTROL");
        }
        // PU_BRIGHTNESS_CONTROL(0x02), GET_CUR(0x81) [UVC1.5, p. 160, 158, 96]
        len = camDeviceConnection.controlTransfer(RT_CLASS_INTERFACE_GET, GET_CUR, PU_BRIGHTNESS_CONTROL << 8, 0x0200, brightnessParms, brightnessParms.length, timeout);
        if (len != brightnessParms.length) {
            displayMessage("Error: Durning PU_BRIGHTNESS_CONTROL");
        } else {
            currentBrightness = unpackIntBrightness(brightnessParms);
            log( "currentBrightness: " + currentBrightness);
        }
        changeBrightness = false;
        if (currentBrightness != 0) brightnessChanged = true;
    }

}