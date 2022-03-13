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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;

import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.TextInputLayout;
import android.text.InputType;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.crowdfire.cfalertdialog.views.CFPushButton;
import com.sun.jna.Pointer;
import com.tomer.fadingtextview.FadingTextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import humer.UvcCamera.AutomaticDetection.Jna_AutoDetect;
import humer.UvcCamera.AutomaticDetection.LibUsb_AutoDetect;
import humer.UvcCamera.JNA_I_LibUsb.JNA_I_LibUsb;
import humer.UvcCamera.UVC_Descriptor.UVC_Descriptor;

public class SaveToFile  {

    public static int       sALT_SETTING;
    public static int       smaxPacketSize ;
    public static int       scamFormatIndex ;   // MJPEG // YUV // bFormatIndex: 1 = uncompressed
    public static String    svideoformat;
    public static int       scamFrameIndex ; // bFrameIndex: 1 = 640 x 360;       2 = 176 x 144;     3 =    320 x 240;      4 = 352 x 288;     5 = 640 x 480;
    public static int       simageWidth;
    public static int       simageHeight;
    public static int       scamFrameInterval ; // 333333 YUV = 30 fps // 666666 YUV = 15 fps
    public static int       spacketsPerRequest ;
    public static int       sactiveUrbs ;
    public static String    sdeviceName;
    public static byte      bUnitID;
    public static byte      bTerminalID;
    public static byte[]    bNumControlTerminal;
    public static byte[]    bNumControlUnit;
    public static byte[]    bcdUVC;
    public static byte[]    bcdUSB;
    public static byte      bStillCaptureMethod;
    private static String   saveFilePathFolder = "values_for_the_camera";
    private static String   autoFilePathFolder = "autoDetection";
    private TextInputLayout valueInput;
    private TextInputLayout valueInput_libUsb;
    private boolean         init = false;
    private static boolean  libUsb;
    public static boolean   moveToNative;
    public static boolean  bulkMode;



    private LibUsb_AutoDetect libUsb_autoDetect;
    private SetUpTheUsbDeviceUsbIso setUpTheUsbDeviceUsbIso;
    private SetUpTheUsbDeviceUvc setUpTheUsbDeviceUvc;

    private Jna_AutoDetect jna_autoDetect;
    private Main uvc_camera = null;
    private Context mContext;
    private View v;
    private Activity activity;

    TextView sALT_SETTING_text;
    TextView smaxPacketSize_text;
    TextView scamFormatIndex_text;
    TextView svideoformat_text;
    TextView scamFrameIndex_text;
    TextView simageWidth_text;
    TextView simageHeight_text;
    TextView scamFrameInterval_text;
    TextView spacketsPerRequest_text;
    TextView sactiveUrbs_text;
    TextView sLibUsb_text;

    StringBuilder stringBuilder;
    String rootdirStr;

    private enum OptionForSaveFile {savetofile, restorefromfile}
    OptionForSaveFile optionForSaveFile;
    static ArrayList<String> paths = new ArrayList<>(50);
    static String autoDetectFileOrdersString;
    static String autoDetectFileValuesString;

    TextView tv;

    private UVC_Descriptor uvc_descriptor;
    private static int [] numberFormatIndexes;
    public UVC_Descriptor.FormatIndex formatIndex;
    public UVC_Descriptor.FormatIndex.FrameIndex frameIndex;
    private static String[] frameDescriptorsResolutionArray;
    private static String [] dwFrameIntervalArray;
    private static String [] maxPacketSizeStr;
    private String name;
    private String fileName;

    // Values for Auto Detection
    public static boolean completed;
    public static boolean highQuality;
    public static boolean raiseMaxPacketSize;
    public static boolean lowerMaxPacketSize;
    public static boolean raisePacketsPerRequest;
    public static boolean raiseActiveUrbs;

    public static boolean highestMaxPacketSizeDone;
    public static boolean lowestMaxPacketSizeDone;

    private boolean libUsb_AutoDetect;

    // VS Interface Descriptor Subtypes
    private final static byte VS_UNDEFINED = 0x00;
    private final static byte VS_input_header = 0x01;
    private final static byte VS_still_image_frame = 0x03;
    private final static byte VS_format_uncompressed = 0x04;
    private final static byte VS_frame_uncompressed = 0x05;
    private final static byte VS_format_mjpeg = 0x06;
    private final static byte VS_frame_mjpeg = 0x07;
    private final static byte VS_colour_format = 0x0D;

    public SaveToFile(Main main, Context mContext) {
        this.uvc_camera = main;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
    }

    public SaveToFile (SetUpTheUsbDeviceUsbIso setUpTheUsbDeviceUsbIso, Context mContext, View v) {
        this.setUpTheUsbDeviceUsbIso = setUpTheUsbDeviceUsbIso;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
        this.v = v;
        this.init = true;
    }

    public SaveToFile (SetUpTheUsbDeviceUvc setUpTheUsbDeviceUvc, Context mContext, View v) {
        this.setUpTheUsbDeviceUvc = setUpTheUsbDeviceUvc;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
        this.v = v;
        this.init = true;
    }

    public SaveToFile(LibUsb_AutoDetect libUsb_autoDetect, Context mContext) {
        this.libUsb_autoDetect = libUsb_autoDetect;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
        this.libUsb_AutoDetect = true;
    }

    public SaveToFile(Jna_AutoDetect jna_autoDetect, Context mContext) {
        this.jna_autoDetect = jna_autoDetect;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
        this.libUsb_AutoDetect = true;
    }


    private void returnToMainLayout(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(R.layout.set_up_the_device_layout_main);
                tv = activity.findViewById(R.id.textDarstellung);
                if (scamFrameInterval == 0) tv.setText(msg + "\n\nYour current Values are:\n\nPackets Per Request = " + spacketsPerRequest +"\nActive Urbs = " + sactiveUrbs +
                        "\nAltSetting = " + sALT_SETTING + "\nMaximal Packet Size = " + smaxPacketSize + "\nVideoformat = " + svideoformat + "\nCamera Format Index = " + scamFormatIndex + "\n" +
                        "Camera FrameIndex = " + scamFrameIndex + "\nImage Width = "+ simageWidth + "\nImage Height = " + simageHeight + "\nCamera Frame Interval = " + scamFrameInterval + "\nLibUsb = " + libUsb);
                else tv.setText(msg + "\n\nYour current Values are:\n\nPackets Per Request = " + spacketsPerRequest +"\nActive Urbs = " + sactiveUrbs +
                        "\nAltSetting = " + sALT_SETTING + "\nMaximal Packet Size = " + smaxPacketSize + "\nVideoformat = " + svideoformat + "\nCamera Format Index = " + scamFormatIndex + "\n" +
                        "Camera FrameIndex = " + scamFrameIndex + "\nImage Width = "+ simageWidth + "\nImage Height = " + simageHeight + "\nCamera Frame Interval (fps) = " + (10000000 / scamFrameInterval)  + "\nLibUsb = " + libUsb );
                tv.setTextColor(Color.BLACK);

                ConstraintLayout fadingTextView = (ConstraintLayout) activity.findViewById(R.id.fadingTextViewLayout);
                fadingTextView.setVisibility(View.GONE);
                fadingTextView.setVisibility(View.INVISIBLE);

                FadingTextView FTV = (FadingTextView) activity.findViewById(R.id.fadingTextView);
                FTV.setVisibility(View.INVISIBLE);
                FTV.setVisibility(View.GONE);
                Button testrun  = activity.findViewById(R.id.testrun);
                testrun.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (setUpTheUsbDeviceUsbIso != null) setUpTheUsbDeviceUsbIso.showTestRunMenu(view);
                        if (setUpTheUsbDeviceUvc != null) setUpTheUsbDeviceUvc.showTestRunMenu(view);
                    }
                });
                Button button = activity.findViewById(R.id.raiseSize_setUp);
                button.setEnabled(false); button.setAlpha(0);
                Button button2 = activity.findViewById(R.id.lowerSize_setUp);
                button2.setEnabled(false); button2.setAlpha(0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ScrollView scrollView = activity.findViewById(R.id.scrolli_setup);
                    scrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
                        @Override
                        public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                            final int TIME_TO_WAIT = 2500;
                            Button button = activity.findViewById(R.id.raiseSize_setUp);
                            if (button.isEnabled()) {
                                if (setUpTheUsbDeviceUsbIso != null) {
                                    setUpTheUsbDeviceUsbIso.buttonHandler.removeCallbacks(setUpTheUsbDeviceUsbIso.myRunnable);
                                    setUpTheUsbDeviceUsbIso.buttonHandler.postDelayed(setUpTheUsbDeviceUsbIso.myRunnable, TIME_TO_WAIT);
                                } else {
                                    setUpTheUsbDeviceUvc.buttonHandler.removeCallbacks(setUpTheUsbDeviceUvc.myRunnable);
                                    setUpTheUsbDeviceUvc.buttonHandler.postDelayed(setUpTheUsbDeviceUvc.myRunnable, TIME_TO_WAIT);
                                }

                                return ;
                            }
                            button.setEnabled(true);
                            button.setAlpha(0.8f);
                            Button button2 = activity.findViewById(R.id.lowerSize_setUp);
                            button2.setEnabled(true); button2.setAlpha(0.8f);
                            if (setUpTheUsbDeviceUsbIso != null) {
                                setUpTheUsbDeviceUsbIso.buttonHandler = new Handler();
                                setUpTheUsbDeviceUsbIso.buttonHandler.postDelayed(setUpTheUsbDeviceUsbIso.myRunnable, TIME_TO_WAIT);
                            } else {
                                setUpTheUsbDeviceUvc.buttonHandler = new Handler();
                                setUpTheUsbDeviceUvc.buttonHandler.postDelayed(setUpTheUsbDeviceUvc.myRunnable, TIME_TO_WAIT);
                            }
                        }
                    });
                }
            }
        });
    }

    private boolean checkForNullValues() {
        if (bcdUVC != null) return true;
        else return false;
    }

    public void startEditSave() {
        fetchTheValues();
        activity.setContentView(R.layout.set_up_the_device_configuration);


        sALT_SETTING_text = (TextView) activity.findViewById(R.id.Altsetting);
        sALT_SETTING_text.setText(setColorText("ALT_SETTING:\n", String.format("%s" , sALT_SETTING)), TextView.BufferType.SPANNABLE);
        smaxPacketSize_text = (TextView) activity.findViewById(R.id.MaxPacketSize);
        smaxPacketSize_text.setText(setColorText("MaxPacketSize:\n", String.format("%s" , smaxPacketSize)), TextView.BufferType.SPANNABLE);
        scamFormatIndex_text = (TextView) activity.findViewById(R.id.FormatIndex);
        scamFormatIndex_text.setText(setColorText("FormatIndex:\n", String.format("%s" , scamFormatIndex)), TextView.BufferType.SPANNABLE);
        svideoformat_text = (TextView) activity.findViewById(R.id.svideoformat);
        svideoformat_text.setText(setColorText("Videoformat:\n", String.format("%s" , svideoformat)), TextView.BufferType.SPANNABLE);
        scamFrameIndex_text = (TextView) activity.findViewById(R.id.FrameIndex);
        scamFrameIndex_text.setText(setColorText("FrameIndex:\n", String.format("%s" , scamFrameIndex)), TextView.BufferType.SPANNABLE);
        simageWidth_text = (TextView) activity.findViewById(R.id.ImageWidth);
        simageWidth_text.setText(setColorText("ImageWidth:\n", String.format("%s" , simageWidth)), TextView.BufferType.SPANNABLE);
        simageHeight_text = (TextView) activity.findViewById(R.id.ImageHeight);
        simageHeight_text.setText(setColorText("ImageHeight:\n", String.format("%s" , simageHeight)), TextView.BufferType.SPANNABLE);
        scamFrameInterval_text = (TextView) activity.findViewById(R.id.FrameInterval);
        scamFrameInterval_text.setText(setColorText("Frame_Interval:\n", String.format("%s" , scamFrameInterval)), TextView.BufferType.SPANNABLE);
        spacketsPerRequest_text = (TextView) activity.findViewById(R.id.PacketsPerReq);
        spacketsPerRequest_text.setText(setColorText("PacketsPerRequest:\n", String.format("%s" , spacketsPerRequest)), TextView.BufferType.SPANNABLE);
        sactiveUrbs_text = (TextView) activity.findViewById(R.id.ActiveUrbs);
        sactiveUrbs_text.setText(setColorText("ACTIVE_URBS:\n", String.format("%s" , sactiveUrbs)), TextView.BufferType.SPANNABLE);


        valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
        valueInput.getEditText().setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
                builderSingle.setIcon(R.drawable.ic_menu_camera);
                builderSingle.setTitle("Select the Video Format:");
                //builderSingle.setMessage("Select the maximal size of the Packets, which where sent to the camera device!! Important for Mediathek Devices !!");

                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
                arrayAdapter.add("MJPEG");
                arrayAdapter.add("YUY2");
                arrayAdapter.add("YV12");
                arrayAdapter.add("YUV_422_888");
                arrayAdapter.add("YUV_420_888");
                arrayAdapter.add("UYVY");


                builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = arrayAdapter.getItem(which);
                        if (input == "YUY2") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("YUY2");
                        } else if (input == "MJPEG") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("MJPEG");
                        } else if (input == "YV12") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("YV12");
                        } else if (input == "YUV_422_888") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("YUV_422_888");
                        } else if (input == "YUV_420_888") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("YUV_420_888");
                        } else if (input == "UYVY") {
                            valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                            valueInput.getEditText().setText("UYVY");
                        }
                        System.out.println("svideoformat = " + svideoformat);
                    }
                });
                builderSingle.show();
            }
        });

        Button button_cancle = (Button) activity.findViewById(R.id.button_cancel);
        button_cancle.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                log("cancle");
                returnToMainLayout("No values edited or saved");
                return;
            }
        });
        Button button_ok = (Button) activity.findViewById(R.id.button_ok);
        button_ok.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                log("ok");

                valueInput = (TextInputLayout) activity.findViewById(R.id.alt);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) sALT_SETTING = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.maxP);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) smaxPacketSize = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.Format);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) scamFormatIndex = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) svideoformat = valueInput.getEditText().getText().toString();
                valueInput = (TextInputLayout) activity.findViewById(R.id.Frame);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) scamFrameIndex = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.Imagewi);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) simageWidth = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.ImageHei);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) simageHeight = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.frameInt);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) scamFrameInterval = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.PacketsPer);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) spacketsPerRequest = Integer.parseInt(valueInput.getEditText().getText().toString());
                valueInput = (TextInputLayout) activity.findViewById(R.id.ActiveUr);
                if (valueInput.getEditText().getText().toString().isEmpty() == false) sactiveUrbs = Integer.parseInt(valueInput.getEditText().getText().toString());

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                writeTheValues();
                                //optionForSaveFile = OptionForSaveFile.savetofile;
                                checkTheSaveFileName(OptionForSaveFile.savetofile);
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                writeTheValues();
                                returnToMainLayout("Values edited but not stored to a file");
                                break;
                        }
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setMessage("Do you want to save the values to a file?").setPositiveButton("Yes, Save", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();

            }
        });
        Button button_delete = (Button) activity.findViewById(R.id.button_delete);
        button_delete.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                log("delete button clicked");



                CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
                builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
                builder.setTitle("Delete Savefile:");
                builder.setMessage("Select the file your want to delete:");


                int selectedItem = 0;



                fileName = null;
                name = null;
                rootdirStr = null;
                stringBuilder = new StringBuilder();
                paths = new ArrayList<>(50);




                Context context = activity.getApplicationContext();
                File directory = context.getFilesDir();
                File file = new File(directory, saveFilePathFolder);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        Log.e("TravellerLog :: ", "Problem creating Image folder");
                    }
                }
                rootdirStr = file.toString();
                rootdirStr += "/";
                listFilesForFolder(file);

                if (paths.isEmpty() == true) returnToMainLayout("No save files stored on your Device\n");
                else {
                    for (int i = 0; i < paths.size(); i++) {
                        stringBuilder.append(String.format("%d   ->   ", (i+1)));
                        String entry = paths.get(i);
                        String root = context.getFilesDir().getAbsolutePath();
                        root  += "/";
                        root += saveFilePathFolder ;
                        root += "/";
                        stringBuilder.append(entry.substring(root.length()));
                        stringBuilder.append("\n");
                    }
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
                    builderSingle.setIcon(R.drawable.ic_menu_camera);
                    builderSingle.setTitle("Please select the file to delete");
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
                    for (int i = 0; i < paths.size(); i++) {
                        StringBuilder sb = new StringBuilder();

                        sb.append(paths.get(i).substring(0, (paths.get(i).length())));
                        int end = rootdirStr.length();
                        sb.delete(0, end);
                        arrayAdapter.add(sb.toString());
                    }
                    builderSingle.setNegativeButton("cancel restore", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String strName = arrayAdapter.getItem(which);
                            //String path = rootdirStr;
                            //path += strName;
                            //path+= ".sav";

                            String deleteFile = context.getFilesDir().getAbsolutePath();
                            deleteFile  += "/";
                            deleteFile += saveFilePathFolder;
                            deleteFile += "/";
                            deleteFile += strName;

                            log("deleteFile = " + deleteFile);

                            File file =new File (deleteFile);
                            file.delete();

                            returnToMainLayout("File deleted:\n" + strName);


                            //restoreFromFile(strName);

                        }
                    });
                    builderSingle.show();
                }





