# Android-UVC-Camera

Still under development.... 

This is a Android Studio Project. It connects to a usb camera from your Android Device. (OTG cabel or OTG Hub needed)

Explaination: (To know how the program works) (You can make all the main changes in the file: Kam.java)

Read out the camera specifications with the terminal command:
list with lsusb to get all connected devices
Detail informations can be received with Vendor+Produkt ID: lsusb -v -d <vendor>:<product>
Variant over: lsusb -v -s <bus>:<device>

You can compile the project at ones and use it for your usb camera.

- you have to set up all camera settings for your device. The program then saves the values and you can restore them latet or overwrite them with other values. Use the "Edit/Save/Restore" Button to adjust the values.
- Next you can connect to your camera and first you will send a controlltransfer to your camera
- If the controlltransfer is successful, than you are ready to go.
- Next try out the method Isoread and take a look at the frames.
- When you receive identically and long frames, you can proceed to the method Isostream, where the frames were displayed on your screen.



IsoRead Button:
Take a look at the camera frames you receive with your settings on STDOUT. To know how big be a Frame should be, you can look at the output of the controll transfer of the camera in the log: maxVideoFrameSize, This value is returned from the camera and should be the valid frame size (The value is calculated by Imagewidth x Imagehight x 2).

The IsochronousRead1 class shows you how the frames are structered by the camera. Different camerasetting == Different Frame structers. Try it out with different setting and look at the output. The eof hint shows the framesize in the log. For valid camera settings the size should be the same as maxFrameSize value of the controlltransfer. You can use the search function in the log ...


Output method Isoread: (Controltransfer)
Thirst the program will send a controlltransfer to your camera device. The output of it looks as following:
Initial streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=0 maxPayloadTransferSize=0
Probed streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=614400 maxPayloadTransferSize=3000
Final streaming parms: hint=0x0 format=1 frame=1 frameInterval=2000000 keyFrameRate=0 pFrameRate=0 compQuality=0 compWindowSize=0 delay=0 maxVideoFrameSize=614400 maxPayloadTransferSize=3000
The first line are the values you set in the program, to connect the camera. (Initial streaming parms}

The secound line are the values from the camera, which the camera returned from your values.

And in the third line are the new saved and final values from the usb camera.

Outpuf from the first Method: isoRead:

(sample)

I/UsbCamTest1: requests=317 packetCnt=317 packetErrorCnt=0 packet0Cnt=5, packet12Cnt=0, packetDataCnt=312 packetHdr8cCnt=123 frameCnt=57
I/UsbCamTest1: 1/0 len=1280 data=0c 8c 00 00 00 00 9c 1e 4b 4b 31 05 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80
I/UsbCamTest1: 2/0 len=1280 data=0c 8c 00 00 00 00 0c d5 66 4b 3f 06 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80
I/UsbCamTest1: 13/0 len=304 data=0c 8e 00 00 00 00 1f a3 fd 4b f7 03 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 10 80 EOF frameLen=10436
The first line shows a summary of the output. Here you see How many frame were collected, how many errors have been detected and you can take a look at the packetsize of the requests.

The secound line is a line from a packet of a URB: 1/0 means first paket in request number 0. --> 2/0 means second package .. The data shows the offsets wich were transmitted.

The third line shows the data of a package and the hint: EOF frameLen=10436. --> For Example here a frame ends with a length of 10436 wich is not 614400 as we expected from the controltransfer, so you may have to change some values of you program to get a valid frame size.
