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

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public interface Libc extends Library {

    public static final Libc INSTANCE = Native.load("c", Libc.class);

    public static final int O_RDONLY = 00;
    public static final int O_WRONLY = 01;
    public static final int O_RDWR = 02;

    int open(String path, int flags) throws LastErrorException;

    int close(int fd) throws LastErrorException;

    int ioctl(int fileHandle, int request, PointerByReference p) throws LastErrorException;

    // edited by Peter Stoiber August 2020
    int ioctl(int fileHandle, int request, Pointer p) ;

    int ioctl(int fileHandle, int request, long l) throws LastErrorException;

    int ioctl(int fileHandle, int request, IntByReference l) throws LastErrorException;

    int ioctl(int fileHandle, int request, Structure s) throws LastErrorException;
}
