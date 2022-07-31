# Android-UVC-Camera

Still under development....


Link on Play Store:
https://play.google.com/store/apps/details?id=humer.uvc_camera&hl=de


This is a Android Studio Project. It connects to a usb camera from your Android Device. (OTG cabel or OTG Hub needed) (It works with Micro Usb and Usb Type C devices)


# This Project was built to perform an Isochronous Video Stream from all Android Devices (Above 4.0.4 Ice Cream Sandwich)(Mediathek Devices too).

The app connects to USB Cameras via variable, different input values for the camera driver creation.
In most cases you won't need to set up your own camera driver, because other apps may do this for you automatically, but for some Android devices it could help to watch videos from Usb Cameras.

(Some OTG cabels doesn't work -->  I'll found one which is an extern powered more Port USB-C OTG cable and doesn't work ...)
(An non working OTG cable doesn't show the right interfaces and endpoints of you camera: --> When you click on 'List Up The Camera' Button)


License
-------

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



Since Android 9 Usb Cameras on Android needs several other permissions and at least the SdkVersion 30 to run sucessfully
--> https://issuetracker.google.com/issues/145082934



Explaination:
Before you start with entering the Camera values, your Android device have to detect the Camera:
So you click the Button <Set Up The Usb Device> and then the Button <Find the Camera>. The app will ask you for granting the permissions.

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/findcam.png?raw=true)


In the Picture above a camera was found and the Permissions to the Camera are granted.
If no Camera is detected, you can nott use this app. (except of the WebRTC function).
Next you have read out the camera Interfaces, to see if your camera is UVC complient.
So you click the Button: <List Up The Camera>:

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/listdev.png?raw=true)

Here you can see a sucessful return of an UVC compliant camera. The first Interface is always the ControlInterface and the second is always the Stream Interface.
This Controlinterface (1st) has only 1 Endpoint.
The StreamInterface could have more than one Endpoints (depends on the supported resolution of the camera).
Here the Endpoint of the StreamInterface has a maxPacketSize of 3072 bytes (You need such a value later).
(It could be, that your camera has an audio output, or Sd-Card output too, than see more than 2 Interfaces for your device).
If those two Buttons work correctly, you can start to set up the UVC values:
Click the Button <Set Up With UVC Settings> to start the camera setup.
You have two pissibilies: The <manual> and the <automatic> Method. The <automatic> method is in beta stadium for now, so if this button fails, you choose the <manual> method next time!
The automatic method should find working camera values for its own, but this values may not be optimal for video transmission. To choose the right values for your own, you click on the <Manual> Button.

Manual Method Explained:
First you choose you Maximal Packet Size:
You Camera may supports more Values for the MaxPacketSize, so you can test out each of them, which works best. If your phone uses a Mediathek Chipset you may choose the smallest value, but normaly you choose the highest value!
Click on <Done> and proceed to the Packets Per Request selection:
The Values you select builds the size of the Stream. This means if you select higher Values (such as 32 or more ..) you stream gets bigger, but there may could result error from your device or the camera because of a too large amount of data.
For the start you select 1 for this Value. (This would be definitly to less, but you can raise it later) ..
Next Sceen shows the ActiveUrbs (actice Usb Request Blocks) --> This is also a value which represents the size of the camera stream. One Block of the activeUrbs is exactly the maxPacketSize x packetsPerRequest. You can select 1 for the start (You will have to raise it later ..)
Then the Setup Method will ask you for the Camera Format, which your camera supports. If there is only one format, you click on <Done>, if there were more, you select one (does not matter which one) (eventually MJpeg if present) and click on <Done>.
Next you have to select you Camera Resolution, which you camera supports with the Format (perhaps your camera supports other resolutions, with the other Format ...). Select something ... and click on <Done>
Then you have to select the Frame Interval, which your camera supports. You can click on a Value (maybe the lowest on displayed on the screen, because it is better for the setup.
You can save your Entries now:
Click on <Yes, Save> to save this values (you do not need to run the method again, if you have finished the setup and found some working values ...
If you click on <ok> in the next screen --> an automatic name will be taken from the camera to save the file. You can also enter a unique name or enter the value, which is displayed on the bottom, to choose an existing file.

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/setup_complete.png?raw=true)

The picture shows the output of the sucessful setup:
Now you want to know, if your Camera works with your selected Values. So you start the comunication with your camera by clicking on the Button <-Controltransfer-Testrun-> and then you select the first entry <Video Probe-Commit Controls>.
The app then starts to initialize the camera with your selected values!

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/controltransfer.png?raw=true)

