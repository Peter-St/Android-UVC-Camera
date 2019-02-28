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

import android.os.Environment;
import android.util.Log;
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
    public static int scamFormatIndex ;   // MJPEG // YUV // bFormatIndex: 1 = uncompressed
    public static int scamFrameIndex ; // bFrameIndex: 1 = 640 x 360;       2 = 176 x 144;     3 =    320 x 240;      4 = 352 x 288;     5 = 640 x 480;
    public static int simageWidth;
    public static int simageHeight;
    public static int scamFrameInterval ; // 333333 YUV = 30 fps // 666666 YUV = 15 fps
    public static int spacketsPerRequest ;
    public static int smaxPacketSize ;
    public static int sactiveUrbs ;
    public static int svideoformat;
    private static String saveFilePath = "save/saveFile.sav";

    Main uvc_camera;

    private boolean abfrage = true;

    static ArrayList<String> paths = new ArrayList<>(50);
    private static ArrayList<String> saveValues = new ArrayList<>(20);

    public SaveToFile(Main main) {
        this.uvc_camera = main;

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



    public void startRestore() {
        //initKamClass();
        int option;
        String name;
        paths = new ArrayList<>(50);
        StringBuilder stringBuilder = new StringBuilder();

        File s = new File(saveFilePath).getAbsoluteFile();
        s.getParentFile().mkdirs();
        String filePath = s.getParent();



        //String path = Environment.getExternalStorageDirectory().toString()+"/Pictures";
        log("Path: " + filePath);
        File directory = new File(filePath);
        File[] files = directory.listFiles();
        Log.d("Files", "Size: "+ files.length);
        for (int i = 0; i < files.length; i++)
        {
            Log.d("Files", "FileName:" + files[i].getName());
        }
        filePath += "/";

        for (int i = 0; i < paths.size(); i++) {
            stringBuilder.append(String.format("%d   ->   ", (i+1)));
            stringBuilder.append(paths.get(i));
            stringBuilder.append("\n");
        }

        /*
        name = JOptionPane.showInputDialog(String.format("Please type the name of the restore file.\nFollowing files are stored in the directory:\n \n%s\n\n   To select the first file type in 1, or for the secound file 2\n   Or type in a name (without the Directory) (for example: camera)" , stringBuilder.toString() ));
        if (name == null) JOptionPane.showMessageDialog(null, "save canceled","Save Canceled", JOptionPane.INFORMATION_MESSAGE) ;
        else if (name.isEmpty() == false) {
            if (isInteger(name) == true) {
                try { restorFromFile(paths.get((Integer.parseInt(name) - 1))); }
                catch (Exception e) { Logger.getLogger(Kam.class.getName()).log(Level.SEVERE, null, e); JOptionPane.showMessageDialog(null, "Error restoring the file","Error while restoring the file", JOptionPane.ERROR_MESSAGE);}
            } else {restorFromFile((filePath += name += ".sav"));      JOptionPane.showMessageDialog(null, "save complete","Save Complete", JOptionPane.INFORMATION_MESSAGE);}
        }

        System.out.println("Restore completed");
        System.out.println("restore bus = " + sbus);
        System.out.println("restore camFrameInterval  =  " + scamFrameInterval);
        System.out.println("ALT_SETTING = " + sALT_SETTING);
        System.out.println("camFormatIndex = " + scamFormatIndex);
        System.out.println("camFrameIndex = " + scamFrameIndex);
        System.out.println("camFrameInterval = " + scamFrameInterval);
        System.out.println("imageWidth = " + simageWidth);
        System.out.println("imageHeight = " + simageHeight);
        System.out.println("devpath = " + sdevicePath);
        System.out.println("videoformat = " + svideoformat);
        */
    }



    public void startEditSave() {
        /*
        initKamClass();
        fetchTheValues();


        JTextField ALT_SETTING0 = new JTextField();
        JTextField maxPacketSize0 = new JTextField();
        JTextField camFormatIndex0 = new JTextField();
        JTextField videoformat0 = new JTextField();
        JTextField camFrameIndex0 = new JTextField();
        JTextField imageWidth0 = new JTextField();
        JTextField imageHeight0 = new JTextField();
        JTextField camFrameInterval0 = new JTextField();
        JTextField packetsPerRequest0 = new JTextField();
        JTextField activeUrbs0 = new JTextField();

        Object[] message = {
                "ALT_SETTING:             Typ in the Camera ALT_SETTING: --> normaly from 0 to 10 ..." + String.format("\n    Stored Value: ALT_SETTING = %d", sALT_SETTING), ALT_SETTING0,
                "MaxPacketSize:           Typ in the Camera MaxPacketSize: --> normaly from 1 to 2 ..."+ String.format("\n     Stored Value: maxPacketSize = %d", smaxPacketSize) , maxPacketSize0,
                "CamFormatIndex:          Typ in the Camera CamFormatIndex: --> normaly from 1 to 5 ... (This represents the Resolution)"+ String.format("\n     Stored Value: camFormatIndex = %d", scamFormatIndex) , camFormatIndex0,
                "MJPEG = 0   // YUV = 1:  Typ in the Camera Frameformat: MJPEG = 0 YUV = 1 (means uncompressed) Note: Uncompressed are at least 10 different formats.\n      So the picture could be displayed in a wrong color way ... This format is for YUY2" + String.format("\n     Stored Value: VideoSetting = %d", svideoformat), videoformat0,
                "CamFrameIndex:           Typ in the Camera camFrameIndex: --> normaly from 1 to 5 ...(This represents the Resolution)" + String.format("\n     Stored Value: camFrameIndex = %d", scamFrameIndex), camFrameIndex0,
                "ImageWidth:              Typ in the Camera imageWidth: --> Some formats are 640x480 or 1920x1240 ...(Only type in the Width --> 640 or 1920 .." + String.format("\n      Stored Value: imageWidth = %d", simageWidth), imageWidth0,
                "ImageHeight:             Typ in the Camera imageHeight: --> (Only type in the Width --> 480 or 1240 .." + String.format("\n     Stored Value: imageHeight = %d", simageHeight), imageHeight0,
                "CamFrameInterval         Typ in the Camera camFrameInterval:  333333 --> means 30 fps (Frames per secound)\n              666666 ---> means 15 fps 1000000 = 10 fps    2000000 = 5 fps:" + String.format("\n     Stored Value: camFrameInterval = %d", scamFrameInterval), camFrameInterval0,
                "PacketsPerRequest:       Typ in the Camera packetsPerRequest:  (at least 1 packet up to 8 or 32 or 64 or 128 or ..." + String.format("\n     Stored Value: packetsPerRequest = %d", spacketsPerRequest), packetsPerRequest0,
                "ActiveUrbs:              Typ in the Camera activeUrbs: At least 1 active URB (USB REQUEST BLOCK) up to 8, or 16, 64, or ..." + String.format("\n     Stored Value: activeUrbs = %d", sactiveUrbs), activeUrbs0,
        };
        // password.getText().equals("h")
        int option = JOptionPane.showConfirmDialog(null, message, "Edit the Camera Values   --->  You can leave Fields blank. When you enter no value, the stored value will be kept.", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            if (ALT_SETTING0.getText().isEmpty() == false)  sALT_SETTING = Integer.parseInt(ALT_SETTING0.getText());
            if (maxPacketSize0.getText().isEmpty() == false)  smaxPacketSize = Integer.parseInt(maxPacketSize0.getText());
            if (camFormatIndex0.getText().isEmpty() == false)  scamFormatIndex = Integer.parseInt(camFormatIndex0.getText());
            if (videoformat0.getText().isEmpty() == false) svideoformat = Integer.parseInt(videoformat0.getText());
            if (camFrameIndex0.getText().isEmpty() == false)  scamFrameIndex = Integer.parseInt(camFrameIndex0.getText());
            if (imageWidth0.getText().isEmpty() == false)  simageWidth = Integer.parseInt(imageWidth0.getText());
            if (imageHeight0.getText().isEmpty() == false)  simageHeight = Integer.parseInt(imageHeight0.getText());
            if (camFrameInterval0.getText().isEmpty() == false)  scamFrameInterval = Integer.parseInt(camFrameInterval0.getText());
            if (packetsPerRequest0.getText().isEmpty() == false)  spacketsPerRequest = Integer.parseInt(packetsPerRequest0.getText());
            if (activeUrbs0.getText().isEmpty() == false)  sactiveUrbs = Integer.parseInt(activeUrbs0.getText());
            System.out.println("Input saved");
            //writeTheValues();

            Object[] options = {"Save to a File", "Don't Save !"};
            option = JOptionPane.showOptionDialog(null, "Would you like to save the settings to a file?" ,"Save the Settings ?", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,options , options[0]);
            if (option == JOptionPane.OK_OPTION) {
                String name;
                /*
                Object[] options2 = {"Use the standard path", "Select new filepath"};
                option = JOptionPane.showOptionDialog(null, String.format("Would you like to use the standard filepath?\nThe Filepath is:   %s" , rootPath ),"Filepath ...", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,options2 , options2[0]);
                if (option != JOptionPane.OK_OPTION){
                    name = JOptionPane.showInputDialog("Please type in the Path:   (Example:    /home/user/camera/  )");
                    rootPath = name;
                }
                *//*
                paths = new ArrayList<>(50);

                File s = new File(saveFilePath).getAbsoluteFile();
                s.getParentFile().mkdirs();
                String filePath = s.getParent();
                filePath += "/";

                recursiveFind(Paths.get(s.getParent()), System.out::println);
                //recursiveFind(Paths.get(rootPath), p -> {if (p.toFile().getName().toString().equals("src")) { System.out.println(p); }});
                System.out.println("Anzahl der Dateien: " + paths.size() + "\n");
                for (int i = 0; i < paths.size(); i++) {
                    System.out.println( paths.get(i) );
                }
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < paths.size(); i++) {
                    stringBuilder.append(String.format("%d   ->   ", (i+1)));
                    stringBuilder.append(paths.get(i));
                    stringBuilder.append("\n");
                }
                s = null;

                name = JOptionPane.showInputDialog(String.format("Please type the name of the savefile.\n   Following Files were stored in the directory:\n \n%s\n\n To select the First File Type in 1, or for the secound File 2\nOr Type in a name (without the Directory) (for example: camera)" , stringBuilder.toString() ));
                if (name == null) JOptionPane.showMessageDialog(null, "save canceld","Save canceld", JOptionPane.INFORMATION_MESSAGE);
                else if (name.isEmpty() == false) {
                    if (isInteger(name) == true) {
                        try { saveValueToFile(paths.get((Integer.parseInt(name)) - 1)); }
                        catch (Exception e) { Logger.getLogger(Kam.class.getName()).log(Level.SEVERE, null, e); JOptionPane.showMessageDialog(null, "Error saving the file","Error while saving the file", JOptionPane.ERROR_MESSAGE);}
                    } else {saveValueToFile(filePath  += name += ".sav");      }
                }
            }
        } else  System.out.println("Input canceled");
*/
    }

    private void saveValueToFile (String savePath) {

        System.out.println("savePath = " + savePath);
        try {  // Catch errors in I/O if necessary.
            /*
        File dump = new File(DUMP_FILE).getAbsoluteFile();
        dump.getParentFile().mkdirs();
        */

            File file = new File(savePath).getAbsoluteFile();
            file.getParentFile().mkdirs();
            if (file.exists())  file.delete();

            FileOutputStream saveFile=new FileOutputStream(savePath);

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
            save.writeObject(saveFilePath);

            // Close the file.
            save.close(); // This also closes saveFile.
        } catch (Exception e) { log("Error");}

      //  JOptionPane.showMessageDialog(null, "save complete","Save Complete", JOptionPane.INFORMATION_MESSAGE);
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
            saveFilePath  = (String) save.readObject();
            save.close();
        }
        catch(Exception exc){
            exc.printStackTrace();
        }
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




}