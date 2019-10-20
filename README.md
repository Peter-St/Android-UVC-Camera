# Android-UVC-Camera

Still under development.... 

Link on Play Store:
https://play.google.com/store/apps/details?id=humer.uvc_camera&hl=de




This is a Android Studio Project. It connects to a usb camera from your Android Device. (OTG cabel or OTG Hub needed)

# This Project was built to perform an Isochronous Video Stream from all Android Devices (Above 4.1 Ice Cream Sandwich)(Mediathek Devices too).

The program uses the usb device driver to perform an isochronous transfer with your camera device.

- When you click on 'Set up the Camera Device' Button, the app searches for a connected camera. (Usb OTG Cable required)
- First you have to set up all camera settings for your device. The program then saves the values and you can restore them later or overwrite them with other values. There is a built-in service included to automatically set up the values ("Set Up With UVC Values Button").




License
-------

    Copyright 2019 Peter Stoiber

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.






Explaination:
- When the automatic search succeeds, you first set up the MAXIMAL PACKET SIZE. If your device is a mediathek device, you may have to lower the value for the max packet size. Here your camera device supports normaly 4 to 10 different values.
- The Value PACKETS PER REQUEST defines the Number of the Packets sending to the camera device. It also defines the amount of bytes from one Urb (UsbRequestBlock). The minimal size is one packet and you can raise it up to maybe 64.
- Next value to set is the USB REQUEST BLOCKS (activeUrb) (Urb):  One Urb has a size of "MaxPacketsize x PacketsPerRequest". One Urb is the lowest value and you can raise it up to maybe 64. You have to find here the right values for your device and control the output on the screen under the menupoint "Isoread". The Urbs defines the amount of bytes which are currently available at your camera device. 
- Some typically values for Qualcom Devices are: 8 for the activeUrbs and 16 Packets per Request....
- Each camera and Phone needs different values and can proceed to find the right values for your devices.

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
