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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.freeapps.hosamazzam.androidchangelanguage.MyContextWrapper;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import humer.UvcCamera.JNA_I_LibUsb.JNA_I_LibUsb;
import noman.zoomtextview.ZoomTextView;

public class Main extends AppCompatActivity {


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("usb1.0");
        System.loadLibrary("jpeg9");
        System.loadLibrary("yuv");

        //System.loadLibrary("jpeg-turbo");
        System.loadLibrary("Uvc_Support");
        System.loadLibrary("uvc");
        isLoaded = true;
    }

    // JNI METHODS
    public native long nativeCreate( long camera_pointer);

    // Native UVC Camera
    private long mNativePtr;
    private int connected_to_camera;


    public static int       camStreamingAltSetting;
    public static int       camFormatIndex;
    public static int       camFrameIndex;
    public static int       camFrameInterval;
    public static int       packetsPerRequest;
    public static int       maxPacketSize;
    public static int       imageWidth;
    public static int       imageHeight;
    public static int       activeUrbs;
    public static String    videoformat;
    public static String    deviceName;
    public static byte      bUnitID;
    public static byte      bTerminalID;
    public static byte[]    bNumControlTerminal;
    public static byte[]    bNumControlUnit;
    public static byte[]    bcdUVC;
    public static byte[]    bcdUSB;
    public static byte      bStillCaptureMethod;
    public static boolean   LIBUSB = true;
    public static boolean   moveToNative;
    public static boolean   bulkMode;

    public Button           menu;
    private ZoomTextView    tv;

    private final int       REQUEST_PERMISSION_STORAGE_READ=1;
    private final int       REQUEST_PERMISSION_STORAGE_WRITE=2;
    private final int       REQUEST_PERMISSION_CAMERA=3;
    private static int      ActivitySetUpTheUsbDeviceRequestCode = 1;
    private static int      ActivityStartIsoStreamRequestCode = 2;

    private static boolean isLoaded;

    final static float STEP = 200;
    float mRatio = 1.0f;
    int mBaseDist;
    float mBaseRatio;
    private Handler buttonHandler;
    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            Button button = findViewById(R.id.raiseSize);
            button.setEnabled(false); button.setAlpha(0);
            Button button2 = findViewById(R.id.lowerSize);
            button2.setEnabled(false); button2.setAlpha(0);
            buttonHandler = null;
        }
    };

    // Language Support
    private String LANG_CURRENT = "en";
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(newBase);
        LANG_CURRENT = preferences.getString("Language", "en");
        super.attachBaseContext(MyContextWrapper.wrap(newBase, LANG_CURRENT));
    }


    private static EditText textView;

    private static final int CREATE_REQUEST_CODE = 40;
    private static final int OPEN_REQUEST_CODE = 41;
    private static final int SAVE_REQUEST_CODE = 42;


    ///// File Save Buttons

    public void saveFile(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, SAVE_REQUEST_CODE);
    }

    public void newFile(View view)
    {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "newfile.txt");
        startActivityForResult(intent, CREATE_REQUEST_CODE);
    }

    public void openFile(View view)
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }



    private String readFileContent(Uri uri) throws IOException {

        InputStream inputStream =
                getContentResolver().openInputStream(uri);
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(
                        inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String currentline;
        while ((currentline = reader.readLine()) != null) {
            stringBuilder.append(currentline + "\n");
        }
        inputStream.close();
        return stringBuilder.toString();
    }

    private void writeFileContent(Uri uri)
    {
        try{
            ParcelFileDescriptor pfd =
                    this.getContentResolver().
                            openFileDescriptor(uri, "w");
            FileOutputStream fileOutputStream =
                    new FileOutputStream(
                            pfd.getFileDescriptor());
            String textContent =
                    textView.getText().toString();
            fileOutputStream.write(textContent.getBytes());
            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /////  END File Save Buttons



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);

        //textView = (EditText) findViewById(R.id.fileText);


        //// Language Settings
        ImageButton language = findViewById(R.id.language);
        LinearLayout sv_language_chooser = findViewById(R.id.languageChooser);
        sv_language_chooser.setEnabled(false);
        sv_language_chooser.setAlpha(0); // 100% transparent
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("en")) {
            language.setImageResource(R.mipmap.country_america);
        } else if (currentLanguage.getLanguage().equals("de")) {
            language.setImageResource(R.mipmap.country_germany);
        } else if (currentLanguage.getLanguage().equals(("ru"))) {
            language.setImageResource(R.mipmap.country_russia);
        } else if (currentLanguage.getLanguage().equals("it")) {
            language.setImageResource(R.mipmap.country_italy);
        } else if (currentLanguage.getLanguage().equals("ko")) {
            language.setImageResource(R.mipmap.country_korea);
        } else if (currentLanguage.getLanguage().equals("ja")) {
            language.setImageResource(R.mipmap.country_japan);
        } else if (currentLanguage.getLanguage().equals("pt")) {
            language.setImageResource(R.mipmap.country_portugal);
        } else if (currentLanguage.getLanguage().equals("th")) {
            language.setImageResource(R.mipmap.country_thai);
        } else if (currentLanguage.getLanguage().equals("zh")) {
            language.setImageResource(R.mipmap.country_china);
        }
        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
        if (camFrameInterval == 0) tv.setText(getResources().getString(R.string.intro) + "\n\n" + getResources().getString(R.string.packetsPerRequest) + " = " + packetsPerRequest + "\n" + getResources().getString(R.string.activeUrbs) + " = " + activeUrbs +
                "\n" + getResources().getString(R.string.camStreamingAltSetting) + " = " + camStreamingAltSetting + "\n" + getResources().getString(R.string.maxPacketSize) + " = " + maxPacketSize + "\n" + getResources().getString(R.string.videoformat) + " = " + videoformat +
                        "\n" + getResources().getString(R.string.camFormatIndex) + " = " + camFormatIndex + "\n" +
                        " " + getResources().getString(R.string.camFrameIndex) + " = " + camFrameIndex + "\n" + getResources().getString(R.string.imageWidth) + " = " + imageWidth + "\n" + getResources().getString(R.string.imageHeight) + " = " + imageHeight +
                        "\n" + getResources().getString(R.string.camFrameInterval) + " (fps) = " + camFrameInterval + "\nLibUsb = " + LIBUSB);
        else tv.setText("Hello\n\nThe App should works on Android 9 (PIE) and Android 10 (Q) Devices." +
                "\n\nYour current Values are:\n\n( - this is a sroll and zoom field - )\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaxPacketSize = " + maxPacketSize + "\nVideoformat = " + videoformat + "\ncamFormatIndex = " + camFormatIndex + "\n" +
                "camFrameIndex = " + camFrameIndex + "\nimageWidth = "+ imageWidth + "\nimageHeight = " + imageHeight + "\ncamFrameInterval (fps) = " +
                (10000000 / camFrameInterval) + "\nLibUsb = " + LIBUSB  +  "" +
                "\n\nYou can edit these Settings by clicking on (Set Up The Camera Device).\nYou can then save the values and later restore them.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ScrollView scrollView = findViewById(R.id.scrolli);
            scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                @Override
                public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                    final int TIME_TO_WAIT = 2500;
                    Button button = findViewById(R.id.raiseSize);
                    if (button.isEnabled()) {
                        buttonHandler.removeCallbacks(myRunnable);
                        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                        return ;
                    }
                    button.setEnabled(true);
                    button.setAlpha(0.8f);
                    Button button2 = findViewById(R.id.lowerSize);
                    button2.setEnabled(true); button2.setAlpha(0.8f);
                    buttonHandler = new Handler();
                    buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
                }
            });
        }
        Button button = findViewById(R.id.raiseSize);
        button.setEnabled(false); button.setAlpha(0);
        Button button2 = findViewById(R.id.lowerSize);
        button2.setEnabled(false); button2.setAlpha(0);
        // New button for Libusb
        ToggleButton libUsbActivate = findViewById(R.id.libusbToggleButton);
        libUsbActivate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    LIBUSB = true;
                    log("libusb true");
                }
                else {
                    LIBUSB = false;
                    log("libusb false");
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STORAGE_READ:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(Main.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Main.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_PERMISSION_STORAGE_WRITE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(Main.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Main.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(Main.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Main.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ActivitySetUpTheUsbDeviceRequestCode && resultCode == RESULT_OK && data != null) {
            // TODO Extract the data returned from the child Activity.
            camStreamingAltSetting = data.getIntExtra("camStreamingAltSetting", 0);
            videoformat = data.getStringExtra("videoformat");
            camFormatIndex = data.getIntExtra("camFormatIndex", 0);
            imageWidth = data.getIntExtra("imageWidth", 0);
            imageHeight = data.getIntExtra("imageHeight", 0);
            camFrameIndex = data.getIntExtra("camFrameIndex", 0);
            camFrameInterval = data.getIntExtra("camFrameInterval", 0);
            packetsPerRequest = data.getIntExtra("packetsPerRequest", 0);
            maxPacketSize = data.getIntExtra("maxPacketSize", 0);
            activeUrbs = data.getIntExtra("activeUrbs", 0);
            deviceName = data.getStringExtra("deviceName");
            bUnitID = data.getByteExtra("bUnitID", (byte) 0);
            bTerminalID = data.getByteExtra("bTerminalID", (byte) 0);
            bNumControlTerminal = data.getByteArrayExtra("bNumControlTerminal");
            bNumControlUnit = data.getByteArrayExtra("bNumControlUnit");
            bcdUVC = data.getByteArrayExtra("bcdUVC");
            bStillCaptureMethod = data.getByteExtra("bStillCaptureMethod", (byte) 0);
            LIBUSB = data.getBooleanExtra("libUsb", false);
            moveToNative = data.getBooleanExtra("moveToNative", false);
            bulkMode = data.getBooleanExtra("bulkMode", false);
            if (camFrameInterval == 0)
                tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest + "\nActive Urbs = " + activeUrbs +
                        "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat +
                        "\nCamera Format Index = " + camFormatIndex + "\n" +
                        "Camera FrameIndex = " + camFrameIndex + "\nImage Width = " + imageWidth + "\nImage Height = " + imageHeight +
                        "\nCamera Frame Interval (fps) = " + camFrameInterval + "\nLibUsb = " + LIBUSB);
            else
                tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest + "\nActive Urbs = " + activeUrbs +
                        "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                        "Camera FrameIndex = " + camFrameIndex + "\nImage Width = " + imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval (fps) = " + (10000000 / camFrameInterval) + "\nLibUsb = " + LIBUSB);
            tv.setTextColor(Color.BLACK);
        }
        if (requestCode == ActivityStartIsoStreamRequestCode && resultCode == RESULT_OK && data != null) {
            connected_to_camera = data.getIntExtra("connected_to_camera", 0);
            boolean exit = data.getBooleanExtra("closeProgram", false);
            if (exit == true) finish();
        }

        ///// File Save Buttons

        Uri currentUri = null;
        if (resultCode == Activity.RESULT_OK)
        {
            if (requestCode == CREATE_REQUEST_CODE)
            {
                if (data != null) {
                    //textView.setText("");
                }
            } else if (requestCode == SAVE_REQUEST_CODE) {

                if (data != null) {
                    currentUri = data.getData();
                    writeFileContent(currentUri);
                }
            } else if (requestCode == OPEN_REQUEST_CODE) {

                if (data != null) {
                    currentUri = data.getData();

                    try {
                        String content =
                                readFileContent(currentUri);
                        //textView.setText(content);
                    } catch (IOException e) {
                        // Handle error here
                    }
                }
            }
        }
    }

    ////////////////   BUTTONS  //////////////////////////////////////////

    public void changeTheLanguage(View view){
        if(isLanguageChooserEnabled()) {
            log ("languageChooser is enabled");
            disableLanguageChooser();
            displayIntro();
        } else{
            log ("enabling languageChooser");
            tv.setEnabled(false);
            tv.setAlpha(0);
            LinearLayout sv_language_chooser = findViewById(R.id.languageChooser);
            sv_language_chooser.setEnabled(true);
            sv_language_chooser.setAlpha(1); // 100% transparent
        }
    }

    public void raiseSize(View view){
        final int TIME_TO_WAIT = 2500;
        Button button = findViewById(R.id.raiseSize);
        if (button.isEnabled()) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
            tv.raiseSize();
            return ;
        }
        button.setEnabled(true);
        button.setAlpha(0.8f);
        Button button2 = findViewById(R.id.lowerSize);
        button2.setEnabled(true); button2.setAlpha(0.8f);
        tv.raiseSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void lowerSize(View view){
        final int TIME_TO_WAIT = 2500;
        Button button = findViewById(R.id.raiseSize);
        if (button.isEnabled()) {
            buttonHandler.removeCallbacks(myRunnable);
            buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
            tv.lowerSize();
            return;
        }
        button.setEnabled(true);
        button.setAlpha(0.8f);
        Button button2 = findViewById(R.id.lowerSize);
        button2.setEnabled(true); button2.setAlpha(0.8f);
        tv.lowerSize();
        buttonHandler = new Handler();
        buttonHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }


    public void viewPrivatePolicy(View view) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getApplicationContext(),
                PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    public void viewReadme (View view) {
        // TODO Auto-generated method stub
        Intent intent = new Intent(getApplicationContext(),
                ReadMeActivity.class);
        Bundle bundle=new Bundle();
        log(getBaseContext().getResources().getConfiguration().locale.getLanguage());
        bundle.putString("locale",getBaseContext().getResources().getConfiguration().locale.getLanguage());
        intent.putExtra("bun",bundle);
        startActivity(intent);
    }

    public void setUpTheUsbDevice(View view){
        // if LIBUSB allocate the UVC Camera Pointer:
        if (LIBUSB && mNativePtr == 0) {
            mNativePtr = nativeCreate(mNativePtr);
            log("mNativePtr = " + mNativePtr);
/*
            JNA_I_LibUsb.uvc_camera.ByReference uvcCamera =  new  Pointer(mNativePtr);
            uvcCamera.preview_pointer = 0;
            uvcCamera.read();
            uvc_camera.preview_pointer = 500;
            readNativeStruct(mNativePtr);
            JNA_I_LibUsb.INSTANCE.read_a_value(new Pointer(mNativePtr));
            uvc_camera.preview_pointer = 600;
            readNativeStruct(mNativePtr);
            JNA_I_LibUsb.INSTANCE.read_a_value(new Pointer(mNativePtr));
 */
        }

        // load the libs if needed

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (showStoragePermissionRead() && showStoragePermissionWrite() && showCameraPermissionCamera()) {
                if (LIBUSB) {


                    Intent intent = new Intent(this, SetUpTheUsbDeviceUvc.class);
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
                    bundle.putString("deviceName",deviceName);
                    bundle.putByte("bUnitID",bUnitID);
                    bundle.putByte("bTerminalID",bTerminalID);
                    bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                    bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                    bundle.putByteArray("bcdUVC", bcdUVC);
                    bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    super.onResume();
                    startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
                } else {
                    Intent intent = new Intent(this, SetUpTheUsbDeviceUsbIso.class);
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
                    bundle.putString("deviceName",deviceName);
                    bundle.putByte("bUnitID",bUnitID);
                    bundle.putByte("bTerminalID",bTerminalID);
                    bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                    bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                    bundle.putByteArray("bcdUVC", bcdUVC);
                    bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    super.onResume();
                    startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
                }
            }
        }
        else if (showStoragePermissionRead() && showStoragePermissionWrite()) {
            if (LIBUSB) {

                Intent intent = new Intent(this, SetUpTheUsbDeviceUvc.class);
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
                bundle.putString("deviceName",deviceName);
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                bundle.putByteArray("bcdUVC", bcdUVC);
                bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                bundle.putBoolean("libUsb", LIBUSB);
                bundle.putBoolean("moveToNative", moveToNative);
                bundle.putBoolean("bulkMode", bulkMode);
                bundle.putLong("mNativePtr", mNativePtr);
                bundle.putInt("connected_to_camera", connected_to_camera);
                intent.putExtra("bun",bundle);
                super.onResume();
                startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
            } else {
                Intent intent = new Intent(this, SetUpTheUsbDeviceUsbIso.class);
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
                bundle.putString("deviceName",deviceName);
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                bundle.putByteArray("bcdUVC", bcdUVC);
                bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                bundle.putBoolean("libUsb", LIBUSB);
                bundle.putBoolean("moveToNative", moveToNative);
                bundle.putBoolean("bulkMode", bulkMode);
                bundle.putLong("mNativePtr", mNativePtr);
                bundle.putInt("connected_to_camera", connected_to_camera);
                intent.putExtra("bun",bundle);
                super.onResume();
                startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
            }
        }
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (LIBUSB) {

                Intent intent = new Intent(this, SetUpTheUsbDeviceUvc.class);
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
                bundle.putString("deviceName",deviceName);
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                bundle.putByteArray("bcdUVC", bcdUVC);
                bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                bundle.putBoolean("libUsb", LIBUSB);
                bundle.putBoolean("moveToNative", moveToNative);
                bundle.putBoolean("bulkMode", bulkMode);
                bundle.putLong("mNativePtr", mNativePtr);
                bundle.putInt("connected_to_camera", connected_to_camera);
                intent.putExtra("bun",bundle);
                super.onResume();
                startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
            } else {
                Intent intent = new Intent(this, SetUpTheUsbDeviceUsbIso.class);
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
                bundle.putString("deviceName",deviceName);
                bundle.putByte("bUnitID",bUnitID);
                bundle.putByte("bTerminalID",bTerminalID);
                bundle.putByteArray("bNumControlTerminal", bNumControlTerminal);
                bundle.putByteArray("bNumControlUnit", bNumControlUnit);
                bundle.putByteArray("bcdUVC", bcdUVC);
                bundle.putByte("bStillCaptureMethod",bStillCaptureMethod);
                bundle.putBoolean("libUsb", LIBUSB);
                bundle.putBoolean("moveToNative", moveToNative);
                bundle.putBoolean("bulkMode", bulkMode);
                bundle.putLong("mNativePtr", mNativePtr);
                bundle.putInt("connected_to_camera", connected_to_camera);
                intent.putExtra("bun",bundle);
                super.onResume();
                startActivityForResult(intent, ActivitySetUpTheUsbDeviceRequestCode);
            }

        }
        ToggleButton libUsbActivate = findViewById(R.id.libusbToggleButton);
        libUsbActivate.setEnabled(false);
    }

    public void restoreCameraSettings (View view) {
        if (showStoragePermissionRead() && showStoragePermissionWrite()) {
            SaveToFile  stf;
            stf = new SaveToFile(this, this);
            stf.restoreValuesFromFile();
            stf = null;
        }
        else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            SaveToFile  stf;
            stf = new SaveToFile(this, this);
            stf.restoreValuesFromFile();
            stf = null;
        }
    }

    public void isoStream(View view){
        if (LIBUSB && mNativePtr == 0) {
            mNativePtr = nativeCreate(mNativePtr);
            log("mNativePtr = " + mNativePtr);
/*
            JNA_I_LibUsb.uvc_camera.ByReference uvcCamera =  new  Pointer(mNativePtr);
            uvcCamera.preview_pointer = 0;
            uvcCamera.read();
            uvc_camera.preview_pointer = 500;
            readNativeStruct(mNativePtr);
            JNA_I_LibUsb.INSTANCE.read_a_value(new Pointer(mNativePtr));
            uvc_camera.preview_pointer = 600;
            readNativeStruct(mNativePtr);
            JNA_I_LibUsb.INSTANCE.read_a_value(new Pointer(mNativePtr));
 */
        }
        if (showStoragePermissionRead() && showStoragePermissionWrite()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if(!showCameraPermissionCamera()) return;
            }
            if (camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0 ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Values for the camera not correctly setted !!\nPlease set up the values for the Camera first.\nTo Set Up the Values press the Settings " +
                                "Button and click on 'Set up with Uvc Values' or 'Edit / Save / Restor' and 'Edit Save'");
                        tv.setTextColor(darker(Color.RED, 100));
                    }
                });
            } else {
                if (LIBUSB) {
                    Intent intent = new Intent(this, StartIsoStreamActivityUvc.class);
                    Bundle bundle=new Bundle();
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
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    startActivityForResult(intent, ActivityStartIsoStreamRequestCode);
                } else {
                    Intent intent = new Intent(this, StartIsoStreamActivityUsbIso.class);
                    Bundle bundle=new Bundle();
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
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    startActivityForResult(intent, ActivityStartIsoStreamRequestCode);
                }

            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (camFormatIndex == 0 || camFrameIndex == 0 ||camFrameInterval == 0 ||packetsPerRequest == 0 ||maxPacketSize == 0 ||imageWidth == 0 || activeUrbs == 0 ) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv = (ZoomTextView) findViewById(R.id.textDarstellung);
                        tv.setText("Values for the camera not correctly setted !!\nPlease set up the values for the Camera first.\nTo Set Up the " +
                                "Values press the Settings Button and click on 'Set up with Uvc Values' or 'Edit / Save / Restor' and 'Edit Save'");  }
                });
            } else {
                if (LIBUSB) {
                    Intent intent = new Intent(this, StartIsoStreamActivityUvc.class);
                    Bundle bundle=new Bundle();
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
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    startActivityForResult(intent, ActivityStartIsoStreamRequestCode);
                } else {
                    Intent intent = new Intent(this, StartIsoStreamActivityUsbIso.class);
                    Bundle bundle=new Bundle();
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
                    bundle.putBoolean("libUsb", LIBUSB);
                    bundle.putBoolean("moveToNative", moveToNative);
                    bundle.putBoolean("bulkMode", bulkMode);
                    bundle.putLong("mNativePtr", mNativePtr);
                    bundle.putInt("connected_to_camera", connected_to_camera);
                    intent.putExtra("bun",bundle);
                    startActivityForResult(intent, ActivityStartIsoStreamRequestCode);
                }

            }
        }
        ToggleButton libUsbActivate = findViewById(R.id.libusbToggleButton);
        libUsbActivate.setEnabled(false);
    }

    // Language Buttons Methods

    public void changeLang(Context context, String lang) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("Language", lang);
        editor.apply();
    }

    public void onAmericaLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("en")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "en"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onGermanyLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("de")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "de"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onItalyLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("it")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "it"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onJapanLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("ja")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "ja"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onKoreaLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("ko")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "ko"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onPortugalLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("pt")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "pt"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onRussiaLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("ru")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "ru"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onThaiLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("th")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "th"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onChinaLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("zh")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "zh"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    public void onDefaultLanguageSelected(View view) {
        Locale currentLanguage = getResources().getConfiguration().locale;;
        if (currentLanguage.getLanguage().equals("en")) {
            disableLanguageChooser();
            displayIntro();
            return;
            //language.setImageResource(R.mipmap.country_america);
        }
        String languageToLoad  = "en"; // your language
        changeLang(Main.this, languageToLoad);
        finish();
        startActivity(new Intent(Main.this, Main.class));
    }

    // Other Methods

    public void displayMessage(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Main.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void log(String msg) {
        Log.i("UVC_Camera_Main", msg);
    }

    public void setTextTextView () {
        if (camFrameInterval == 0) tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                "Camera FrameIndex = " + camFrameIndex + "\nImage Width = "+ imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval (fps) = " +
                camFrameInterval + "\n\nUVC Values:\nbUnitID = " + bUnitID + "\nbTerminalID = " + bTerminalID + "\nbcdUVC = " + getbcdUVC() + "\nLibUsb = " + LIBUSB  );
        else tv.setText("Your current Values are:\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat + "\nCamera Format Index = " + camFormatIndex + "\n" +
                "Camera FrameIndex = " + camFrameIndex + "\nImage Width = "+ imageWidth + "\nImage Height = " + imageHeight + "\nCamera Frame Interval (fps) = " +
                (10000000 / camFrameInterval) + "\n\nUVC Values:\nbUnitID = " + bUnitID + "\nbTerminalID = " + bTerminalID  + "\nbcdUVC = " + getbcdUVC() + "\nLibUsb = " + LIBUSB  );
        tv.setTextColor(darker(Color.GREEN, 100));
    }

    private String getbcdUVC() {
        if (bcdUVC != null) {
            String a = new String(Integer.toString(bcdUVC[1]));
            a += ".";
            NumberFormat formatter = new DecimalFormat("00");
            String s = formatter.format(bcdUVC[0]); // ----> 01
            a += s;
            return a;
        } else return "?";
    }

    private boolean showStoragePermissionRead() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed:", "Read External Storage", Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE_READ);
                return false;
            } else {
                requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE_READ);
                return false;
            }
        } else {
            //Toast.makeText(Main.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private boolean showStoragePermissionWrite() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed:", "Write to External Storage", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE_WRITE);
                return false;
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE_WRITE);
                return false;
            }
        } else {
            //Toast.makeText(Main.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private boolean showCameraPermissionCamera() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                showExplanation("Permission Needed:", "Camera", Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
                return false;
            } else {
                requestPermission(Manifest.permission.CAMERA, REQUEST_PERMISSION_CAMERA);
                return false;
            }
        } else {
            //Toast.makeText(Main.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    public static int darker (int color, float factor) {
        int a = Color.alpha( color );
        int r = Color.red( color );
        int g = Color.green( color );
        int b = Color.blue( color );

        return Color.argb( a,
                Math.max( (int)(r * factor), 0 ),
                Math.max( (int)(g * factor), 0 ),
                Math.max( (int)(b * factor), 0 ) );
    }

    private void disableLanguageChooser() {
        LinearLayout sv_language_chooser = findViewById(R.id.languageChooser);
        sv_language_chooser.setEnabled(false);
        sv_language_chooser.setAlpha(0); // 100% transparent
    }

    private boolean isLanguageChooserEnabled () {
        LinearLayout sv_language_chooser = findViewById(R.id.languageChooser);
        if(sv_language_chooser.isEnabled()) return true;
        return false;
    }

    private void displayIntro() {
        tv.setEnabled(true);
        tv.setAlpha(1);
        if (camFrameInterval == 0) tv.setText(getResources().getString(R.string.intro) + "\n\nPackets Per Request = " + packetsPerRequest + "\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaximal Packet Size = " + maxPacketSize + "\nVideoformat = " + videoformat +
                "\nCamera Format Index = " + camFormatIndex + "\n" +
                "Camera FrameIndex = " + camFrameIndex + "\nImage Width = " + imageWidth + "\nImage Height = " + imageHeight +
                "\nCamera Frame Interval (fps) = " + camFrameInterval + "\nLibUsb = " + LIBUSB);
        else tv.setText(getResources().getString(R.string.intro) + "Hello\n\nThis App may not work on Android 9 (PIE) and Android 10 (Q) Devices. In this case please use other Usb Camera Apps from the Play Store" +
                "\n\nYour current Values are:\n\n( - this is a sroll and zoom field - )\n\nPackets Per Request = " + packetsPerRequest +"\nActive Urbs = " + activeUrbs +
                "\nAltSetting = " + camStreamingAltSetting + "\nMaxPacketSize = " + maxPacketSize + "\nVideoformat = " + videoformat + "\ncamFormatIndex = " + camFormatIndex + "\n" +
                "camFrameIndex = " + camFrameIndex + "\nimageWidth = "+ imageWidth + "\nimageHeight = " + imageHeight + "\ncamFrameInterval (fps) = " +
                (10000000 / camFrameInterval) + "\nLibUsb = " + LIBUSB  +  "" +
                "\n\nYou can edit these Settings by clicking on (Set Up The Camera Device).\nYou can then save the values and later restore them.");
    }



}
