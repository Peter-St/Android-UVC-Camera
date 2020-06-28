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

import com.sun.jna.Platform;

public class Ioctl {

    public Ioctl() {
    }

    public static final int _IOC_NRBITS = 8;
    public static final int _IOC_TYPEBITS = 8;

    public static final int _IOC_SIZEBITS = get_IOC_SIZEBITS();
    public static final int _IOC_DIRBITS = get_IOC_DIRBITS();

    public static final int _IOC_NRMASK = ((1 << _IOC_NRBITS) - 1);
    public static final int _IOC_TYPEMASK = ((1 << _IOC_TYPEBITS) - 1);
    public static final int _IOC_SIZEMASK = ((1 << _IOC_SIZEBITS) - 1);
    public static final int _IOC_DIRMASK = ((1 << _IOC_DIRBITS) - 1);

    public static final int _IOC_NRSHIFT = 0;
    public static final int _IOC_TYPESHIFT = _IOC_NRSHIFT + _IOC_NRBITS;
    public static final int _IOC_SIZESHIFT = _IOC_TYPESHIFT + _IOC_TYPEBITS;
    public static final int _IOC_DIRSHIFT = _IOC_SIZESHIFT + _IOC_SIZEBITS;

    public static final int _IOC_NONE = get_IOC_NONE();
    public static final int _IOC_WRITE = get_IOC_WRITE();
    public static final int _IOC_READ = get_IOC_READ();

    private static int get_IOC_SIZEBITS() {
        if(Platform.isSPARC() || Platform.isPPC() || Platform.isMIPS()) {
            return 13;
        } else {
            return 14; // Default, depends on platform
        }
    }

    private static int get_IOC_DIRBITS() {
        if(Platform.isSPARC() || Platform.isPPC() || Platform.isMIPS()) {
            return 3;
        } else {
            return 2;// Default, depends on platform
        }
    }

    private static int get_IOC_NONE() {
        if(Platform.isSPARC() || Platform.isPPC() || Platform.isMIPS()) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int get_IOC_WRITE() {
        if(Platform.isSPARC()) {
            return 3;
        } else if (Platform.isPPC() || Platform.isMIPS()) {
            return 4;
        } else {
            return 1;
        }
    }

    private static int get_IOC_READ() {
        return 2;
    }

    public static final int _IOC(int dir, int type, int nr, int size) {
        return (dir << _IOC_DIRSHIFT)
                | (type << _IOC_TYPESHIFT)
                | (nr << _IOC_NRSHIFT)
                | (size << _IOC_SIZESHIFT);
    }

    public static final int _IO(int type, int nr) {
        return _IOC(_IOC_NONE, type, nr, 0);
    }

    public static final int _IOR(int type, int nr, int size) {
        return _IOC(_IOC_READ, type, nr, size);
    }

    public static final int _IOW(int type, int nr, int size) {
        return _IOC(_IOC_WRITE, type, nr, size);
    }

    public static final int _IOWR(int type, int nr, int size) {
        return _IOC(_IOC_READ | _IOC_WRITE, type, nr, size);
    }
}
