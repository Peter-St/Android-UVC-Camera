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