If you output is the same like on the Picture above, you have sucessfuly initialized your camera with your selected values.
If you do not get a sucessful output of this Button (Video Probe-Commit Control) you can not use the app with your phone / camera.
If this output fails, you may contact the developer or try to run the manual Uvc setup method again.
If you were sucessful, then you can proceed with reading out the camera frames (first only over the Text View, because your frames may be corrupted or too short or ..
To receive some data from your camera, your press the Button <-Controltransfer-Testrun-> again and then on <Testrun> and then on <One Frame>.
You then should receive the first frame data from your camera.

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/one_frame.png?raw=true)

The first data which was received on the picture above, were 39950 byte in this case.
This means that the camera communicats with the android device and trys to submit the frame data. But the data received above is much to less for a frame Image of a sample size of 640 x 480. So tbe Values "ActiveUrbs" and "PacketsPerRequest" should be raised to get a better output: in my case to 16 Packets and 16 Urbs.
If you were not able to get the frame data from your camera, then re-run the manual setup method and change same values. (lower the maxPacketSize, raise activeUrbs, raise packetsPerRequest).
To raise the vales for (packetsPerRequest and activeUrbs) you can re-run the <manual> setup method or you click on the Button <Edit / Save The Camera Values>
There you should only change the values for these two values (packetsPerRequest and activeUrbs).
If you re-run the manual method, you can keep your values by clicking on done, or type in new values to change em.

![alt text](https://github.com/Peter-St/Android-UVC-Camera/blob/master/app/src/main/res/drawable/one_frame_2.png?raw=true)

Here you see, that the frame now gots larger --> 614400 --> this is exact the size of imageWidth x ImageHight x 2 --> 640 x 480 x 2.
So in the shown case, the correct setting was found and now the camera setup is finished!
Sometimes cameras compresses the frames they send to the devices, so in some cases you have to receive frames with a nearly same size too each other (use the 2nd method from the testrun menu --> "Frames for 5 seconds")
When all frames are identically (or nearly) and also all methods above were sucessful, you have sucessfuly finish the setup.
You will sure have to spend more time to figure out the right values, but if you have found them, save them and later you can easy restore them any time.
If you want to delete some savefiles you will have to manually do this with a file explorer (but only this one you do not need any more)
To the Button "Frames for 5 secound":
Have a look at your frames: If they are all identically and big enougth, than you can proceed to start the camera stream with the <Start the camera stream> button from the main screen.
Otherwise you have to edit some values for your camera: Perhaps -PacketsPerRequest- or -ActiveUrbs-, or something else.
Click the <Transmission Start> --> <Start the Camera> Button to start the transmission.
If everything works, you can watch the video of your camera.

Descripton of the Camera Values:

- Alt-Setting:    The Alt-Setting is a camera specific setting which defines which interface of your camera shall be used for the isochronous data transfer.
- Maximal-Packet-Size:    Each Alt-Setting has its own Maximal-Packet-Size. The Maximal-Packet-Size is byte Number which defines how many bytes each Packet of the iso transfer maximal contains.

- Format-Index:     Your Camera can support different Format Indexs. This could be MJPEG, or uncompressed (YUV) or ...
- Videoformat:    This is a helper value, which has to be set to your selected Format-Index. You have to enter YUV, or MJPEG, or ...

- Frame-Index:    Each Format-Index of your camera can have different Frame Indexs. This is a number which represents the camera resolution (1920x1020, or 640x480)

- Image-Width, Image-Hight:  This are helper values, which have to be set to your selected Frame-Index. For Example: Image-Width = 640 and Image-Hight = 480

- Frame-Interval:    The Frame-Interval is a number which defines, on how much nano secounds each new frame will be sent from the camera to your device.For example: 666666 means each 0,066 Secounds a frame is sent. 666666 = 15 frames per secound.333333 are 30 Frames per secound

- Packets-Per-Request:   This is a value wich defines how many Packet will be sent from your camera to your device in one cycle of the transfer.

- Active-Urbs:    And this value defines have many cycles are running paralell to each other in the data steam between camera and Android device.



Good Luck and fun during testing!
