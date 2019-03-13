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
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;



import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import humer.uvc_camera.Main;
import java.io.PrintWriter;


/**
 *
 * @author peter
 */
public class SaveToFile {

    public static int sALT_SETTING;
    public static int smaxPacketSize ;
    public static int scamFormatIndex ;   // MJPEG // YUV // bFormatIndex: 1 = uncompressed
    public static int svideoformat;
    public static int scamFrameIndex ; // bFrameIndex: 1 = 640 x 360;       2 = 176 x 144;     3 =    320 x 240;      4 = 352 x 288;     5 = 640 x 480;
    public static int simageWidth;
    public static int simageHeight;
    public static int scamFrameInterval ; // 333333 YUV = 30 fps // 666666 YUV = 15 fps
    public static int spacketsPerRequest ;
    public static int sactiveUrbs ;

    private static String saveFilePathFolder = "UVC_Camera/save";
    private TextInputLayout valueInput;



    private Main uvc_camera;
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

    public SaveToFile(Main main, Context mContext) {
        this.uvc_camera = main;
        this.mContext = mContext;
        this.activity = (Activity)mContext;
    }

    private void returnToMainLayout(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.setContentView(R.layout.layout_main);
                settingsButton = activity.findViewById(R.id.einstellungen);
                settingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Creating the instance of PopupMenu
                        PopupMenu popup = new PopupMenu(mContext, settingsButton);
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
                if (valueInput.getEditText().getText().toString().isEmpty() == false) svideoformat = Integer.parseInt(valueInput.getEditText().getText().toString());
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
                                optionForSaveFile = OptionForSaveFile.savetofile;
                                checkTheSaveFileName();
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

        sALT_SETTING = uvc_camera.camStreamingAltSetting;
        svideoformat = uvc_camera.videoformat;
        scamFormatIndex = uvc_camera.camFormatIndex;
        simageWidth = uvc_camera.imageWidth;
        simageHeight = uvc_camera.imageHeight;
        scamFrameIndex = uvc_camera.camFrameIndex;
        scamFrameInterval = uvc_camera.camFrameInterval;
        spacketsPerRequest = uvc_camera.packetsPerRequest;
        smaxPacketSize = uvc_camera.maxPacketSize;
        sactiveUrbs = uvc_camera.activeUrbs;
    }

    private void writeTheValues(){

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

    }



    public void restoreValuesFromFile() {
        optionForSaveFile = OptionForSaveFile.restorefromfile;
        checkTheSaveFileName();
        writeTheValues();
    }



