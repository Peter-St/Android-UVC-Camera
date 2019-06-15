package humer.uvc_camera;

/*
 * Copyright 2019 peter.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;




/**
 *
 * @author peter
 */


public class SaveToFile  {

    public static int sALT_SETTING;
    public static int smaxPacketSize ;
    public static int scamFormatIndex ;   // MJPEG // YUV // bFormatIndex: 1 = uncompressed
    public static String svideoformat;
    public static int scamFrameIndex ; // bFrameIndex: 1 = 640 x 360;       2 = 176 x 144;     3 =    320 x 240;      4 = 352 x 288;     5 = 640 x 480;
    public static int simageWidth;
    public static int simageHeight;
    public static int scamFrameInterval ; // 333333 YUV = 30 fps // 666666 YUV = 15 fps
    public static int spacketsPerRequest ;
    public static int sactiveUrbs ;
    public static String sdeviceName;


    private static String saveFilePathFolder = "UVC_Camera/save";
    private TextInputLayout valueInput;
    private boolean init = false;


    private SetUpTheUsbDevice setUpTheUsbDevice;
    private Main uvc_camera = null;
    private Context mContext;
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

    StringBuilder stringBuilder;
    String rootdirStr;

    private enum OptionForSaveFile {savetofile, restorefromfile}
    OptionForSaveFile optionForSaveFile;

    private boolean abfrage = true;

    static ArrayList<String> paths = new ArrayList<>(50);
    private static ArrayList<String> saveValues = new ArrayList<>(20);

    TextView tv;
    private Button settingsButton;

    private UVC_Descriptor uvc_descriptor;
    private static int [] numberFormatIndexes;
    private UVC_Descriptor.FormatIndex formatIndex;
    private UVC_Descriptor.FormatIndex.FrameIndex frameIndex;
    private static String[] frameDescriptorsResolutionArray;
    private static String [] dwFrameIntervalArray;
    private static String [] maxPacketSizeStr;
    private String name;
    private String fileName;




    public SaveToFile(Main main, Context mContext) {
        this.uvc_camera = main;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
    }

    public SaveToFile (SetUpTheUsbDevice setUpTheUsbDevice, Context mContext) {
        this.setUpTheUsbDevice = setUpTheUsbDevice;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
        this.init = true;
    }




