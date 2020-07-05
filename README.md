# Android-UVC-Camera

Still under development.... 

Link on Play Store:
https://play.google.com/store/apps/details?id=humer.uvc_camera&hl=de


This is a Android Studio Project. It connects to a usb camera from your Android Device. (OTG cabel or OTG Hub needed) (It works with Micro Usb and Usb Type C devices)


# This Project was built to perform an Isochronous Video Stream from all Android Devices (Above 4.1 Ice Cream Sandwich)(Mediathek Devices too).

The app connects to USB Cameras via variable, different input values for the camera driver creation.
In most cases you won't need to set up your own camera driver, because other apps may do this for you automatically, but for some Android devices it could help to watch videos from Usb Cameras.

- LibUsb Support Added: LibUsb raises the performance of the standard Usb Device Driver.

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



Since Android 9 Google made a mistake granting the Usb Permissions for Usb Cameras, so this app may not works on Android 9 + Devices
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





Explaination:
- When the automatic search succeeds, you first set up the MAXIMAL PACKET SIZE. If your device is a mediathek device, you may have to lower the value for the max packet size. Here your camera device supports normaly 4 to 10 different values.
- The Value PACKETS PER REQUEST defines the Number of the Packets sending to the camera device. It also defines the amount of bytes from one Urb (UsbRequestBlock). The minimal size is one packet and you can raise it up to maybe 64.
- Next value to set is the USB REQUEST BLOCKS (activeUrb) (Urb):  One Urb has a size of "MaxPacketsize x PacketsPerRequest". One Urb is the lowest value and you can raise it up to maybe 64. You have to find here the right values for your device and control the output on the screen under the menupoint "Isoread". The Urbs defines the amount of bytes which are currently available at your camera device. 
- Some typically values for Qualcom Devices are: 8 for the activeUrbs and 16 Packets per Request....
- Each camera and Phone needs different values! Now you have to find the right values for your devices.

Isoread:
The first thing of the method Isoread is a Controltransfer to the camera device:

- If the controlltransfer is successful, than you are ready to go.
- Next take a look at the frames.
- When you receive identically and long frames, you can proceed to the method Isostream, where the frames were displayed on your screen.



- To know how big be a Frame should be, you can look at the output of the controll transfer of the camera in the log: maxVideoFrameSize, This value is returned from the camera and should be the valid frame size (The value is calculated by Imagewidth x Imagehight x 2).

The ReadOneFrame method shows you how the frames are structered by the camera. Different camerasetting == Different Frame structers. Try it out with different setting and look at the output. The eof hint shows the framesize in the log. For valid camera settings the size should be the same as maxFrameSize value of the controlltransfer.


Output from the controltransfer method:
Thirst the program will send a controlltransfer to your camera device. The output maybe looks as following:
Initial streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=0 maxPayloadTransferSize=0
Probed streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=614400 maxPayloadTransferSize=3000
Final streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=614400 maxPayloadTransferSize=3000
The first line are the values you set in the program, to connect the camera. (Initial streaming parms}

The secound line are the values from the camera, which the camera returned from your values.

And in the third line are the new saved and final values from the usb camera.

Outpuf from the first Method: isoRead:

EOF frameLen=10436. --> For Example here a frame ends with a length of 10436 wich is not 614400 as we expected from the controltransfer, so you may have to change some values of you program to get a valid frame size.

Good Luck and fun during testing!