    private void checkTheSaveFileName(){
        rootdirStr = null;
        stringBuilder = new StringBuilder();
        paths = new ArrayList<>(50);
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() ;
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
                    log("Die Eingabe war: " + input.getText().toString());
                    String name = input.getText().toString();
                    if (name.isEmpty() == true) {
                        switch(optionForSaveFile) {
                            case savetofile: saveValuesToFile(rootdirStr += "saveFile.sav");
                                break;
                            case restorefromfile: restorFromFile(rootdirStr += "saveFile.sav");
                                break;
                        }
                    }
                    else if (isInteger(name) == true) {
                        switch(optionForSaveFile) {
                            case savetofile:
                                try { saveValuesToFile(paths.get((Integer.parseInt(name) - 1))); }
                                catch (Exception e) { log("Save Failed ; Exception = " + e); e.printStackTrace();}
                                break;
                            case restorefromfile:
                                restorFromFile(paths.get((Integer.parseInt(name) - 1)));
                                break;
                        }
                    } else {
                        switch(optionForSaveFile) {
                            case savetofile:
                                saveValuesToFile((rootdirStr += name += ".sav"));      log("Saving ...");
                                break;
                            case restorefromfile:
                                restorFromFile((rootdirStr += name += ".sav"));      log("Saving ...");
                                break;
                        }
                    }
                    returnToMainLayout("Values written and saved");
                    return;
                }
            });
            builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnToMainLayout("Values written but not saved.");
                    return;
                }
            });
            builder2.show();
        }




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
            svideoformat = (Integer) save.readObject();
            scamFormatIndex  = (Integer) save.readObject();
            scamFrameIndex  = (Integer) save.readObject();
            simageWidth = (Integer) save.readObject();
            simageHeight = (Integer) save.readObject();
            scamFrameInterval  = (Integer) save.readObject();
            spacketsPerRequest  = (Integer) save.readObject();
            smaxPacketSize  = (Integer) save.readObject();
            sactiveUrbs  = (Integer) save.readObject();
            saveFilePathFolder  = (String) save.readObject();
            save.close();
        }
        catch(Exception exc){
            exc.printStackTrace();
        }

        returnToMainLayout(String.format("Values restored from File\n%s", pathToFile));
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
        uvc_camera.displayMessage(msg);
    }

    private void log(String msg) {
        Log.i("SaveToFile", msg);
    }

    private void displayErrorMessage(Throwable e) {
        uvc_camera.displayErrorMessage(e);
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

        String [] textmsg = new String [maxPacketSizeArray.length];
        for (int a =0; a<maxPacketSizeArray.length; a++) {
            textmsg[a] = Integer.toString(maxPacketSizeArray[a]);
        }
        selectMaxPacketSize(textmsg);

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




    private void selectMaxPacketSize(String [] options){

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Select the maximal size of the Packets, which where sent to the camera device!! Important for Mediathek Devices !!");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);
        for (int i = 0; i<options.length; i++){
            arrayAdapter.add(options[i]);
        }

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
                smaxPacketSize = Integer.parseInt(strName.toString());
                AlertDialog.Builder builderInner = new AlertDialog.Builder(mContext);
                builderInner.setMessage(strName);
                builderInner.setTitle("Your Selected Item is");
                builderInner.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog,int which) {
                        dialog.dismiss();
                        selectPackets();
                    }
                });
                builderInner.show();
            }
        });
        builderSingle.show();

    }




    public void selectPackets() {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(String.format("Select the Packets per Request: (Number of Packet with a size of: %d per Request)", smaxPacketSize));

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
                dialog.cancel();
            }
        });

        builder.show();

    }

    public void selectUrbs() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Select the URBs: (Select the number of Packet Blocks running in paralell order.");

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
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void selectFormatIndex (){

        UVC_Descriptor.FormatIndex formatIndex;
        UVC_Descriptor.FormatIndex.FrameIndex frameIndex;
        int [] numberFormatIndexes = new int [uvc_descriptor.formatIndex.size()];
        final String[] textmsg = new String [uvc_descriptor.formatIndex.size()];
        for (int a =0; a<uvc_descriptor.formatIndex.size(); a++) {
            formatIndex = uvc_descriptor.getFormatIndex(a);
            System.out.println("formatIndex.videoformat = " + formatIndex.videoformat);
            if (formatIndex.videoformat == UVC_Descriptor.FormatIndex.Videoformat.yuy2) System.out.println("PhraseUvcDescriptor.FormatIndex.Videoformat.yuy2 = " + UVC_Descriptor.FormatIndex.Videoformat.yuy2);
            else System.out.println(" ............");

            numberFormatIndexes[a] = formatIndex.formatIndexNumber;
            System.out.println("numberFormatIndexes[a] = " + numberFormatIndexes[a]);
            textmsg[a] = formatIndex.videoformat.toString();
        }


        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(mContext);
        builderSingle.setIcon(R.drawable.ic_menu_camera);
        builderSingle.setTitle("Select the camera Format (This video Formats were supportet)");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(mContext, android.R.layout.select_dialog_singlechoice);

        for (int i = 0; i<textmsg.length; i++){
            arrayAdapter.add(textmsg[i]);
        }

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        /*

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strName = arrayAdapter.getItem(which);
                for (int i=0; i<textmsg.length; i++) {
                    if (strName.matches(textmsg[i]) ) {
                        scamFormatIndex = numberFormatIndexes[i];
                        formatIndex = uvc_descriptor.getFormatIndex(i);
                        String[] textmessage = new String [formatIndex.numberOfFrameDescriptors];
                        String inp;
                        for (int j=0; j<formatIndex.numberOfFrameDescriptors; j++) {



                }




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










        String input = (String) JOptionPane.showInputDialog(null, "             Select the FormatIndex              ", "This Videoformats are supported of your camera", JOptionPane.QUESTION_MESSAGE, null,  textmsg, textmsg[textmsg.length-1]);
        if (input != null) {
            svideoformat = (input.toString());
            for (int i=0; i<textmsg.length; i++) {
                if (input.matches(textmsg[i]) ) {
                    scamFormatIndex = numberFormatIndexes[i];
                    formatIndex = phrasedUvcDescriptor.getFormatIndex(i);
                    String[] textmessage = new String [formatIndex.numberOfFrameDescriptors];
                    String inp;
                    for (int j=0; j<formatIndex.numberOfFrameDescriptors; j++) {
                        frameIndex = formatIndex.getFrameIndex(j);
                        StringBuilder stringb = new StringBuilder();
                        stringb.append(Integer.toString(frameIndex.wWidth));
                        stringb.append(" x ");
                        stringb.append(Integer.toString(frameIndex.wHeight));
                        textmessage[j] = stringb.toString();
                    }
                    inp = (String) JOptionPane.showInputDialog(null, "Select the camera Resolution", "Following Resolutions are supported:", JOptionPane.QUESTION_MESSAGE, null,  textmessage, textmessage[textmessage.length-1]);
                    if (inp != null) {
                        for (int j=0; j<formatIndex.numberOfFrameDescriptors; j++) {
                            if (inp.equals(textmessage[j])) {
                                frameIndex = formatIndex.getFrameIndex(j);

                                scamFrameIndex = frameIndex.frameIndex;
                                System.out.println("scamFrameIndex = " + scamFrameIndex);
                                simageWidth = frameIndex.wWidth;
                                simageHeight = frameIndex.wHeight;

                                String [] text = new String [frameIndex.dwFrameInterval.length];
                                for (int k=0; k<text.length; k++) {
                                    text[k] = Integer.toString(frameIndex.dwFrameInterval[k]);
                                }
                                String textInput = (String) JOptionPane.showInputDialog(null, String.format("Example: 333333 = 30 fps (Frames per Secound);   666666 = 15 fps\nA higher value means less Pictures per Secound\nOne Frame is one Picture."), "Select the FrameIntervall", JOptionPane.QUESTION_MESSAGE, null,  text, text[text.length-1]);
                                if (textInput != null) {
                                    scamFrameInterval = Integer.parseInt(textInput);
                                    System.out.println("scamFrameInterval = " + scamFrameInterval);
                                }
                            }
                        }
                    }






                }
            }

        }


        */
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