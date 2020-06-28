/*
 * Copyright (c) 2019, Matthias Bläsing
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0.
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */

package humer.UvcCamera.UsbIso64;

import com.sun.jna.ptr.IntByReference;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_GET_CAPABILITIES;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_SETINTERFACE;
import java.io.IOException;

public class usbdevice_fs_util {
    /**
     * Sends a SET_INTERFACE command to the USB device.
     *
     * @param interfaceId The interface ID.
     * @param altSetting  The alternate setting number. The value 0 is used to
     *                    stop streaming. You may use
     *                    <code>lsusb -v -d xxxx:xxxx</code> to find the
     *                    alternate settings available for your USB device.
     */
    public static void setInterface(int fileDescriptor, int interfaceId, int altSetting) throws IOException {
        usbdevice_fs.usbdevfs_setinterface p = new usbdevice_fs.usbdevfs_setinterface();
        p.interfaceId = interfaceId;
        p.altsetting = altSetting;
        Libc.INSTANCE.ioctl(fileDescriptor, USBDEVFS_SETINTERFACE, p);
    }

    public int getCapabilities(int fileDescriptor) {
        IntByReference resBuffer = new IntByReference();
        Libc.INSTANCE.ioctl(fileDescriptor, USBDEVFS_GET_CAPABILITIES, resBuffer);
        return resBuffer.getValue();
    }
}