    private void returnToMainLayout(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(R.layout.set_up_the_device_layout_main);
                tv = activity.findViewById(R.id.textDarstellung);
                tv.setText(msg);

            }
        });

    }

    public void startEditSave() {
        fetchTheValues();
        activity.setContentView(R.layout.einstellungen);
        sALT_SETTING_text = (TextView) activity.findViewById(R.id.Altsetting);
        sALT_SETTING_text.setText( String.format("ALT_SETTING:  %s" , sALT_SETTING));
        smaxPacketSize_text = (TextView) activity.findViewById(R.id.MaxPacketSize);
        smaxPacketSize_text.setText( String.format("maxPacketSize:  %s" , smaxPacketSize));
        scamFormatIndex_text = (TextView) activity.findViewById(R.id.FormatIndex);
        scamFormatIndex_text.setText( String.format("FormatIndex:  %s" , scamFormatIndex));
        svideoformat_text = (TextView) activity.findViewById(R.id.svideoformat);
        svideoformat_text.setText( String.format("Videoformat:  %s" , svideoformat));
        scamFrameIndex_text = (TextView) activity.findViewById(R.id.FrameIndex);
        scamFrameIndex_text.setText( String.format("FrameIndex:  %s" , scamFrameIndex));
        simageWidth_text = (TextView) activity.findViewById(R.id.ImageWidth);
        simageWidth_text.setText( String.format("imageWidth:  %s" , simageWidth));
        simageHeight_text = (TextView) activity.findViewById(R.id.ImageHeight);
        simageHeight_text.setText( String.format("imageHeight:  %s" , simageHeight));
        scamFrameInterval_text = (TextView) activity.findViewById(R.id.FrameInterval);
        scamFrameInterval_text.setText( String.format("CAM_FRAME_INTERVAL:  %s" , scamFrameInterval));
        spacketsPerRequest_text = (TextView) activity.findViewById(R.id.PacketsPerReq);
        spacketsPerRequest_text.setText( String.format("PacketsPerRequest:  %s" , spacketsPerRequest));
        sactiveUrbs_text = (TextView) activity.findViewById(R.id.ActiveUrbs);
        sactiveUrbs_text.setText( String.format("ACTIVE_URBS:  %s" , sactiveUrbs));


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
                valueInput = (TextInputLayout) activity.findViewById(R.id.Imagewi);
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

    }

    private void fetchTheValues(){
        sALT_SETTING = setUpTheUsbDevice.camStreamingAltSetting;
        svideoformat = setUpTheUsbDevice.videoformat;
        scamFormatIndex = setUpTheUsbDevice.camFormatIndex;
        simageWidth = setUpTheUsbDevice.imageWidth;
        simageHeight = setUpTheUsbDevice.imageHeight;
        scamFrameIndex = setUpTheUsbDevice.camFrameIndex;
        scamFrameInterval = setUpTheUsbDevice.camFrameInterval;
        spacketsPerRequest = setUpTheUsbDevice.packetsPerRequest;
        smaxPacketSize = setUpTheUsbDevice.maxPacketSize;
        sactiveUrbs = setUpTheUsbDevice.activeUrbs;
        sdeviceName = setUpTheUsbDevice.deviceName;
    }

    private void writeTheValues(){
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
        } else {
            setUpTheUsbDevice.camStreamingAltSetting = sALT_SETTING;
            setUpTheUsbDevice.videoformat = svideoformat;
            setUpTheUsbDevice.camFormatIndex = scamFormatIndex;
            setUpTheUsbDevice.imageWidth = simageWidth;
            setUpTheUsbDevice.imageHeight = simageHeight;
            setUpTheUsbDevice.camFrameIndex = scamFrameIndex;
            setUpTheUsbDevice.camFrameInterval = scamFrameInterval;
            setUpTheUsbDevice.packetsPerRequest = spacketsPerRequest;
            setUpTheUsbDevice.maxPacketSize = smaxPacketSize;
            setUpTheUsbDevice.activeUrbs = sactiveUrbs;
            setUpTheUsbDevice.deviceName = sdeviceName;
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

        int code = mContext.getPackageManager().checkPermission(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                mContext.getPackageName());
        if (code == PackageManager.PERMISSION_GRANTED) {
            final String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() ;
            final File file = new File(rootPath, "/" + saveFilePathFolder);
            if (!file.exists()) {
                file.mkdirs();
            }

            log("Path: " + rootPath.toString());
            rootdirStr = file.toString();
            rootdirStr += "/";
            //final File folder = new File("/home/you/Desktop");
            listFilesForFolder(file);
            if (paths.isEmpty() == true && optionForSaveFile == OptionForSaveFile.restorefromfile) {
                returnToMainLayout(String.format("No savefiles found in the save directory.\nDirectory: &s", rootdirStr ));
            } else {
                stringBuilder.append("Type the number of the file to select it, or type in the name (new or existing).\n");
                for (int i = 0; i < paths.size(); i++) {
                    stringBuilder.append(String.format("%d   ->   ", (i+1)));
                    stringBuilder.append(paths.get(i));
                    stringBuilder.append("\n");
                }
                log(stringBuilder.toString());
                if (option == OptionForSaveFile.restorefromfile) {

                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
                    builderSingle.setIcon(R.drawable.ic_menu_camera);
                    builderSingle.setTitle("Please select the file to restore");

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

                    for (int i = 0; i < paths.size(); i++) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(paths.get(i));
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
                            String path = rootdirStr;
                            path += strName;
                            restorFromFile(path);
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
                                log("Die Eingabe war: " + input.getText().toString());
                                name = input.getText().toString();
                            }
                            if (name.isEmpty() == true) {
                                switch(option) {
                                    case savetofile: saveValuesToFile(rootdirStr += sdeviceName += ".sav");
                                    log("sdeviceName = " + sdeviceName);
                                        break;
                                    case restorefromfile: restorFromFile(rootdirStr += sdeviceName += ".sav");
                                        break;
                                }
                            }
                            else if (isInteger(name) == true) {
                                switch(option) {
                                    case savetofile:
                                        fileName = paths.get((Integer.parseInt(name) - 1));
                                        try { saveValuesToFile(fileName); }
                                        catch (Exception e) { log("Save Failed ; Exception = " + e); e.printStackTrace();}
                                        break;
                                    case restorefromfile:
                                        restorFromFile(paths.get((Integer.parseInt(name) - 1)));
                                        break;
                                }
                            } else {
                                switch(option) {
                                    case savetofile:
                                        saveValuesToFile((rootdirStr += name += ".sav"));      log("Saving ...");
                                        break;
                                    case restorefromfile:
                                        restorFromFile((rootdirStr += name += ".sav"));      log("Saving ...");
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
                                    returnToMainLayout("Values written and saved:\n\nName of the savefile:\n" + fileN + "\n\nFolder of the savefile:\n" + rootdirString );
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
        } else returnToMainLayout(String.format("No Permissions were granted to the app !!\nSolution:\nGo to Settings --> Apps --> UVC_Camera\nClick on Permissions and then grant the storage permission!"));


    }



    private void saveValuesToFile (String savePath) {

        log("savePath = " + savePath);
        try {  // Catch errors in I/O if necessary.
            /*
        File dump = new File(DUMP_FILE).getAbsoluteFile();
        dump.getParentFile().mkdirs();
        */
            File file = new File(savePath);
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
            // Close the file.
            save.close(); // This also closes saveFile.
        } catch (Exception e) { log("Error"); e.printStackTrace();}


        returnToMainLayout(String.format("Values edited and saved\nSavefile = %s", savePath));
    }


    public void restorFromFile(String pathToFile){
        try{
            FileInputStream saveFile = new FileInputStream(pathToFile);
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
            save.close();
        }
        catch(Exception exc){
            exc.printStackTrace();
        }

        log("sALT_SETTING = " + sALT_SETTING + "  /  svideoformat = " + svideoformat + "  /  scamFormatIndex = " + scamFormatIndex + "  /  scamFrameIndex = " + scamFrameIndex);
        writeTheValues();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Restored Values:\n"));
        sb.append(String.format("Altsetting = %d\nVideoFormat = %s\nImageWidth = %d\nImageHeight = %d\nFrameInterval = %d\nPacketsPerRequest = %d\nActiveUrbs = %d", sALT_SETTING, svideoformat, simageWidth, simageHeight, scamFrameInterval, spacketsPerRequest, sactiveUrbs));
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
        else setUpTheUsbDevice.displayMessage(msg);
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



    public void setUpWithUvcValues(UVC_Descriptor uvc_desc, int[] maxPacketSizeArray) {

        fetchTheValues();
        this.uvc_descriptor = uvc_desc;
        for (int a=0; a<maxPacketSizeArray.length; a++) {
            log ("maxPacketSizeArray[" + a + "] = " + maxPacketSizeArray[a]);
        }
        UVC_Descriptor.FormatIndex formatIndex;
        int [] arrayFormatFrameIndexes = new int [uvc_descriptor.formatIndex.size()];
        for (int i=0; i<uvc_descriptor.formatIndex.size(); i++) {
            formatIndex = uvc_descriptor.getFormatIndex(i);
            arrayFormatFrameIndexes[i] = formatIndex.frameIndex.size();
        }

        maxPacketSizeStr = new String [maxPacketSizeArray.length];
        for (int a =0; a<maxPacketSizeArray.length; a++) {
            maxPacketSizeStr[a] = Integer.toString(maxPacketSizeArray[a]);
        }
        selectMaxPacketSize();

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




    private void selectMaxPacketSize(){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Select the maximal Packet Size:");
        //builderSingle.setMessage("Select the maximal size of the Packets, which where sent to the camera device!! Important for Mediathek Devices !!");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
        for (int i = 0; i<maxPacketSizeStr.length; i++){
            arrayAdapter.add(maxPacketSizeStr[i]);
        }


        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = arrayAdapter.getItem(which);
                smaxPacketSize = Integer.parseInt(input.toString());
                for (int i=1; i<(maxPacketSizeStr.length +1); i++) {
                    if (input.matches(maxPacketSizeStr[i-1]) ) {
                        sALT_SETTING = i;
                    }
                }
                System.out.println("sALT_SETTING = " + sALT_SETTING);
                System.out.println("smaxPacketSize = " + smaxPacketSize);
                selectPackets();
            }
        });
        builderSingle.show();

    }




    public void selectPackets() {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Packets per Request");
        builder.setMessage(String.format("Select the Packets per Request: (Number of Packet with a size of: %d)", smaxPacketSize));

// Set up the input
        final EditText input = new EditText(mContext);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty() == false)  spacketsPerRequest = Integer.parseInt(input.getText().toString());
                selectUrbs();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void selectUrbs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("USB Request Block");
        builder.setMessage(String.format("Select the URBs: (Select the number of Packet Blocks running in paralell order.)\nOne Block is %d x %d Bytes",smaxPacketSize,  spacketsPerRequest ));

// Set up the input
        final EditText input = new EditText(mContext);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty() == false)  sactiveUrbs = Integer.parseInt(input.getText().toString());
                selectFormatIndex();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.cancel();
            }
        });

        builder.show();

    }


    private void selectFormatIndex () {


        numberFormatIndexes = new int[uvc_descriptor.formatIndex.size()];
        final String[] textmsg = new String[uvc_descriptor.formatIndex.size()];
        for (int a = 0; a < uvc_descriptor.formatIndex.size(); a++) {
            formatIndex = uvc_descriptor.getFormatIndex(a);
            System.out.println("formatIndex.videoformat = " + formatIndex.videoformat);
            numberFormatIndexes[a] = formatIndex.formatIndexNumber;
            System.out.println("numberFormatIndexes[" + a + "] = " + numberFormatIndexes[a]);
            textmsg[a] = formatIndex.videoformat.toString();
        }


        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Camera Format (MJPEG / YUV / ...");
        //builderSingle.setMessage("Select the camera format (This video formats were supportet)");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

        for (int i = 0; i < textmsg.length; i++) {
            arrayAdapter.add(textmsg[i]);
        }

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.dismiss();
            }
        });
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                for (int i = 0; i < textmsg.length; i++) {
                    if (strName.matches(textmsg[i])) {
                        scamFormatIndex = numberFormatIndexes[i];
                        formatIndex = uvc_descriptor.getFormatIndex(i);
                        svideoformat = formatIndex.videoformat.toString();
                        String[] textmessage = new String[formatIndex.numberOfFrameDescriptors];
                        String inp;
                        for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {


                        }
                    }

                }
                selectFrameIndex();
            }
        });


        builderSingle.show();
    }



    private void selectFrameIndex () {

     frameDescriptorsResolutionArray = new String[formatIndex.numberOfFrameDescriptors];
        String inp;

        for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {
            frameIndex = formatIndex.getFrameIndex(j);
            StringBuilder stringb = new StringBuilder();
            stringb.append(Integer.toString(frameIndex.wWidth));
            stringb.append(" x ");
            stringb.append(Integer.toString(frameIndex.wHeight));
            frameDescriptorsResolutionArray[j] = stringb.toString();
        }
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Camera Resolution (Frame Format)");
        //builderSingle.setMessage("Select the camera Frame Format (Represents the Resolution)");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

        for (int i = 0; i < frameDescriptorsResolutionArray.length; i++) {
            arrayAdapter.add(frameDescriptorsResolutionArray[i]);
        }

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.dismiss();
            }
        });
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = arrayAdapter.getItem(which);
                for (int i = 0; i < frameDescriptorsResolutionArray.length; i++) {
                    for (int j = 0; j < formatIndex.numberOfFrameDescriptors; j++) {
                        if (input.equals(frameDescriptorsResolutionArray[j])) {
                            frameIndex = formatIndex.getFrameIndex(j);
                            scamFrameIndex = frameIndex.frameIndex;
                            System.out.println("scamFrameIndex = " + scamFrameIndex);
                            simageWidth = frameIndex.wWidth;
                            simageHeight = frameIndex.wHeight;
                        }
                    }
                }
                selectDWFrameIntervall();
            }
        });
        builderSingle.show();
    }


    private void selectDWFrameIntervall(){

        dwFrameIntervalArray = new String [frameIndex.dwFrameInterval.length];
        for (int k=0; k<dwFrameIntervalArray.length; k++) {
            dwFrameIntervalArray[k] = Integer.toString(frameIndex.dwFrameInterval[k]);
        }


        final AlertDialog.Builder dwFrameIntervalArraybuilder = new AlertDialog.Builder(mContext);
        dwFrameIntervalArraybuilder.setIcon(R.drawable.ic_menu_camera);
        dwFrameIntervalArraybuilder.setTitle("Frameintervall");
        //dwFrameIntervalArraybuilder.setMessage("Select the camera Frame Intervall\n333333 means 30 Frames per Second\n666666 means 15 Frames per Second\nThe number is in Nano Secounds and every amount of nanao secounds a Frame is sent.");
        final ArrayAdapter<String> dwFrameIntervalArrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

        for (int i = 0; i < dwFrameIntervalArray.length; i++) {
            dwFrameIntervalArrayAdapter.add(dwFrameIntervalArray[i]);
        }

        dwFrameIntervalArraybuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                writeMsgMain("UVC values canceled");
                dialog.dismiss();
            }
        });
        dwFrameIntervalArraybuilder.setAdapter(dwFrameIntervalArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String textInput = dwFrameIntervalArrayAdapter.getItem(which);
                if (textInput != null) {
                    scamFrameInterval = Integer.parseInt(textInput);
                    System.out.println("scamFrameInterval = " + scamFrameInterval);
                }
                writeTheValues();
                saveYesNo();

            }
        });

        dwFrameIntervalArraybuilder.show();
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
                //
                //tv.setText("msg");


            }
        });

    }


}



/*

public void displayDialog(){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Select One Name:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

        arrayAdapter.add("Hardik");
        arrayAdapter.add("Archit");
        arrayAdapter.add("Jignesh");
        arrayAdapter.add("Umang");
        arrayAdapter.add("Gatti");

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);



                AlertDialog.Builder builderInner = new AlertDialog.Builder(mContext);
                builderInner.setMessage(strName);
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                    }
                });
                builderInner.show();


            }

        });
        builderSingle.show();

    }
 */