/*
                for (int a =0; a<maxPacketsSizeArray.length; a++) {
                    if (maxPacketsSizeArray[a] == smaxPacketSize) selectedItem = a;
                }
                int coise;
                builder.setSingleChoiceItems(maxPacketSizeStr, selectedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                        sALT_SETTING = index +1;
                        System.out.println("sALT_SETTING = " + sALT_SETTING);
                        System.out.println("smaxPacketSize = " + smaxPacketSize);
                    }
                });

                builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int index) {
                        if(sALT_SETTING == 0) sALT_SETTING = index +1;
                        if(smaxPacketSize == 0) smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                        selectPackets(automatic);
                        dialogInterface.dismiss();
                    }
                });
                builder.show();
*/


                return;
            }
        });

    }

    ////// Buttons:  /////////////////


    public void selectVideoFormat (View v) {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Select the Video Format:");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("YUY2");
        arrayAdapter.add("MJPEG");
        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = arrayAdapter.getItem(which);
                if (input == "YUY2") {
                    valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                    valueInput.getEditText().setText("YUY2");
                }
                if (input == "MJPEG") {
                    valueInput = (TextInputLayout) activity.findViewById(R.id.Video);
                    valueInput.getEditText().setText("MJPEG");
                }
                System.out.println("svideoformat = " + svideoformat);
            }
        });
        builderSingle.show();
    }


    ////// Buttons  END //////////

    public void fetchTheValues(){
        if (setUpTheUsbDeviceUsbIso != null) {

        }
        if (setUpTheUsbDeviceUsbIso != null) {
            sALT_SETTING = setUpTheUsbDeviceUsbIso.camStreamingAltSetting;
            svideoformat = setUpTheUsbDeviceUsbIso.videoformat;
            scamFormatIndex = setUpTheUsbDeviceUsbIso.camFormatIndex;
            simageWidth = setUpTheUsbDeviceUsbIso.imageWidth;
            simageHeight = setUpTheUsbDeviceUsbIso.imageHeight;
            scamFrameIndex = setUpTheUsbDeviceUsbIso.camFrameIndex;
            scamFrameInterval = setUpTheUsbDeviceUsbIso.camFrameInterval;
            spacketsPerRequest = setUpTheUsbDeviceUsbIso.packetsPerRequest;
            smaxPacketSize = setUpTheUsbDeviceUsbIso.maxPacketSize;
            sactiveUrbs = setUpTheUsbDeviceUsbIso.activeUrbs;
            sdeviceName = setUpTheUsbDeviceUsbIso.deviceName;
            bUnitID = setUpTheUsbDeviceUsbIso.bUnitID;
            bTerminalID = setUpTheUsbDeviceUsbIso.bTerminalID;
            bNumControlTerminal = setUpTheUsbDeviceUsbIso.bNumControlTerminal;
            bNumControlUnit = setUpTheUsbDeviceUsbIso.bNumControlUnit;
            bcdUVC = setUpTheUsbDeviceUsbIso.bcdUVC;
            bcdUSB = setUpTheUsbDeviceUsbIso.bcdUSB;
            bStillCaptureMethod = setUpTheUsbDeviceUsbIso.bStillCaptureMethod;
            libUsb = setUpTheUsbDeviceUsbIso.libUsb;
            moveToNative = setUpTheUsbDeviceUsbIso.moveToNative;
            bulkMode = setUpTheUsbDeviceUsbIso.bulkMode;

        } else if (setUpTheUsbDeviceUvc != null) {
            sALT_SETTING = setUpTheUsbDeviceUvc.camStreamingAltSetting;
            svideoformat = setUpTheUsbDeviceUvc.videoformat;
            scamFormatIndex = setUpTheUsbDeviceUvc.camFormatIndex;
            simageWidth = setUpTheUsbDeviceUvc.imageWidth;
            simageHeight = setUpTheUsbDeviceUvc.imageHeight;
            scamFrameIndex = setUpTheUsbDeviceUvc.camFrameIndex;
            scamFrameInterval = setUpTheUsbDeviceUvc.camFrameInterval;
            spacketsPerRequest = setUpTheUsbDeviceUvc.packetsPerRequest;
            smaxPacketSize = setUpTheUsbDeviceUvc.maxPacketSize;
            sactiveUrbs = setUpTheUsbDeviceUvc.activeUrbs;
            sdeviceName = setUpTheUsbDeviceUvc.deviceName;
            bUnitID = setUpTheUsbDeviceUvc.bUnitID;
            bTerminalID = setUpTheUsbDeviceUvc.bTerminalID;
            bNumControlTerminal = setUpTheUsbDeviceUvc.bNumControlTerminal;
            bNumControlUnit = setUpTheUsbDeviceUvc.bNumControlUnit;
            bcdUVC = setUpTheUsbDeviceUvc.bcdUVC;
            bcdUSB = setUpTheUsbDeviceUvc.bcdUSB;
            bStillCaptureMethod = setUpTheUsbDeviceUvc.bStillCaptureMethod;
            libUsb = setUpTheUsbDeviceUvc.libUsb;
            moveToNative = setUpTheUsbDeviceUvc.moveToNative;
            bulkMode = setUpTheUsbDeviceUvc.bulkMode;

        } else if (libUsb_autoDetect != null) {
            sALT_SETTING = libUsb_autoDetect.camStreamingAltSetting;
            svideoformat = libUsb_autoDetect.videoformat;
            scamFormatIndex = libUsb_autoDetect.camFormatIndex;
            simageWidth = libUsb_autoDetect.imageWidth;
            simageHeight = libUsb_autoDetect.imageHeight;
            scamFrameIndex = libUsb_autoDetect.camFrameIndex;
            scamFrameInterval = libUsb_autoDetect.camFrameInterval;
            spacketsPerRequest = libUsb_autoDetect.packetsPerRequest;
            smaxPacketSize = libUsb_autoDetect.maxPacketSize;
            sactiveUrbs = libUsb_autoDetect.activeUrbs;
            sdeviceName = libUsb_autoDetect.deviceName;
            bUnitID = libUsb_autoDetect.bUnitID;
            bTerminalID = libUsb_autoDetect.bTerminalID;
            bNumControlTerminal = libUsb_autoDetect.bNumControlTerminal;
            bNumControlUnit = libUsb_autoDetect.bNumControlUnit;
            bcdUVC = libUsb_autoDetect.bcdUVC;
            bcdUSB = libUsb_autoDetect.bcdUSB;
            bStillCaptureMethod = libUsb_autoDetect.bStillCaptureMethod;
            libUsb = libUsb_autoDetect.libUsb;
            moveToNative = libUsb_autoDetect.moveToNative;
            bulkMode = libUsb_autoDetect.bulkMode;

        } else if (jna_autoDetect != null) {
            sALT_SETTING = jna_autoDetect.camStreamingAltSetting;
            svideoformat = jna_autoDetect.videoformat;
            scamFormatIndex = jna_autoDetect.camFormatIndex;
            simageWidth = jna_autoDetect.imageWidth;
            simageHeight = jna_autoDetect.imageHeight;
            scamFrameIndex = jna_autoDetect.camFrameIndex;
            scamFrameInterval = jna_autoDetect.camFrameInterval;
            spacketsPerRequest = jna_autoDetect.packetsPerRequest;
            smaxPacketSize = jna_autoDetect.maxPacketSize;
            sactiveUrbs = jna_autoDetect.activeUrbs;
            sdeviceName = jna_autoDetect.deviceName;
            bUnitID = jna_autoDetect.bUnitID;
            bTerminalID = jna_autoDetect.bTerminalID;
            bNumControlTerminal = jna_autoDetect.bNumControlTerminal;
            bNumControlUnit = jna_autoDetect.bNumControlUnit;
            bcdUVC = jna_autoDetect.bcdUVC;
            bcdUSB = jna_autoDetect.bcdUSB;
            bStillCaptureMethod = jna_autoDetect.bStillCaptureMethod;
            libUsb = jna_autoDetect.libUsb;
            moveToNative = jna_autoDetect.moveToNative;
            bulkMode = jna_autoDetect.bulkMode;
        }
    }

    public void writeTheValues(){
        if (uvc_camera != null) {
            uvc_camera.camStreamingAltSetting = sALT_SETTING;
            uvc_camera.videoformat = svideoformat;
            uvc_camera.camFormatIndex = scamFormatIndex;
            uvc_camera.imageWidth = simageWidth;
            uvc_camera.imageHeight = simageHeight;
            uvc_camera.camFrameIndex = scamFrameIndex;
            uvc_camera.camFrameInterval = scamFrameInterval;
            uvc_camera.packetsPerRequest = spacketsPerRequest;
            uvc_camera.maxPacketSize = smaxPacketSize;
            uvc_camera.activeUrbs = sactiveUrbs;
            uvc_camera.deviceName = sdeviceName;
            uvc_camera.bUnitID = bUnitID;
            uvc_camera.bTerminalID = bTerminalID;
            uvc_camera.bNumControlUnit = bNumControlUnit;
            uvc_camera.bNumControlTerminal = bNumControlTerminal;
            uvc_camera.bcdUVC = bcdUVC;
            uvc_camera.bcdUSB = bcdUSB;
            uvc_camera.bStillCaptureMethod = bStillCaptureMethod;
            uvc_camera.LIBUSB = libUsb;
            uvc_camera.moveToNative = moveToNative;
            uvc_camera.bulkMode = bulkMode;

        } else if (setUpTheUsbDeviceUsbIso != null) {
            setUpTheUsbDeviceUsbIso.camStreamingAltSetting = sALT_SETTING;
            setUpTheUsbDeviceUsbIso.videoformat = svideoformat;
            setUpTheUsbDeviceUsbIso.camFormatIndex = scamFormatIndex;
            setUpTheUsbDeviceUsbIso.imageWidth = simageWidth;
            setUpTheUsbDeviceUsbIso.imageHeight = simageHeight;
            setUpTheUsbDeviceUsbIso.camFrameIndex = scamFrameIndex;
            setUpTheUsbDeviceUsbIso.camFrameInterval = scamFrameInterval;
            setUpTheUsbDeviceUsbIso.packetsPerRequest = spacketsPerRequest;
            setUpTheUsbDeviceUsbIso.maxPacketSize = smaxPacketSize;
            setUpTheUsbDeviceUsbIso.activeUrbs = sactiveUrbs;
            setUpTheUsbDeviceUsbIso.deviceName = sdeviceName;
            setUpTheUsbDeviceUsbIso.bUnitID = bUnitID;
            setUpTheUsbDeviceUsbIso.bTerminalID = bTerminalID;
            setUpTheUsbDeviceUsbIso.bNumControlTerminal = bNumControlTerminal;
            setUpTheUsbDeviceUsbIso.bNumControlUnit = bNumControlUnit;
            setUpTheUsbDeviceUsbIso.bcdUVC = bcdUVC;
            setUpTheUsbDeviceUsbIso.bcdUSB = bcdUSB;
            setUpTheUsbDeviceUsbIso.bStillCaptureMethod = bStillCaptureMethod;
            setUpTheUsbDeviceUsbIso.libUsb = libUsb;
            setUpTheUsbDeviceUsbIso.moveToNative = moveToNative;
            setUpTheUsbDeviceUsbIso.bulkMode = bulkMode;

        } else if (setUpTheUsbDeviceUvc != null) {
            setUpTheUsbDeviceUvc.camStreamingAltSetting = sALT_SETTING;
            setUpTheUsbDeviceUvc.videoformat = svideoformat;
            setUpTheUsbDeviceUvc.camFormatIndex = scamFormatIndex;
            setUpTheUsbDeviceUvc.imageWidth = simageWidth;
            setUpTheUsbDeviceUvc.imageHeight = simageHeight;
            setUpTheUsbDeviceUvc.camFrameIndex = scamFrameIndex;
            setUpTheUsbDeviceUvc.camFrameInterval = scamFrameInterval;
            setUpTheUsbDeviceUvc.packetsPerRequest = spacketsPerRequest;
            setUpTheUsbDeviceUvc.maxPacketSize = smaxPacketSize;
            setUpTheUsbDeviceUvc.activeUrbs = sactiveUrbs;
            setUpTheUsbDeviceUvc.deviceName = sdeviceName;
            setUpTheUsbDeviceUvc.bUnitID = bUnitID;
            setUpTheUsbDeviceUvc.bTerminalID = bTerminalID;
            setUpTheUsbDeviceUvc.bNumControlTerminal = bNumControlTerminal;
            setUpTheUsbDeviceUvc.bNumControlUnit = bNumControlUnit;
            setUpTheUsbDeviceUvc.bcdUVC = bcdUVC;
            setUpTheUsbDeviceUvc.bcdUSB = bcdUSB;
            setUpTheUsbDeviceUvc.bStillCaptureMethod = bStillCaptureMethod;
            setUpTheUsbDeviceUvc.libUsb = libUsb;
            setUpTheUsbDeviceUvc.moveToNative = moveToNative;
            setUpTheUsbDeviceUvc.bulkMode = bulkMode;

        } else if (libUsb_autoDetect != null) {
            libUsb_autoDetect.camStreamingAltSetting = sALT_SETTING;
            libUsb_autoDetect.videoformat = svideoformat;
            libUsb_autoDetect.camFormatIndex = scamFormatIndex;
            libUsb_autoDetect.imageWidth = simageWidth;
            libUsb_autoDetect.imageHeight = simageHeight;
            libUsb_autoDetect.camFrameIndex = scamFrameIndex;
            libUsb_autoDetect.camFrameInterval = scamFrameInterval;
            libUsb_autoDetect.packetsPerRequest = spacketsPerRequest;
            libUsb_autoDetect.maxPacketSize = smaxPacketSize;
            libUsb_autoDetect.activeUrbs = sactiveUrbs;
            libUsb_autoDetect.deviceName = sdeviceName;
            libUsb_autoDetect.bUnitID = bUnitID;
            libUsb_autoDetect.bTerminalID = bTerminalID;
            libUsb_autoDetect.bNumControlTerminal = bNumControlTerminal;
            libUsb_autoDetect.bNumControlUnit = bNumControlUnit;
            libUsb_autoDetect.bcdUVC = bcdUVC;
            libUsb_autoDetect.bcdUSB = bcdUSB;
            libUsb_autoDetect.bStillCaptureMethod = bStillCaptureMethod;
            libUsb_autoDetect.libUsb = libUsb;
            libUsb_autoDetect.moveToNative = moveToNative;
            libUsb_autoDetect.bulkMode = bulkMode;


        } else if (jna_autoDetect != null) {
            jna_autoDetect.camStreamingAltSetting = sALT_SETTING;
            jna_autoDetect.videoformat = svideoformat;
            jna_autoDetect.camFormatIndex = scamFormatIndex;
            jna_autoDetect.imageWidth = simageWidth;
            jna_autoDetect.imageHeight = simageHeight;
            jna_autoDetect.camFrameIndex = scamFrameIndex;
            jna_autoDetect.camFrameInterval = scamFrameInterval;
            jna_autoDetect.packetsPerRequest = spacketsPerRequest;
            jna_autoDetect.maxPacketSize = smaxPacketSize;
            jna_autoDetect.activeUrbs = sactiveUrbs;
            jna_autoDetect.deviceName = sdeviceName;
            jna_autoDetect.bUnitID = bUnitID;
            jna_autoDetect.bTerminalID = bTerminalID;
            jna_autoDetect.bNumControlTerminal = bNumControlTerminal;
            jna_autoDetect.bNumControlUnit = bNumControlUnit;
            jna_autoDetect.bcdUVC = bcdUVC;
            jna_autoDetect.bcdUSB = bcdUSB;
            jna_autoDetect.bStillCaptureMethod = bStillCaptureMethod;
            jna_autoDetect.libUsb = libUsb;
            jna_autoDetect.moveToNative = moveToNative;
            jna_autoDetect.bulkMode = bulkMode;

        }
    }


    public void restoreValuesFromFile() {
        checkTheSaveFileName(OptionForSaveFile.restorefromfile);
    }


    private void checkTheSaveFileName(final OptionForSaveFile option){
        fileName = null;
        name = null;
        rootdirStr = null;
        stringBuilder = new StringBuilder();
        paths = new ArrayList<>(50);

        Context context = activity.getApplicationContext();
        File directory = context.getFilesDir();
        File file = new File(directory, saveFilePathFolder);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
            }
        }
        rootdirStr = file.toString();
        rootdirStr += "/";
        listFilesForFolder(file);
        if (paths.isEmpty() == true && optionForSaveFile == OptionForSaveFile.restorefromfile) {
            returnToMainLayout(String.format("No savefiles found in the save directory.\nDirectory: &s", file.getAbsolutePath() ));
        } else {
            stringBuilder.append("Click on 'ok' to auto select a name, or type the number infront of a shown file, or type in a unique name.\n");
            for (int i = 0; i < paths.size(); i++) {
                stringBuilder.append(String.format("%d   ->   ", (i+1)));
                String entry = paths.get(i);
                String root = context.getFilesDir().getAbsolutePath();
                root  += "/";
                root += saveFilePathFolder ;
                root += "/";
                stringBuilder.append(entry.substring(root.length()));
                stringBuilder.append("\n");
            }
            //log(stringBuilder.toString());
            if (option == OptionForSaveFile.restorefromfile) {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
                builderSingle.setIcon(R.drawable.ic_menu_camera);
                builderSingle.setTitle("Please select the file to restore");
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
                for (int i = 0; i < paths.size(); i++) {
                    StringBuilder sb = new StringBuilder();

                    sb.append(paths.get(i).substring(0, (paths.get(i).length())));
                    int end = rootdirStr.length();
                    sb.delete(0, end);
                    arrayAdapter.add(sb.toString());
                }
                builderSingle.setNegativeButton("cancel restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strName = arrayAdapter.getItem(which);
                        restoreFromFile(strName);
                    }
                });
                builderSingle.show();
            } else {
                AlertDialog.Builder builder2 = new AlertDialog.Builder(mContext);
                builder2.setTitle("Input a value");
                builder2.setMessage(stringBuilder.toString());
// Set up the input
                final EditText input = new EditText(mContext);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                builder2.setView(input);
// Set up the buttons
                builder2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        log("OK");
                        if (input.getText().equals(null))  {
                            name = sdeviceName;
                        } else {
                            log("You entered: " + input.getText().toString());
                            name = input.getText().toString();
                        }
                        if (name.isEmpty() == true) {
                            switch(option) {
                                case savetofile:
                                    if (sdeviceName != null) {
                                        saveValuesToFile(sdeviceName);
                                        name = sdeviceName;
                                        break;
                                    }
                                    else saveValuesToFile("Random");
                                    name = "Random";
                                    log("sdeviceName = " + sdeviceName);
                                    break;
                                case restorefromfile:
                                    if (sdeviceName != null) restoreFromFile(sdeviceName);
                                    break;
                            }
                        }
                        else if (isInteger(name) == true) {
                            if (Integer.parseInt(name) > paths.size()) {
                                alertView("Number too high", "Your input number was higher than the number of files which were existing in the save path folder!\n\nIf you input a number, than an existing file will be choosen!\n\n Solutions:\n\nLower the number to fit a file,\nor type in a word,\nor leave the field blanc.");
                                //displayMessage("Number too high!\n\nIf you input a number, than an existing file will be choosen!\n\nYour input number was higher than the number of file wich exists");
                                return;
                            }
                            switch(option) {
                                case savetofile:
                                    fileName = paths.get((Integer.parseInt(name) - 1));
                                    log("fileName = " + fileName);
                                    String root = context.getFilesDir().getAbsolutePath();
                                    root  += "/";
                                    root += saveFilePathFolder ;
                                    root += "/";
                                    try { saveValuesToFile(fileName.substring(root.length())); }
                                    catch (Exception e) { log("Save Failed ; Exception = " + e); e.printStackTrace();}
                                    name = fileName.substring(root.length());
                                    break;
                                case restorefromfile:
                                    restoreFromFile(paths.get((Integer.parseInt(name) - 1)));
                                    break;
                            }
                        } else {
                            switch(option) {
                                case savetofile:
                                    saveValuesToFile((name));      log("Saving ...");
                                    break;
                                case restorefromfile:
                                    restoreFromFile(( name ));      log("Saving ...");
                                    break;
                            }
                        }
                        switch(option) {
                            case savetofile:
                                String rootdirString = file.toString();
                                rootdirString += "/";
                                String fileN;
                                if (fileName != null) {
                                    int index = fileName.length()- (fileName.length() - rootdirString.length());
                                    fileN = fileName.substring(index, fileName.length());
                                } else {
                                    int index = rootdirStr.length()- (rootdirStr.length() - rootdirString.length());
                                    fileN = rootdirStr.substring(index, rootdirStr.length());
                                }
                                returnToMainLayout("Values written and saved:\n\nName of the savefile:\n" + name + "\n\nFolder of the savefile:\n" + activity.getApplicationContext().getFilesDir().getAbsolutePath() );
                                break;
                            case restorefromfile:
                                returnToMainLayout(String.format("Values sucessfully restored"));
                                break;
                        }
                        return;
                    }
                });
                builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(option) {
                            case savetofile:
                                returnToMainLayout("Values written but not saved.");
                                break;
                            case restorefromfile:
                                returnToMainLayout(String.format("No values restored"));
                                break;
                        }
                        return;
                    }
                });
                builder2.show();
            }
        }
    }


    public void saveValuesToFile (String savePath) {

        Context context = activity.getApplicationContext();
        File directory = context.getFilesDir();
        File saveDir = new File(directory, saveFilePathFolder);

        log("saveName = " + savePath);
        try {  // Catch errors in I/O if necessary.
            File file = new File(saveDir, savePath);
            //file = new File(savePath).getAbsoluteFile();
            log("AbsolutePath = " + file.getAbsolutePath());
            //file.getParentFile().mkdirs();
            if (file.exists())  file.delete();
            FileOutputStream saveFile=new FileOutputStream(file.toString());
            ObjectOutputStream save = new ObjectOutputStream(saveFile);
            save.writeObject(sALT_SETTING);
            save.writeObject(svideoformat);
            save.writeObject(scamFormatIndex);
            save.writeObject(scamFrameIndex);
            save.writeObject(simageWidth);
            save.writeObject(simageHeight);
            save.writeObject(scamFrameInterval);
            save.writeObject(spacketsPerRequest);
            save.writeObject(smaxPacketSize);
            save.writeObject(sactiveUrbs);
            save.writeObject(saveFilePathFolder);
            save.writeObject(sdeviceName);
            save.writeObject(bUnitID);
            save.writeObject(bTerminalID);
            save.writeObject(bNumControlTerminal);
            save.writeObject(bNumControlUnit);
            save.writeObject(bStillCaptureMethod);
            save.writeObject(libUsb);
            save.writeObject(moveToNative);
            save.writeObject(bcdUVC);
            save.writeObject(bulkMode);
            if (bcdUSB[0] == 3) save.writeObject(bcdUSB);
            save.close(); // This also closes saveFile.
        } catch (Exception e) { log("Error"); e.printStackTrace();}
        returnToMainLayout(String.format("Values edited and saved\nSavefile = %s", savePath));
    }

    public void restoreFromFile(String pathToFile){
        Context context = activity.getApplicationContext();
        File directory = context.getFilesDir();
        File saveDir = new File(directory, saveFilePathFolder);
        try{
            FileInputStream saveFile = new FileInputStream(new File(saveDir, pathToFile));
            ObjectInputStream save = new ObjectInputStream(saveFile);
            sALT_SETTING = (Integer) save.readObject();
            svideoformat = (String) save.readObject();
            scamFormatIndex  = (Integer) save.readObject();
            scamFrameIndex  = (Integer) save.readObject();
            simageWidth = (Integer) save.readObject();
            simageHeight = (Integer) save.readObject();
            scamFrameInterval  = (Integer) save.readObject();
            spacketsPerRequest  = (Integer) save.readObject();
            smaxPacketSize  = (Integer) save.readObject();
            sactiveUrbs  = (Integer) save.readObject();
            saveFilePathFolder  = (String) save.readObject();
            sdeviceName = (String) save.readObject();
            bUnitID  = (Byte) save.readObject();
            bTerminalID  = (Byte) save.readObject();
            bNumControlTerminal  = (byte[]) save.readObject();
            bNumControlUnit  = (byte[]) save.readObject();
            bStillCaptureMethod = (Byte) save.readObject();
            libUsb = (Boolean) save.readObject();
            moveToNative = (Boolean) save.readObject();
            bcdUVC  = (byte[]) save.readObject();
            bulkMode = (Boolean) save.readObject();
            save.close();
        }
        catch(Exception exc){
            exc.printStackTrace();
        }
        if (uvc_camera != null ) {
            ToggleButton libUsbActivate = activity.findViewById(R.id.libusbToggleButton);
            if (!libUsbActivate.isEnabled()) {
                if (uvc_camera.LIBUSB != libUsb) {
                    StringBuilder sb = new StringBuilder();
                    if (!uvc_camera.LIBUSB) sb.append(String.format("Restoring not possible! (Libusb Driver is needed, but not enabled)\n\nRestart the app to solve the issue"));
                    else sb.append(String.format("Restoring not possible! (Libusb Driver is enabled, but is not needed)\n\nRestart the app to solve the issue"));
                    libUsb = uvc_camera.LIBUSB;
                    writeMsgMain(sb.toString());
                    return;
                }
            }
        }
        log("sALT_SETTING = " + sALT_SETTING + "  /  svideoformat = " + svideoformat + "  /  scamFormatIndex = " + scamFormatIndex + "  /  scamFrameIndex = " + scamFrameIndex);
        writeTheValues();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Restored Values:\n"));
        if (scamFrameInterval != 0) sb.append(String.format("Altsetting = %d\nVideoFormat = %s\nImageWidth = %d\nImageHeight = %d\nFrameInterval = %d\nPacketsPerRequest = %d\nActiveUrbs = %d", sALT_SETTING, svideoformat, simageWidth, simageHeight, (10000000 / scamFrameInterval), spacketsPerRequest, sactiveUrbs));
        else sb.append(String.format("Altsetting = %d\nVideoFormat = %s\nImageWidth = %d\nImageHeight = %d\nPacketsPerRequest = %d\nActiveUrbs = %d", sALT_SETTING, svideoformat, simageWidth, simageHeight, spacketsPerRequest, sactiveUrbs));
        setTextViewMain();
        writeMsgMain(sb.toString());
    }

    public static boolean isInteger(String s) {
        return isInteger(s,10);
    }

    public static boolean isInteger(String s, int radix) {
        if(s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if(Character.digit(s.charAt(i),radix) < 0) return false;
        }
        return true;
    }

    private void displayMessage(final String msg) {
        if (uvc_camera != null)  uvc_camera.displayMessage(msg);
        else if (setUpTheUsbDeviceUsbIso != null) setUpTheUsbDeviceUsbIso.displayMessage(msg);
        else setUpTheUsbDeviceUvc.displayMessage(msg);
    }

    private void log(String msg) {
        Log.i("SaveToFile", msg);
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
                paths.add(fileEntry.toString());
            }
        }
    }

    public void setUpWithUvcValues(UVC_Descriptor uvc_desc, int[] maxPacketSizeArray, boolean automatic) {
        fetchTheValues();
        this.uvc_descriptor = uvc_desc;
        bUnitID = uvc_desc.bUnitID;
        bTerminalID = uvc_desc.bTerminalID;
        bNumControlTerminal = uvc_desc.bNumControlTerminal;
        bNumControlUnit = uvc_desc.bNumControlUnit;
        bcdUVC = uvc_desc.bcdUVC;
        bcdUSB = uvc_desc.bcdUSB;
        log ("bcdUSB = " + bcdUSB[0] + "." + bcdUSB[1]);
        if (bcdUSB[0] == 2) log ("bcdUVC = " + bcdUVC[0] + bcdUVC[1]);
        if (bcdUSB[0] == 3) {
        }
        bStillCaptureMethod = uvc_desc.bStillCaptureMethod;
        UVC_Descriptor.FormatIndex formatIndex;
        int [] arrayFormatFrameIndexes = new int [uvc_descriptor.formatIndex.size()];
        for (int i=0; i<uvc_descriptor.formatIndex.size(); i++) {
            formatIndex = uvc_descriptor.getFormatIndex(i);
            arrayFormatFrameIndexes[i] = formatIndex.frameIndex.size();
        }
        if (moveToNative) {
            maxPacketSizeStr = new String [uvc_descriptor.maxPacketSizeArray.size()];
            if (setUpTheUsbDeviceUsbIso != null) {
                setUpTheUsbDeviceUsbIso.convertedMaxPacketSize = new int[uvc_descriptor.maxPacketSizeArray.size()];
                for (int a =0; a<uvc_descriptor.maxPacketSizeArray.size(); a++) {
                    setUpTheUsbDeviceUsbIso.convertedMaxPacketSize[a] = uvc_descriptor.maxPacketSizeArray.get(a);
                    maxPacketSizeStr[a] = Integer.toString(uvc_descriptor.maxPacketSizeArray.get(a));
                }
            } else {
                setUpTheUsbDeviceUvc.convertedMaxPacketSize = new int[uvc_descriptor.maxPacketSizeArray.size()];
                for (int a =0; a<uvc_descriptor.maxPacketSizeArray.size(); a++) {
                    setUpTheUsbDeviceUvc.convertedMaxPacketSize[a] = uvc_descriptor.maxPacketSizeArray.get(a);
                    maxPacketSizeStr[a] = Integer.toString(uvc_descriptor.maxPacketSizeArray.get(a));
                }
            }

        } else {
            maxPacketSizeStr = new String [maxPacketSizeArray.length];
            for (int a =0; a<maxPacketSizeArray.length; a++) {
                maxPacketSizeStr[a] = Integer.toString(maxPacketSizeArray[a]);
            }
        }
        if (!automatic)  selectMaxPacketSize(automatic);
        else {
            selectMaxPacketSize(true);
        }
    }

    public void setUpWithUvcValues_libusb(final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info, int[] maxPacketSizeArray) {
        fetchTheValues();

        if(uvc_device_info == null) return;
        //if (uvc_device_info.ctrl_if.processing_unit_descs.bUnitID != 0)  bUnitID = uvc_device_info.ctrl_if.processing_unit_descs.bUnitID;
        //bTerminalID = uvc_device_info.ctrl_if.output_term_descs.bTerminalID;
        //bNumControlTerminal = uvc_desc.bNumControlTerminal;
        //bNumControlUnit = uvc_desc.bNumControlUnit;
        //bcdUVC = uvc_desc.bcdUVC;
        //bcdUSB = uvc_desc.bcdUSB;
        /*
        log ("bcdUSB = " + bcdUSB[0] + "." + bcdUSB[1]);
        if (bcdUSB[0] == 2) log ("bcdUVC = " + bcdUVC[0] + bcdUVC[1]);
        if (bcdUSB[0] == 3) {
        }
        */
        bStillCaptureMethod = uvc_device_info.stream_ifs.bStillCaptureMethod;
        /*
        UVC_Descriptor.FormatIndex formatIndex;
        int [] arrayFormatFrameIndexes = new int [uvc_descriptor.formatIndex.size()];
        for (int i=0; i<uvc_descriptor.formatIndex.size(); i++) {
            formatIndex = uvc_descriptor.getFormatIndex(i);
            arrayFormatFrameIndexes[i] = formatIndex.frameIndex.size();
        }
        */


        if (moveToNative) {
            maxPacketSizeStr = new String [maxPacketSizeArray.length];
            if (setUpTheUsbDeviceUsbIso != null) {
                setUpTheUsbDeviceUsbIso.convertedMaxPacketSize = new int[maxPacketSizeArray.length];
                for (int a =0; a<maxPacketSizeArray.length; a++) {
                    //setUpTheUsbDeviceUsbIso.convertedMaxPacketSize[a] = maxPacketSizeArray[a];
                    maxPacketSizeStr[a] = Integer.toString(maxPacketSizeArray[a]);
                }
            } else {
                setUpTheUsbDeviceUvc.convertedMaxPacketSize = new int[uvc_descriptor.maxPacketSizeArray.size()];
                for (int a =0; a<uvc_descriptor.maxPacketSizeArray.size(); a++) {
                   // setUpTheUsbDeviceUvc.convertedMaxPacketSize[a] = uvc_descriptor.maxPacketSizeArray.get(a);
                    maxPacketSizeStr[a] = Integer.toString(maxPacketSizeArray[a]);
                }
            }

        } else {
            maxPacketSizeStr = new String [maxPacketSizeArray.length];
            for (int a =0; a<maxPacketSizeArray.length; a++) {
                maxPacketSizeStr[a] = Integer.toString(maxPacketSizeArray[a]);
            }
        }


        selectMaxPacketSize(uvc_device_info);

    }

    private void selectMaxPacketSize(boolean automatic){
        if (!automatic) {
            int[] maxPacketsSizeArray;
            if (setUpTheUsbDeviceUsbIso != null) maxPacketsSizeArray = setUpTheUsbDeviceUsbIso.convertedMaxPacketSize.clone();
            else maxPacketsSizeArray = setUpTheUsbDeviceUvc.convertedMaxPacketSize.clone();
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select the maximal Packet Size:");
            builder.setMessage("Your current MaxPacketSize: " + smaxPacketSize);
            int selectedItem = 0;
            for (int a =0; a<maxPacketsSizeArray.length; a++) {
                if (maxPacketsSizeArray[a] == smaxPacketSize) selectedItem = a;
            }
            int coise;
            builder.setSingleChoiceItems(maxPacketSizeStr, selectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                    sALT_SETTING = index +1;
                    System.out.println("sALT_SETTING = " + sALT_SETTING);
                    System.out.println("smaxPacketSize = " + smaxPacketSize);
                }
            });

            builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    if(sALT_SETTING == 0) sALT_SETTING = index +1;
                    if(smaxPacketSize == 0) smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                    selectPackets(automatic);
                    dialogInterface.dismiss();
                }
            });
            builder.show();

        } else {
            // Select highest Max Packet Size
            if (!highestMaxPacketSizeDone) {
                int[] maxPacketsSizeArray;
                if (setUpTheUsbDeviceUsbIso != null) maxPacketsSizeArray = setUpTheUsbDeviceUsbIso.convertedMaxPacketSize.clone();
                if (setUpTheUsbDeviceUvc != null) maxPacketsSizeArray = setUpTheUsbDeviceUvc.convertedMaxPacketSize.clone();
                else if (libUsb_autoDetect != null) maxPacketsSizeArray = libUsb_autoDetect.convertedMaxPacketSize.clone();
                else if (jna_autoDetect != null) maxPacketsSizeArray = jna_autoDetect.convertedMaxPacketSize.clone();
                else return;

                // find greatest MaxPacketSize:
                int maxValue = maxPacketsSizeArray[0];
                int maxPos = 0;
                for (int i = 0; i < maxPacketsSizeArray.length; i++) {
                    if (maxPacketsSizeArray[i] > maxValue) {
                        maxValue = maxPacketsSizeArray[i];
                        maxPos = i;
                    }
                }
                sALT_SETTING = (maxPos + 1);
                smaxPacketSize = maxPacketsSizeArray[maxPos];
                System.out.println("smaxPacketSize = " + smaxPacketSize);
                System.out.println("sALT_SETTING = " + sALT_SETTING);
                selectPackets(true);
                return;
            }
        }
    }

    private void selectMaxPacketSize(final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info){
        int[] maxPacketsSizeArray;
        if (setUpTheUsbDeviceUsbIso != null) maxPacketsSizeArray = setUpTheUsbDeviceUsbIso.convertedMaxPacketSize.clone();
        else maxPacketsSizeArray = setUpTheUsbDeviceUvc.convertedMaxPacketSize.clone();
        CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
        builder.setTitle("Select the maximal Packet Size:");
        builder.setMessage("Your current MaxPacketSize: " + smaxPacketSize);
        int selectedItem = 0;
        for (int a =0; a<maxPacketsSizeArray.length; a++) {
            if (maxPacketsSizeArray[a] == smaxPacketSize) selectedItem = a;
        }
        int coise;
        builder.setSingleChoiceItems(maxPacketSizeStr, selectedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                sALT_SETTING = index +1;
                System.out.println("sALT_SETTING = " + sALT_SETTING);
                System.out.println("smaxPacketSize = " + smaxPacketSize);
            }
        });

        builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                if(sALT_SETTING == 0) sALT_SETTING = index +1;
                if(smaxPacketSize == 0) smaxPacketSize = Integer.parseInt(maxPacketSizeStr[index]);
                selectPackets(uvc_device_info);
                dialogInterface.dismiss();
            }
        });
        builder.show();

    }

    public void selectPackets(boolean automatic) {
        if (!automatic) {

            CFAlertDialog alertDialog;
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            LayoutInflater li = LayoutInflater.from(mContext);
            View stf_select_max_package_layout_view = li.inflate(R.layout.stf_select_max_package_layout, null);
            builder.setHeaderView(stf_select_max_package_layout_view);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select your Packets per Request");
            builder.setMessage("Your current Value = " + spacketsPerRequest);
            final EditText input = new EditText(mContext);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            builder.setFooterView(input);
            builder.addButton("Done", Color.parseColor("#FFFFFF"), Color.parseColor("#429ef4"), CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (input.getText().toString().isEmpty() == false) {
                        spacketsPerRequest = Integer.parseInt(input.getText().toString());
                        selectUrbs(automatic);
                        dialog.dismiss();
                    }
                    else if (spacketsPerRequest != 0) {
                        selectUrbs(automatic);
                        dialog.dismiss();
                    }
                    else displayMessage("Select a Number, or type in a Value");
                }
            });
            alertDialog = builder.show();
            TextView tv = stf_select_max_package_layout_view.findViewById(R.id.textView_setPackets);
            tv.setText("One Packets has a size of " + smaxPacketSize + " bytes");
            CFPushButton packetOne = stf_select_max_package_layout_view.findViewById(R.id.one) ;
            packetOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 1;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
            CFPushButton packetTwo = stf_select_max_package_layout_view.findViewById(R.id.two) ;
            packetTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 2;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
            CFPushButton packetThree = stf_select_max_package_layout_view.findViewById(R.id.three) ;
            packetThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 4;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
            CFPushButton packetFour = stf_select_max_package_layout_view.findViewById(R.id.four) ;
            packetFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 8;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
            CFPushButton packetFive = stf_select_max_package_layout_view.findViewById(R.id.five) ;
            packetFive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 16;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
            CFPushButton packetSix = stf_select_max_package_layout_view.findViewById(R.id.six) ;
            packetSix.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 32;
                    alertDialog.dismiss();
                    selectUrbs(false);
                }
            });
        } else {
            //if (raisePacketsPerRequest) spacketsPerRequest ++;
            selectUrbs(true);
        }
    }

    public void selectPackets(final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info) {
            CFAlertDialog alertDialog;
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            LayoutInflater li = LayoutInflater.from(mContext);
            View stf_select_max_package_layout_view = li.inflate(R.layout.stf_select_max_package_layout, null);
            builder.setHeaderView(stf_select_max_package_layout_view);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select your Packets per Request");
            builder.setMessage("Your current Value = " + spacketsPerRequest);
            final EditText input = new EditText(mContext);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            builder.setFooterView(input);
            builder.addButton("Done", Color.parseColor("#FFFFFF"), Color.parseColor("#429ef4"), CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (input.getText().toString().isEmpty() == false) {
                        spacketsPerRequest = Integer.parseInt(input.getText().toString());
                        selectUrbs(uvc_device_info);
                        dialog.dismiss();
                    }
                    else if (spacketsPerRequest != 0) {
                        selectUrbs(uvc_device_info);
                        dialog.dismiss();
                    }
                    else displayMessage("Select a Number, or type in a Value");
                }
            });
            alertDialog = builder.show();
            TextView tv = stf_select_max_package_layout_view.findViewById(R.id.textView_setPackets);
            tv.setText("One Packets has a size of " + smaxPacketSize + " bytes");
            CFPushButton packetOne = stf_select_max_package_layout_view.findViewById(R.id.one) ;
            packetOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 1;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
            CFPushButton packetTwo = stf_select_max_package_layout_view.findViewById(R.id.two) ;
            packetTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 2;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
            CFPushButton packetThree = stf_select_max_package_layout_view.findViewById(R.id.three) ;
            packetThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 4;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
            CFPushButton packetFour = stf_select_max_package_layout_view.findViewById(R.id.four) ;
            packetFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 8;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
            CFPushButton packetFive = stf_select_max_package_layout_view.findViewById(R.id.five) ;
            packetFive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 16;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
            CFPushButton packetSix = stf_select_max_package_layout_view.findViewById(R.id.six) ;
            packetSix.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spacketsPerRequest = 32;
                    alertDialog.dismiss();
                    selectUrbs(uvc_device_info);
                }
            });
    }

    public void selectUrbs(boolean automatic) {
        if (!automatic) {
            CFAlertDialog alertDialog;
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            LayoutInflater li = LayoutInflater.from(mContext);
            View stf_select_max_package_layout_view = li.inflate(R.layout.stf_select_active_urbs, null);
            builder.setHeaderView(stf_select_max_package_layout_view);

            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select your active Usb Request Blocks");
            builder.setMessage("Your current Value = " + sactiveUrbs);
            final EditText input = new EditText(mContext);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            builder.setFooterView(input);
            builder.addButton("Done", Color.parseColor("#FFFFFF"), Color.parseColor("#429ef4"), CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (input.getText().toString().isEmpty() == false) {
                        sactiveUrbs = Integer.parseInt(input.getText().toString());
                        selectFormatIndex(automatic);
                        dialog.dismiss();
                    }
                    else if (sactiveUrbs != 0) {
                        selectFormatIndex(automatic);
                        dialog.dismiss();
                    }
                    else displayMessage("Select a Number, or type in a Value");
                }
            });
            alertDialog = builder.show();
            TextView tv = stf_select_max_package_layout_view.findViewById(R.id.textView_setUrbs);
            tv.setText("One Usb Request Block has a size of " + smaxPacketSize + " x " + spacketsPerRequest +" bytes");
            CFPushButton packetOne = stf_select_max_package_layout_view.findViewById(R.id.uone) ;
            packetOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log("selectUrbs --> one clicked");
                    sactiveUrbs = 1;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                    log("selectUrbs --> one end");

                }
            });
            CFPushButton packetTwo = stf_select_max_package_layout_view.findViewById(R.id.utwo) ;
            packetTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 2;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                }
            });
            CFPushButton packetThree = stf_select_max_package_layout_view.findViewById(R.id.uthree) ;
            packetThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 4;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                }
            });
            CFPushButton packetFour = stf_select_max_package_layout_view.findViewById(R.id.ufour) ;
            packetFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 8;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                }
            });
            CFPushButton packetFive = stf_select_max_package_layout_view.findViewById(R.id.ufive) ;
            packetFive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 16;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                }
            });
            CFPushButton packetSix = stf_select_max_package_layout_view.findViewById(R.id.usix) ;
            packetSix.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 32;
                    alertDialog.dismiss();
                    selectFormatIndex(automatic);
                }
            });
        } else {
            //if (raiseActiveUrbs) sactiveUrbs ++;
            selectFormatIndex(automatic);
        }
    }

    public void selectUrbs(final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info) {
            CFAlertDialog alertDialog;
            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            LayoutInflater li = LayoutInflater.from(mContext);
            View stf_select_max_package_layout_view = li.inflate(R.layout.stf_select_active_urbs, null);
            builder.setHeaderView(stf_select_max_package_layout_view);

            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select your active Usb Request Blocks");
            builder.setMessage("Your current Value = " + sactiveUrbs);
            final EditText input = new EditText(mContext);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            builder.setFooterView(input);
            builder.addButton("Done", Color.parseColor("#FFFFFF"), Color.parseColor("#429ef4"), CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (input.getText().toString().isEmpty() == false) {
                        sactiveUrbs = Integer.parseInt(input.getText().toString());
                        selectFormatIndex(uvc_device_info);
                        dialog.dismiss();
                    }
                    else if (sactiveUrbs != 0) {
                        selectFormatIndex(uvc_device_info);
                        dialog.dismiss();
                    }
                    else displayMessage("Select a Number, or type in a Value");
                }
            });
            alertDialog = builder.show();
            TextView tv = stf_select_max_package_layout_view.findViewById(R.id.textView_setUrbs);
            tv.setText("One Usb Request Block has a size of " + smaxPacketSize + " x " + spacketsPerRequest +" bytes");
            CFPushButton packetOne = stf_select_max_package_layout_view.findViewById(R.id.uone) ;
            packetOne.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    log("selectUrbs --> one clicked");
                    sactiveUrbs = 1;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                    log("selectUrbs --> one end");

                }
            });
            CFPushButton packetTwo = stf_select_max_package_layout_view.findViewById(R.id.utwo) ;
            packetTwo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 2;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                }
            });
            CFPushButton packetThree = stf_select_max_package_layout_view.findViewById(R.id.uthree) ;
            packetThree.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 4;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                }
            });
            CFPushButton packetFour = stf_select_max_package_layout_view.findViewById(R.id.ufour) ;
            packetFour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 8;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                }
            });
            CFPushButton packetFive = stf_select_max_package_layout_view.findViewById(R.id.ufive) ;
            packetFive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 16;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                }
            });
            CFPushButton packetSix = stf_select_max_package_layout_view.findViewById(R.id.usix) ;
            packetSix.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sactiveUrbs = 32;
                    alertDialog.dismiss();
                    selectFormatIndex(uvc_device_info);
                }
            });
    }

    public void selectFormatIndex (boolean automatic) {
        if (!automatic) {
            log("formatIndex");
            numberFormatIndexes = new int[uvc_descriptor.formatIndex.size()];
            final String[] textmsg = new String[uvc_descriptor.formatIndex.size()];
            for (int a = 0; a < uvc_descriptor.formatIndex.size(); a++) {
                formatIndex = uvc_descriptor.getFormatIndex(a);
                System.out.println("formatIndex.videoformat = " + formatIndex.videoformat);
                numberFormatIndexes[a] = formatIndex.formatIndexNumber;
                System.out.println("numberFormatIndexes[" + a + "] = " + numberFormatIndexes[a]);
                textmsg[a] = formatIndex.videoformat.toString();
            }
            final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select the Camera Format (MJPEG / YUV / ...)");
            if (svideoformat != null) builder.setMessage("Your current format: " + svideoformat);
            int selectedItem = 0;
            for (int a =0; a<numberFormatIndexes.length; a++) {
                if (numberFormatIndexes[a] == scamFormatIndex) selectedItem = a;
            }
            final int startvalue = selectedItem;
            builder.setSingleChoiceItems(textmsg, startvalue, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    scamFormatIndex = numberFormatIndexes[index];
                    formatIndex = uvc_descriptor.getFormatIndex(index);
                    svideoformat = formatIndex.videoformat.toString();
                    log("svideoformat = " + svideoformat);
                    log("index = " + index);
                    selectFrameIndex(automatic);
                    dialogInterface.dismiss();
                }
            });
            builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    log("index = " + index);
                    scamFormatIndex = numberFormatIndexes[index];
                    formatIndex = uvc_descriptor.getFormatIndex(index);
                    svideoformat = formatIndex.videoformat.toString();
                    log("formatIndex = " + formatIndex.videoformat.toString());
                    log("svideoformat = " + svideoformat);
                    log("index = " + index);
                    selectFrameIndex(automatic);
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        } else {
            numberFormatIndexes = new int[uvc_descriptor.formatIndex.size()];
            final String[] textmsg = new String[uvc_descriptor.formatIndex.size()];
            for (int a = 0; a < uvc_descriptor.formatIndex.size(); a++) {
                formatIndex = uvc_descriptor.getFormatIndex(a);
                System.out.println("formatIndex.videoformat = " + formatIndex.videoformat);
                numberFormatIndexes[a] = formatIndex.formatIndexNumber;
                System.out.println("numberFormatIndexes[" + a + "] = " + numberFormatIndexes[a]);
                textmsg[a] = formatIndex.videoformat.toString();
            }
            if (uvc_descriptor.formatIndex.size() == 1) {
                numberFormatIndexes = new int[uvc_descriptor.formatIndex.size()];
                scamFormatIndex = 1;
                formatIndex = uvc_descriptor.getFormatIndex(0);
                svideoformat = formatIndex.videoformat.toString();
                selectFrameIndex(automatic);
                return;
            } else {
                for (int a = 0; a < uvc_descriptor.formatIndex.size(); a++) {
                    if (textmsg[a].equals("MJPEG")) {
                        scamFormatIndex = numberFormatIndexes[a];
                        formatIndex = uvc_descriptor.getFormatIndex(a);
                        svideoformat = formatIndex.videoformat.toString();
                        System.out.println("svideoformat = " + svideoformat);
                        selectFrameIndex(automatic);
                        return;
                    }
                }
                for (int a = 0; a < uvc_descriptor.formatIndex.size(); a++) {
                    if (textmsg[a].equals("YUV") || textmsg[a].equals("YUY2") || textmsg[a].equals("YV12") || textmsg[a].equals("YUV_422_888") || textmsg[a].equals("YUV_420_888")) {
                        scamFormatIndex = numberFormatIndexes[a];
                        formatIndex = uvc_descriptor.getFormatIndex(a);
                        svideoformat = formatIndex.videoformat.toString();
                        System.out.println("svideoformat = " + svideoformat);
                        selectFrameIndex(automatic);
                        return;
                    }
                }
            }
        }
    }

    public void selectFormatIndex (final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info) {
        log("formatIndex");
        JNA_I_LibUsb.uvc_format_desc uvc_format_desc;
        uvc_format_desc = uvc_device_info.stream_ifs.format_descs;
        int numberOfFormatDescriptors = 0;
        while (uvc_format_desc != null ) {
            numberOfFormatDescriptors ++;
            //streamInterfaceEntries.append("\n");
            //streamInterfaceEntries.append("FormatDescriptor " + uvc_format_desc.bFormatIndex + "\n");
            //log("uvc_format_desc.bFormatIndex = " + uvc_format_desc.bFormatIndex);
            uvc_format_desc = uvc_format_desc.next;
        }
        log ("numberOfFormatDescriptors = " + numberOfFormatDescriptors);

        final JNA_I_LibUsb.uvc_format_desc[] format_descs_Array;

        format_descs_Array = new JNA_I_LibUsb.uvc_format_desc[numberOfFormatDescriptors];
        uvc_format_desc = uvc_device_info.stream_ifs.format_descs;
        int num = 0;
        while (uvc_format_desc != null ) {
            format_descs_Array[num] = uvc_format_desc;
            //streamInterfaceEntries.append("\n");
            //streamInterfaceEntries.append("FormatDescriptor " + uvc_format_desc.bFormatIndex + "\n");
            //log("uvc_format_desc.bFormatIndex = " + uvc_format_desc.bFormatIndex);
            uvc_format_desc = uvc_format_desc.next;
            num++;
        }
        //final JNA_I_LibUsb.uvc_format_desc[] format_descs_Array = (JNA_I_LibUsb.uvc_format_desc[])uvc_device_info.stream_ifs.format_descs.toArray(numberOfFormatDescriptors) ;
        numberFormatIndexes = new int[numberOfFormatDescriptors];
        final String[] textmsg = new String[numberOfFormatDescriptors];
        for (int a = 0; a < numberOfFormatDescriptors; a++) {
            if (format_descs_Array[a].bDescriptorSubtype == VS_format_mjpeg) {
                System.out.println("formatIndex = VS_format_mjpeg");
                numberFormatIndexes[a] = format_descs_Array[a].bFormatIndex;
                System.out.println("numberFormatIndexes[" + a + "] = " + numberFormatIndexes[a]);
                textmsg[a] = "MJPEG";
                //svideoformat =
            } else if (format_descs_Array[a].bDescriptorSubtype == VS_format_uncompressed) {
                System.out.println("formatIndex = VS_format_uncompressed");
                numberFormatIndexes[a] = format_descs_Array[a].bFormatIndex;
                String guidFormat = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    guidFormat = new String(format_descs_Array[a].formatSpecifier.guidFormat, StandardCharsets.UTF_8);
                } else guidFormat =  new String (format_descs_Array[a].formatSpecifier.guidFormat);
                String fourccFormat = guidFormat.substring(0,4);
                System.out.println("fourccFormat = " + fourccFormat);
                System.out.println("numberFormatIndexes[" + a + "] = " + numberFormatIndexes[a]);
                textmsg[a] = fourccFormat;
            }
        }
        final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
        builder.setTitle("Select the Camera Format (MJPEG / YUV / ...)");
        if (svideoformat != null) builder.setMessage("Your current format: " + svideoformat);
        int selectedItem = 0;
        for (int a =0; a<numberFormatIndexes.length; a++) {
            if (numberFormatIndexes[a] == scamFormatIndex) selectedItem = a;
        }
        final int startvalue = selectedItem;
        builder.setSingleChoiceItems(textmsg, startvalue, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                scamFormatIndex = numberFormatIndexes[index];

                if (format_descs_Array[index].bDescriptorSubtype == VS_format_mjpeg) svideoformat = "MJPEG";
                else if (format_descs_Array[index].bDescriptorSubtype == VS_format_uncompressed) {
                    String guidFormat = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        guidFormat = new String(format_descs_Array[index].formatSpecifier.guidFormat, StandardCharsets.UTF_8);
                    } else guidFormat =  new String (format_descs_Array[index].formatSpecifier.guidFormat);
                    String fourccFormat = guidFormat.substring(0,4);
                    System.out.println("fourccFormat = " + fourccFormat);
                    svideoformat = fourccFormat;
                }

                log("svideoformat = " + svideoformat);
                log("index = " + index);
                selectFrameIndex(uvc_device_info, format_descs_Array[index]);
                dialogInterface.dismiss();
            }
        });
        builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                log("index = " + index);
                scamFormatIndex = numberFormatIndexes[index];

                if (format_descs_Array[index].bDescriptorSubtype == VS_format_mjpeg) svideoformat = "MJPEG";
                else if (format_descs_Array[index].bDescriptorSubtype == VS_format_uncompressed) {
                    String guidFormat = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                        guidFormat = new String(format_descs_Array[index].formatSpecifier.guidFormat, StandardCharsets.UTF_8);
                    } else guidFormat =  new String (format_descs_Array[index].formatSpecifier.guidFormat);
                    String fourccFormat = guidFormat.substring(0,4);
                    System.out.println("fourccFormat = " + fourccFormat);
                    svideoformat = fourccFormat;
                }
                log("svideoformat = " + svideoformat);
                log("index = " + index);
                selectFrameIndex(uvc_device_info, format_descs_Array[index]);
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void selectFrameIndex (boolean automatic) {
        if (!automatic) {
            int [] scamFrameIndexArray = new int [formatIndex.numberOfFrameDescriptors];
            frameDescriptorsResolutionArray = new String[formatIndex.numberOfFrameDescriptors];
            for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {
                frameIndex = formatIndex.getFrameIndex(j);
                StringBuilder stringb = new StringBuilder();
                stringb.append(Integer.toString(frameIndex.wWidth));
                stringb.append(" x ");
                stringb.append(Integer.toString(frameIndex.wHeight));
                frameDescriptorsResolutionArray[j] = stringb.toString();
                scamFrameIndexArray[j] = frameIndex.frameIndex;
            }
            final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Camera Resolution (Frame Format)");
            builder.setMessage("Your current Resolution: " + simageWidth + "x" + simageHeight);
            int selectedItem = 0;
            for (int a =0; a<scamFrameIndexArray.length; a++) {
                if (scamFrameIndexArray[a] == scamFrameIndex) selectedItem = a;
            }
            builder.setSingleChoiceItems(frameDescriptorsResolutionArray, selectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    frameIndex = formatIndex.getFrameIndex(index);
                    scamFrameIndex = frameIndex.frameIndex;
                    System.out.println("scamFrameIndex = " + scamFrameIndex);
                    simageWidth = frameIndex.wWidth;
                    simageHeight = frameIndex.wHeight;
                }
            });
            builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    if (scamFrameIndex == 0) {
                        frameIndex = formatIndex.getFrameIndex(index);
                        scamFrameIndex = frameIndex.frameIndex;
                    }
                    if (simageWidth == 0 || simageHeight == 0) {
                        frameIndex = formatIndex.getFrameIndex(index);
                        simageWidth = frameIndex.wWidth;
                        simageHeight = frameIndex.wHeight;
                    }
                    selectDWFrameIntervall(automatic);
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        } else {
            if (libUsb_autoDetect != null || jna_autoDetect != null) {
                if(!highQuality) {

                }
                int[] resArray = new int [formatIndex.numberOfFrameDescriptors];
                for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {
                    frameIndex = formatIndex.getFrameIndex(j);
                    resArray[j] = (frameIndex.wWidth * frameIndex.wHeight);
                }
                // find lowest resolution:
                int minValue = resArray[0];
                int minPos = 0;
                for (int i = 1; i < resArray.length; i++) {
                    if (resArray[i] < minValue) {
                        minValue = resArray[i];
                        minPos = i;
                    }
                }
                frameIndex = formatIndex.getFrameIndex(minPos);
                scamFrameIndex = frameIndex.frameIndex;
                simageWidth = frameIndex.wWidth;
                simageHeight = frameIndex.wHeight;
                System.out.println("scamFrameIndex = " + scamFrameIndex);
                System.out.println("simageWidth = " + simageWidth);
                System.out.println("simageHeight = " + simageHeight);
                selectDWFrameIntervall(automatic);
                return;
            }
            selectDWFrameIntervall(automatic);
        }
    }

    private void selectFrameIndex (final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info, final JNA_I_LibUsb.uvc_format_desc format_descs) {


        log ("format_descs.bNumFrameDescriptors = " + format_descs.bNumFrameDescriptors);
        log ("format_descs.bFormatIndex = " + format_descs.bFormatIndex);
        log ("format_descs.frame_descs.bFrameIndex = " + format_descs.frame_descs.bFrameIndex);
        log ("format_descs.frame_descs.wWidth = " + format_descs.frame_descs.wWidth);
        log ("format_descs.frame_descs.wHeight = " + format_descs.frame_descs.wHeight);



        JNA_I_LibUsb.uvc_frame_desc uvc_frame_desc;


        int numberOfFrameDescriptors = 0;
        uvc_frame_desc = format_descs.frame_descs;
        if (uvc_frame_desc == null) log("uvc_frame_desc == null");

        log("uvc_frame_desc != null");

        while (uvc_frame_desc != null ) {
            numberOfFrameDescriptors ++;
            uvc_frame_desc = uvc_frame_desc.next;
        }
        log("numberOfFrameDescriptors = " + numberOfFrameDescriptors);

        JNA_I_LibUsb.uvc_frame_desc[] frame_descs_Array = new JNA_I_LibUsb.uvc_frame_desc[numberOfFrameDescriptors];

        uvc_frame_desc = format_descs.frame_descs;

        int count = 0;
        while (uvc_frame_desc != null ) {
            frame_descs_Array[count] = uvc_frame_desc;
            uvc_frame_desc = uvc_frame_desc.next;
            count ++;
        }


        //final JNA_I_LibUsb.uvc_format_desc[] format_descs_Array = (JNA_I_LibUsb.uvc_format_desc[])uvc_device_info.stream_ifs.format_descs.toArray(numberOfFormatDescriptors) ;
        //final JNA_I_LibUsb.uvc_frame_desc[] frame_descs_Array = (JNA_I_LibUsb.uvc_frame_desc[])format_descs.frame_descs.toArray(numberOfFrameDescriptors) ;



        log("scamFrameIndexArray");

        int [] scamFrameIndexArray = new int [numberOfFrameDescriptors];
        log("numberOfFrameDescriptors = " + numberOfFrameDescriptors);
        frameDescriptorsResolutionArray = new String[numberOfFrameDescriptors];


        for (int j = 0; j < numberOfFrameDescriptors; j++) {
            StringBuilder stringb = new StringBuilder();
            stringb.append(Integer.toString(frame_descs_Array[j].wWidth));
            stringb.append(" x ");
            stringb.append(Integer.toString(frame_descs_Array[j].wHeight));
            frameDescriptorsResolutionArray[j] = stringb.toString();
            scamFrameIndexArray[j] = frame_descs_Array[j].bFrameIndex;
        }
        final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
        builder.setTitle("Camera Resolution (Frame Format)");
        builder.setMessage("Your current Resolution: " + simageWidth + "x" + simageHeight);
        int selectedItem = 0;
        for (int a =0; a<scamFrameIndexArray.length; a++) {
            if (scamFrameIndexArray[a] == scamFrameIndex) selectedItem = a;
        }
        builder.setSingleChoiceItems(frameDescriptorsResolutionArray, selectedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                scamFrameIndex = frame_descs_Array[index].bFrameIndex;
                System.out.println("scamFrameIndex = " + scamFrameIndex);
                simageWidth = frame_descs_Array[index].wWidth;
                simageHeight = frame_descs_Array[index].wHeight;
            }
        });
        builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                if (scamFrameIndex == 0) {
                    scamFrameIndex = frame_descs_Array[index].bFrameIndex;
                }
                if (simageWidth == 0 || simageHeight == 0) {
                    scamFrameIndex = frame_descs_Array[index].bFrameIndex;
                    simageWidth = frame_descs_Array[index].wWidth;
                    simageHeight = frame_descs_Array[index].wHeight;
                }
                selectDWFrameIntervall(uvc_device_info, frame_descs_Array[index]);
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void selectDWFrameIntervall(boolean automatic){
        if (!automatic) {
            dwFrameIntervalArray = new String [frameIndex.dwFrameInterval.length];
            for (int k=0; k<dwFrameIntervalArray.length; k++) {
                dwFrameIntervalArray[k] = Integer.toString(frameIndex.dwFrameInterval[k]);
            }
            final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
            builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
            builder.setTitle("Select the camera Frame Intervall");
            if (scamFrameInterval == 0)  builder.setMessage("Your current FrameInterval:  " + scamFrameInterval + " fps");
            else builder.setMessage("Your current FrameInterval: " + (10000000 /  scamFrameInterval) + " fps");

            int selectedItem = 0;
            for (int a =0; a<frameIndex.dwFrameInterval.length; a++) {
                if (frameIndex.dwFrameInterval[a] == scamFrameInterval) selectedItem = a;
            }
            String [] builderArray = new String[frameIndex.dwFrameInterval.length];
            for (int k=0; k<builderArray.length; k++) {
                builderArray[k] = "";
                builderArray[k] += (  (10000000 / frameIndex.dwFrameInterval[k] )  + "  -  Frames per Second") ;
            }
            builder.setSingleChoiceItems(builderArray, selectedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    scamFrameInterval = frameIndex.dwFrameInterval[index];
                }
            });
            builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int index) {
                    if (scamFrameInterval == 0) scamFrameInterval = frameIndex.dwFrameInterval[index];
                    writeTheValues();
                    saveYesNo();
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        } else {
            if (libUsb_autoDetect != null || jna_autoDetect != null) {
                int[] intervalArray = frameIndex.dwFrameInterval.clone();
                // sorting the array to smalest Value first
                Arrays.sort(intervalArray);
                scamFrameInterval = frameIndex.dwFrameInterval[(intervalArray.length - 1)];
                System.out.println("scamFrameInterval = " + scamFrameInterval);
                writeTheValues();
                return;
            }
            if(!highQuality) {
                int[] intervalArray = frameIndex.dwFrameInterval.clone();
                // sorting the array to smalest Value first
                Arrays.sort(intervalArray);
                scamFrameInterval = frameIndex.dwFrameInterval[(intervalArray.length - 1)];
                System.out.println("scamFrameInterval = " + scamFrameInterval);
                checkAutoDetectFileName(true);
                writeTheOrders();
                writeTheValues();
                return;
            }
            saveYesNo();
        }
    }

    private void selectDWFrameIntervall(final JNA_I_LibUsb.uvc_device_info.ByReference uvc_device_info, final JNA_I_LibUsb.uvc_frame_desc frame_desc){

        dwFrameIntervalArray = new String [frame_desc.bFrameIntervalType];




        log("frame_desc.bFrameIntervalType = " + frame_desc.bFrameIntervalType);

        log("frame_desc.intervals.getValue() = " + frame_desc.intervals.getInt(0));
        log("frame_desc.intervals.getValue() = " + frame_desc.intervals.getInt(4));
        log("frame_desc.intervals.getValue() = " + frame_desc.intervals.getInt(8));
        log("frame_desc.intervals.getValue() = " + frame_desc.intervals.getInt(12));
        log("frame_desc.intervals.getValue() = " + frame_desc.intervals.getInt(16));



        //Pointer p = frame_desc.intervals.getPointer();




        for (int k=0,j=0; k<dwFrameIntervalArray.length; k++,j+=4) {
            dwFrameIntervalArray[k] = Integer.toString(frame_desc.intervals.getInt(j));
        }

        final CFAlertDialog.Builder builder = new CFAlertDialog.Builder(mContext);
        builder.setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT);
        builder.setTitle("Select the camera Frame Intervall");
        if (scamFrameInterval == 0)  builder.setMessage("Your current FrameInterval:  " + scamFrameInterval + " fps");
        else builder.setMessage("Your current FrameInterval: " + (10000000 /  scamFrameInterval) + " fps");

        int selectedItem = 0;
        for (int a =0, j = 0; a<frame_desc.bFrameIntervalType; a++, j+=4) {
            if (frame_desc.intervals.getInt(j) == scamFrameInterval) selectedItem = a;
        }
        String [] builderArray = new String[frame_desc.bFrameIntervalType];
        for (int k=0, l=0; k<builderArray.length; k++, l+=4) {
            builderArray[k] = "";
            builderArray[k] += (  (10000000 / frame_desc.intervals.getInt(l) )  + "  -  Frames per Second") ;
        }
        builder.setSingleChoiceItems(builderArray, selectedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {

                scamFrameInterval = frame_desc.intervals.getInt(index*4);

            }
        });
        builder.addButton("DONE", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.END, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                if (scamFrameInterval == 0) scamFrameInterval = frame_desc.intervals.getInt(index*4);
                writeTheValues();
                saveYesNo();
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void saveYesNo() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        writeTheValues();
                        //optionForSaveFile = OptionForSaveFile.savetofile;
                        checkTheSaveFileName(OptionForSaveFile.savetofile);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        writeTheValues();
                        returnToMainLayout("Values setted up with UVC-configuration but not stored to a file");
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Do you want to save the values to a file?").setPositiveButton("Yes, Save", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void writeMsgMain(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayMessage(msg);
                //log("setting libusb Button");
                ToggleButton libUsbActivate = activity.findViewById(R.id.libusbToggleButton);
                if (libUsb == true) libUsbActivate.setChecked(true);
                else libUsbActivate.setChecked(false);
                libUsbActivate.setEnabled(false);
            }
        });

    }

    public void setTextViewMain() {
        uvc_camera.setTextTextView();

    }

    int getDistance(MotionEvent event) {
        int dx = (int) (event.getX(0) - event.getX(1));
        int dy = (int) (event.getY(0) - event.getY(1));
        return (int) (Math.sqrt(dx * dx + dy * dy));
    }

    private void alertView(String title, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle( title )
                .setIcon(R.drawable.ic_menu_camera)
                .setMessage(message)
//     .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//      public void onClick(DialogInterface dialoginterface, int i) {
//          dialoginterface.cancel();
//          }})
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                    }
                }).show();
    }

    private SpannableStringBuilder setColorText (String aString, String bString ) {

        SpannableStringBuilder spanBuilder = new SpannableStringBuilder();

        SpannableString str1= new SpannableString(aString);
        str1.setSpan(new ForegroundColorSpan(Color.BLACK), 0, str1.length(), 0);
        spanBuilder.append(str1);

        SpannableString str2= new SpannableString(bString);
        str2.setSpan(new ForegroundColorSpan(darker(Color.GREEN,120)), 0, str2.length(), 0);
        spanBuilder.append(str2);

        return spanBuilder;
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

    private void checkAutoDetectFileName(boolean save) {
        fileName = null;
        name = null;
        rootdirStr = null;


        Context context = activity.getApplicationContext();
        File directory = context.getFilesDir();
        File saveDir = new File(directory, autoFilePathFolder);
        if (!saveDir.exists()) {
            log("creating directory");
            if (!saveDir.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
            }
        }
        log("Path: " + saveDir.toString());
        rootdirStr = saveDir.toString();
        rootdirStr += "/";
        autoDetectFileValuesString = new String("AutoDetectFileValues");
        autoDetectFileOrdersString = new String("AutoDetectFileOrders");

        if (save) {
            log("saveAutoOrders Path = " + rootdirStr + autoDetectFileOrdersString + ".sav");
            log("saveValuesToFile Path = " + rootdirStr + autoDetectFileValuesString + ".sav");

            saveAutoOrders(autoDetectFileOrdersString);
            saveValuesToFile(autoDetectFileValuesString);
            return;
        }
        if (listFilesAutoDetectFolder(directory)) {
            log("checking Auto Values ...");
            restoreAutoOrders(autoDetectFileOrdersString);
            restoreFromFile(autoDetectFileValuesString);
            selectMaxPacketSize(true);
        }
    }


    public boolean listFilesAutoDetectFolder(final File folder) {
        boolean autoDetectFileOrders = false;
        boolean autoDetectFileValues = false;
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesAutoDetectFolder(fileEntry);
            } else {
                System.out.println(fileEntry.getName());
                if (fileEntry.getName().equals("AutoDetectFileOrders")) autoDetectFileValues = true;
                else if (fileEntry.getName().equals("AutoDetectFileValues")) autoDetectFileOrders = true;
            }
        }

        if (folder.listFiles().length == 2) return true;
        else return false;
    }

    public void restoreAutoOrders(String pathToFile){
        try{
            Context context = activity.getApplicationContext();
            File directory = context.getFilesDir();
            File saveDir = new File(directory, autoFilePathFolder);
            if (!saveDir.exists()) {
                log("creating directory");
                if (!saveDir.mkdirs()) {
                    Log.e("TravellerLog :: ", "Problem creating Image folder");
                }
            }
            FileInputStream saveFile = new FileInputStream(new File(saveDir, pathToFile));
            ObjectInputStream save = new ObjectInputStream(saveFile);
            completed = (Boolean) save.readObject();
            highQuality = (Boolean) save.readObject();
            raiseMaxPacketSize = (Boolean) save.readObject();
            lowerMaxPacketSize = (Boolean) save.readObject();
            raisePacketsPerRequest = (Boolean) save.readObject();
            raiseActiveUrbs = (Boolean) save.readObject();

            highestMaxPacketSizeDone = (Boolean) save.readObject();
            lowestMaxPacketSizeDone  = (Boolean) save.readObject();
            save.close();
        }
        catch(Exception exc){
            exc.printStackTrace();
        }
        writeTheOrders();
        log("Orders written to setUpTheUsbDevice");
    }

    private void saveAutoOrders (String savePath) {

        Context context = activity.getApplicationContext();
        File directory = context.getFilesDir();
        File saveDir = new File(directory, autoFilePathFolder);
        if (!saveDir.exists()) {
            log("creating directory");
            if (!saveDir.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
            }
        }



        log("Name AutoOrder = " + savePath);
        try {  // Catch errors in I/O if necessary.
            File file = new File(saveDir, savePath);
            //file = new File(savePath).getAbsoluteFile();
            log("AbsolutePath = " + file.getAbsolutePath());
            //file.getParentFile().mkdirs();
            if (file.exists())  file.delete();

            FileOutputStream saveFile=new FileOutputStream(file.toString());
            ObjectOutputStream save = new ObjectOutputStream(saveFile);

            save.writeObject(completed);
            save.writeObject(highQuality);
            save.writeObject(raiseMaxPacketSize);
            save.writeObject(lowerMaxPacketSize);
            save.writeObject(raisePacketsPerRequest);
            save.writeObject(raiseActiveUrbs);

            save.writeObject(highestMaxPacketSizeDone);
            save.writeObject(lowestMaxPacketSizeDone);
            // Close the file.
            save.close(); // This also closes saveFile.
        } catch (Exception e) { log("Error"); e.printStackTrace();}
    }

    private void writeTheOrders() {
        if (setUpTheUsbDeviceUsbIso != null) {
            setUpTheUsbDeviceUsbIso.completed = completed;
            setUpTheUsbDeviceUsbIso.highQuality = highQuality;
            setUpTheUsbDeviceUsbIso.raiseMaxPacketSize = raiseMaxPacketSize;
            setUpTheUsbDeviceUsbIso.lowerMaxPacketSize = lowerMaxPacketSize;
            setUpTheUsbDeviceUsbIso.raisePacketsPerRequest = raisePacketsPerRequest;
            setUpTheUsbDeviceUsbIso.raiseActiveUrbs = raiseActiveUrbs;
        } else {
            setUpTheUsbDeviceUvc.completed = completed;
            setUpTheUsbDeviceUvc.highQuality = highQuality;
            setUpTheUsbDeviceUvc.raiseMaxPacketSize = raiseMaxPacketSize;
            setUpTheUsbDeviceUvc.lowerMaxPacketSize = lowerMaxPacketSize;
            setUpTheUsbDeviceUvc.raisePacketsPerRequest = raisePacketsPerRequest;
            setUpTheUsbDeviceUvc.raiseActiveUrbs = raiseActiveUrbs;
        }


    }

}
/*
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
 */