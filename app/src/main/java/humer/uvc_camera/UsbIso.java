// Copyright 2015 Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland
// www.source-code.biz, www.inventec.ch/chdh
//
// This module is multi-licensed and may be used under the terms of any of the following licenses:
//
//  LGPL, GNU Lesser General Public License, V2.1 or later, http://www.gnu.org/licenses/lgpl.html
//  EPL, Eclipse Public License, V1.0 or later, http://www.eclipse.org/legal
//
// Please contact the author if you need another license.
// This module is provided "as is", without warranties of any kind.
//
// Home page: http://www.source-code.biz/snippets/java/UsbIso

package humer.uvc_camera;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import humer.uvc_camera.UsbIso;


/**
 * An USB isochronous transfer controller.
 * <p>
 * This class is used to read and write from an isochronous endpoint of an USB device.
 * It uses JNA to access the USBFS API via IOCTL calls.
 * USBFS is available in the Linux kernel and can be accessed from an Android application.
 * <p>
 * This class is independent of Android and could also be used under other Linux based operating systems.
 * <p>
 * The following program logic may be used e.g. for reading the video data stream from an UVC compliant camera:
 * <pre>
 *   ... set streaming parameters via control channel (SET_CUR VS_COMMIT_CONTROL, etc.) ...
 *   usbIso.preallocateRequests(n);
 *   usbIso.setInterface(interfaceId, altSetting);       // enable streaming
 *   for (int i = 0; i &lt; n; i++) {                       // submit initial transfer requests
 *      Request req = usbIso.getRequest();
 *      req.initialize(endpointAddr);
 *      req.submit(); }
 *   while (...) {                                       // streaming loop
 *      Request req = usbIso.reapRequest(true);          // wait for next request completion
 *      .. process received data ...
 *      req.initialize(endpointAddr);                    // re-use the request
 *      req.submit(); }                                  // re-submit the request
 *   usbIso.setInterface(interfaceId, 0);                // disable streaming
 *   usbIso.flushRequests();                             // remove pending requests</pre>
 *
 * Note that for e.g. an USB2 UVC camera, data packets arrive at a rate of 125 microseconds per packet.
 * This corresponds to 8000 packets per second. Each packet may contain up to 3072 bytes.
 *
 * @see <a href="https://www.kernel.org/doc/htmldocs/usb/usbfs.html">USBFS</a>
 * @see <a href="http://en.wikipedia.org/wiki/Java_Native_Access">JNA</a>
 * @see <a href="http://en.wikipedia.org/wiki/Ioctl">IOCTL</a>
 */
@SuppressWarnings({"PointlessBitwiseExpression", "unused", "SpellCheckingInspection"})
public class UsbIso {

// Note: The layout and size of the USBFS structures matches that of Linux Kernel 3.2 and 3.14
// for ARM 32 bit. For other environments (X86, 64 bit, future Linux kernels), it might be
// necessary to adjust some values.

    private static final int usbSetIntSize = 8;                // size of struct usbdevfs_setinterface
    private static final int USBDEVFS_URB_TYPE_ISO = 0;
    private static final int USBDEVFS_URB_ISO_ASAP = 2;

    // IOCTL function codes:
    private static final int USBDEVFS_SETINTERFACE = (2 << 30) | (usbSetIntSize << 16) | (0x55 << 8) | 4;
    private static final int USBDEVFS_SUBMITURB = (2 << 30) | (Urb.urbBaseSize << 16) | (0x55 << 8) | 10;
    private static final int USBDEVFS_DISCARDURB = (0 << 30) | (0 << 16) | (0x55 << 8) | 11;
    private static final int USBDEVFS_REAPURB = (1 << 30) | (Pointer.SIZE << 16) | (0x55 << 8) | 12;
    private static final int USBDEVFS_REAPURBNDELAY = (1 << 30) | (Pointer.SIZE << 16) | (0x55 << 8) | 13;
    private static final int USBDEVFS_CLEAR_HALT = (2 << 30) | (4 << 16) | (0x55 << 8) | 21;

    //--- Native data structures ---------------------------------------------------
    private static final int EAGAIN = 11;
    private static final int ENODEV = 19;

    //--- Request object -----------------------------------------------------------
    private static final int EINVAL = 22;

    //--- Main logic ---------------------------------------------------------------
    private static Libc libc;
    private static boolean staticInitDone;
    private int fileDescriptor;
    private ArrayList<Request> requests = new ArrayList<>();
    private int maxPacketsPerRequest;
    private int maxPacketSize;

    /**
     * Creates an isochronous transfer controller instance.
     * <p>
     * The size of the data buffer allocated for each <code>Request</code> object is
     * <code>maxPacketsPerRequest * maxPacketSize</code>.
     *
     * @param fileDescriptor       For Android, this is the value returned by UsbDeviceConnection.getFileDescriptor().
     * @param maxPacketsPerRequest The maximum number of packets per request.
     * @param maxPacketSize        The maximum packet size.
     */
    public UsbIso(int fileDescriptor, int maxPacketsPerRequest, int maxPacketSize) {
        staticInit();
        this.fileDescriptor = fileDescriptor;
        this.maxPacketsPerRequest = maxPacketsPerRequest;
        this.maxPacketSize = maxPacketSize;
    }

    private static synchronized void staticInit() {
        if (staticInitDone) {
            return;
        }
        if (new Usbdevfs_setinterface().size() != usbSetIntSize) {
            throw new RuntimeException("Value of usbSetIntSize constant does not match structure size.");
        }
        libc = (Libc) Native.loadLibrary("c", Libc.class);
        staticInitDone = true;
    }

    /**
     * Pre-allocates <code>n</code> {@link Request} objects with their associated buffer space.
     * <p>
     * The <code>UsbIso</code> class maintains an internal pool of <code>Request</code> objects.
     * Each <code>Request</code> object has native buffer space associated with it.
     * The purpose of this method is to save some execution time between the instant when
     * {@link #setInterface} is called to enable streaming, and the instant when {@link #reapRequest}
     * is called to receive the first data packet.
     *
     * @param n The minimum number of internal <code>Request</code> objects to be pre-allocated.
     */
    public void preallocateRequests(int n) {
        while (requests.size() < n) {
            new Request();
        }
    }

    /**
     * Releases all resources associated with this class.
     * This method calls {@link #flushRequests} to cancel all pending requests.
     */
    public void dispose() throws IOException {
        try {
            flushRequests();
        } catch (LastErrorException e) {
            // This happens when the device has been disconnected.
            if (e.getErrorCode() != ENODEV) {
                throw e;
            }
        }
    }

    /**
     * Cancels all pending requests and removes all requests from the queue of the USB device driver.
     */
    public void flushRequests() throws IOException {
        cancelAllRequests();
        discardAllPendingRequests();
        int queuedRequests = countQueuedRequests();
        if (queuedRequests > 0) {
            throw new IOException("The number of queued requests after flushRequests() is " + queuedRequests + ".");
        }
    }

    private void cancelAllRequests() throws IOException {
        for (Request req : requests) {
            if (req.queued) {
                req.cancel();
            }
        }
    }

    /**
     * Sends a SET_INTERFACE command to the USB device.
     * <p>
     * Starting with Android 5.0, UsbDeviceConnection.setInterface() could be used instead.
     *
     * @param interfaceId The interface ID.
     *                    For Android, this is the value returned by <code>UsbInterface.getId()</code>.
     * @param altSetting  The alternate setting number. The value 0 is used to stop streaming.
     *                    For Android, this is the value returned by <code>UsbInterface.getAlternateSetting()</code> (only available since Android 5.0).
     *                    You may use <code>lsusb -v -d xxxx:xxxx</code> to find the alternate settings available for your USB device.
     */
    public void setInterface(int interfaceId, int altSetting) throws IOException {
        Usbdevfs_setinterface p = new Usbdevfs_setinterface();
        p.interfaceId = interfaceId;
        p.altsetting = altSetting;
        p.write();
        int rc = libc.ioctl(fileDescriptor, USBDEVFS_SETINTERFACE, p.getPointer());
        if (rc != 0) {
            throw new IOException("ioctl(USBDEVFS_SETINTERFACE) failed, rc=" + rc + ".");
        }
    }

    /**
     * Returns an inactive <code>Request</code> object that can be submitted to the device driver.
     * <p>
     * The <code>UsbIso</code> class maintains an internal pool of all it's <code>Request</code> objects.
     * If the pool contains a <code>Request</code> object which is not in the request queue of the
     * USB device driver, that <code>Request</code> object is returned.
     * Otherwise a new <code>Request</code> object is created and returned.
     * <p>
     * The returned <code>Request</code> object must be initialized by calling {@link Request#initialize} and
     * can then be submitted by calling {@link Request#submit}.
     */
    public Request getRequest() {
        for (Request req : requests) {
            if (!req.queued) {
                return req;
            }
        }
        return new Request();
    }

//--- Static parts -------------------------------------------------------------

    /**
     * Returns a completed request.
     * <p>
     * A <code>Request</code> object returned by this method has been removed from the queue and can be re-used by calling {@link Request#initialize} and {@link Request#submit}.
     *
     * @param wait <code>true</code> to wait until a completed request is available. <code>false</code> to return immediately when no
     *             completed request is available.
     * @return A <code>Request</code> object representing a completed request, or <code>null</code> if
     * <code>wait</code> is <code>false</code> and no completed request is available at the time.
     */
    public Request reapRequest(boolean wait) throws IOException {
        PointerByReference urbPointer = new PointerByReference();
        int func = wait ? USBDEVFS_REAPURB : USBDEVFS_REAPURBNDELAY;
        int rc;
        try {
            rc = libc.ioctl(fileDescriptor, func, urbPointer);
        } catch (LastErrorException e) {
            if (e.getErrorCode() == EAGAIN && !wait) {
                return null;
            }
            throw e;
        }
        if (rc != 0) {
            throw new IOException("ioctl(USBDEVFS_REAPURB*) failed, rc=" + rc + ".");
        }
        int urbNdx = Urb.getUserContext(urbPointer.getValue());
        if (urbNdx < 0 || urbNdx >= requests.size()) {
            throw new IOException("URB.userContext returned by ioctl(USBDEVFS_REAPURB*) is out of range.");
        }
        Request req = requests.get(urbNdx);
        if (req.urbAddr != Pointer.nativeValue(urbPointer.getValue())) {
            throw new IOException("Address of URB returned by ioctl(USBDEVFS_REAPURB*) does not match.");
        }
        if (!req.queued) {
            throw new IOException("URB returned by ioctl(USBDEVFS_REAPURB*) was not queued.");
        }
        req.queued = false;
        req.initialized = false;
        return req;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void discardAllPendingRequests() throws IOException {
        // bypass if we have never allocated any request
        if (requests.size() == 0) {
            return;
        }

        // to prevent errors when the USB device has been accessed by another method (e.g. using android.hardware.usb.UsbRequest)
        while (reapRequest(false) != null) ;
    }

    private int countQueuedRequests() {
        int ctr = 0;
        for (Request req : requests) {
            if (req.queued) {
                ctr++;
            }
        }
        return ctr;
    }

    private interface Libc extends Library {

        int ioctl(int fileHandle, int request, PointerByReference p) throws LastErrorException;

        int ioctl(int fileHandle, int request, Pointer p) throws LastErrorException;

        int ioctl(int fileHandle, int request, int i) throws LastErrorException;
    }

    /**
     * This class is modeled after struct usbdevfs_urb in <linuxKernel>/include/linux/usbdevice_fs.h
     * At first I implemented the URB structure directly using com.sun.jna.Structure, but that was extremely slow.
     * Therefore byte offsets are now used to access the fields of the structure.
     */
    public static class Urb {

        /**
         * Base size of native URB (without iso_frame_desc) in bytes
         */
        public static final int urbBaseSize = 44;

        /**
         * Size of struct usbdevfs_iso_packet_desc
         */
        private static final int packetDescSize = 12;

        private ByteBuffer urbBuf;
        private int urbBufAddr;
        private int maxPackets;

        public Urb(int maxPackets) {
            this.maxPackets = maxPackets;
            int urbSize = urbBaseSize + maxPackets * packetDescSize;
            urbBuf = ByteBuffer.allocateDirect(urbSize);
            urbBuf.order(ByteOrder.nativeOrder());
            urbBufAddr = (int) Pointer.nativeValue(Native.getDirectBufferPointer(urbBuf));
        }

        public static int getUserContext(Pointer urbBufPointer) {
            return urbBufPointer.getInt(40);
        }

        public int getNativeUrbAddr() {
            return urbBufAddr;
        }

        public void setType(int type) {
            urbBuf.put(0, (byte) type);
        }

        public void setEndpoint(int endpoint) {
            urbBuf.put(1, (byte) endpoint);
        }

        public int getStatus() {
            return urbBuf.getInt(4);
        }

        public void setStatus(int status) {
            urbBuf.putInt(4, status);
        }

        public void setFlags(int flags) {
            urbBuf.putInt(8, flags);
        }

        public void setBuffer(Pointer buffer) {
            urbBuf.putInt(12, (int) Pointer.nativeValue(buffer));
        }

        public void setBufferLength(int bufferLength) {
            urbBuf.putInt(16, bufferLength);
        }

        public void setActualLength(int actualLength) {
            urbBuf.putInt(20, actualLength);
        }

        public void setStartFrame(int startFrame) {
            urbBuf.putInt(24, startFrame);
        }

        public int getNumberOfPackets() {
            return urbBuf.getInt(28);
        }

        public void setNumberOfPackets(int numberOfPackets) {
            if (numberOfPackets < 0 || numberOfPackets > maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(28, numberOfPackets);
        }

        public void setErrorCount(int errorCount) {
            urbBuf.putInt(32, errorCount);
        }

        /**
         * signal to be sent on completion, or 0 if none should be sent
         *
         * @param signr sigNr
         */
        public void setSigNr(int signr) {
            urbBuf.putInt(36, signr);
        }

        public int getUserContext() {
            return urbBuf.getInt(40);
        }

        public void setUserContext(int userContext) {
            urbBuf.putInt(40, userContext);
        }

        public void setPacketLength(int packetNo, int length) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize, length);
        }

        public int getPacketLength(int packetNo) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize);
        }

        public void setPacketActualLength(int packetNo, int actualLength) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize + 4, actualLength);
        }

        public int getPacketActualLength(int packetNo) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize + 4);
        }

        public void setPacketStatus(int packetNo, int status) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize + 8, status);
        }

        public int getPacketStatus(int packetNo) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize + 8);
        }
    }

    /**
     * Modeled after struct usbdevfs_setinterface in <linuxKernel>/include/uapi/linux/udbdevice_fs.h.
     */
    private static class Usbdevfs_setinterface extends Structure {
        public int interfaceId;
        public int altsetting;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(
                    "interfaceId",
                    "altsetting");
        }
    }

    /**
     * This class represents an isochronous data transfer request that can be queued with the USB device driver.
     * One request object contains multiple data packets.
     * Multiple request objects may be queued at a time.
     * <p>
     * The sequence of actions on a <code>Request</code> object is typically:
     * <pre>
     *   UsbIso.getRequest()
     *   Request.initialize()
     *   ... For output: Set packet data to be sent to device ...
     *   Request.submit()
     *   repeatedly:
     *      UsbIso.reapRequest()
     *      ... For input: Check status and process received packet data ...
     *      ... For output: Check status ...
     *      Request.initialize()
     *      ... For output: Set packet data to be sent to device ...
     *      Request.submit()</pre>
     */
    public class Request {
        private boolean initialized;
        private boolean queued;
        private Urb urb;
        private int urbAddr;
        private Memory buffer;
        private int endpointAddr;

        private Request() {
            urb = new Urb(maxPacketsPerRequest);
            urbAddr = urb.getNativeUrbAddr();
            int bufSize = maxPacketsPerRequest * maxPacketSize;
            buffer = new Memory(bufSize);
            urb.setUserContext(requests.size());
            requests.add(this);
        }

        /**
         * Initializes this <code>Request</code> object for the next {@link #submit}.
         * For input, an initialized <code>Request</code> object can usually be submitted without change.
         * For output, data has to be copied into the packet data buffers before the <code>Request</code> object is submitted.
         *
         * @param endpointAddr The address of an isochronous USB endpoint.
         *                     For Android, this is the value returned by <code>UsbEndpoint.getAddress()</code>.
         */
        public void initialize(int endpointAddr) {
            if (queued) {
                throw new IllegalStateException();
            }
            this.endpointAddr = endpointAddr;
            urb.setEndpoint(endpointAddr);
            urb.setType(USBDEVFS_URB_TYPE_ISO);
            urb.setFlags(USBDEVFS_URB_ISO_ASAP);
            urb.setBuffer(buffer);
            urb.setBufferLength((int) buffer.size());
            urb.setActualLength(0);
            urb.setStartFrame(0);
            setPacketCount(maxPacketsPerRequest);
            urb.setErrorCount(0);
            urb.setSigNr(0);
            urb.setStatus(-1);
            for (int packetNo = 0; packetNo < maxPacketsPerRequest; packetNo++) {
                urb.setPacketLength(packetNo, maxPacketSize);
                urb.setPacketActualLength(packetNo, 0);
                urb.setPacketStatus(packetNo, -1);
            }
            initialized = true;
        }

        /**
         * Submits this request to the USB device driver.
         * The request is added to the queue of active requests.
         */
        public void submit() throws IOException {
            if (!initialized || queued) {
                throw new IllegalStateException();
            }
            initialized = false;
            int rc = libc.ioctl(fileDescriptor, USBDEVFS_SUBMITURB, urbAddr);
            if (rc != 0) {
                throw new IOException("ioctl(USBDEVFS_SUBMITURB) failed, rc=" + rc + ".");
            }
            queued = true;
        }

        /**
         * Cancels a queued request.
         * The request remains within the queue amd must be removed from the queue by calling {@link UsbIso#reapRequest}.
         */
        public void cancel() throws IOException {
            int rc;
            try {
                rc = libc.ioctl(fileDescriptor, USBDEVFS_DISCARDURB, urbAddr);
            } catch (LastErrorException e) {
                if (e.getErrorCode() == EINVAL) {                 // This happens if the request has already completed.
                    return;
                }
                throw e;
            }
            if (rc != 0) {
                throw new IOException("ioctl(USBDEVFS_DISCARDURB) failed, rc=" + rc);
            }
        }

        /**
         * Returns the endpoint address associated with this request.
         */
        public int getEndpointAddr() {
            return endpointAddr;
        }

        /**
         * Returns the packet count of this request.
         */
        public int getPacketCount() {
            return urb.getNumberOfPackets();
        }

        /**
         * May be used to modify the packet count.
         * The default packet count is <code>maxPacketsPerRequest</code> (see <code>UsbIso</code> constructor).
         */
        public void setPacketCount(int n) {
            if (n < 1 || n > maxPacketsPerRequest) {
                throw new IllegalArgumentException();
            }
            urb.setNumberOfPackets(n);
        }

        /**
         * Returns the completion status code of a packet.
         * For normal completion the status is 0.
         */
        public int getPacketStatus(int packetNo) {
            return urb.getPacketStatus(packetNo);
        }

        /**
         * May be used to modify the length of data to request for the packet.
         * The default packet length is <code>maxPacketSize</code> (see <code>UsbIso</code> constructor).
         */
        public void setPacketLength(int packetNo, int length) {
            if (length < 0 || length > maxPacketSize) {
                throw new IllegalArgumentException();
            }
            urb.setPacketLength(packetNo, length);
        }

        /**
         * Returns the amount of data that was actually transferred for the packet.
         * When reading, this is the number of data bytes received from the device.
         */
        public int getPacketActualLength(int packetNo) {
            return urb.getPacketActualLength(packetNo);
        }

        /**
         * Used to provide data to be sent to the device.
         */
        public void setPacketData(int packetNo, byte[] buf, int len) {
            if (packetNo < 0 || packetNo >= maxPacketsPerRequest || len > maxPacketSize) {
                throw new IllegalArgumentException();
            }
            buffer.write(packetNo * maxPacketSize, buf, 0, len);
        }

        /**
         * Used to retrieve data that has been received from the device.
         */
        public void getPacketData(int packetNo, byte[] buf, int len) {
            if (packetNo < 0 || packetNo >= maxPacketsPerRequest || len > maxPacketSize) {
                throw new IllegalArgumentException();
            }
            buffer.read(packetNo * maxPacketSize, buf, 0, len);
        }
    }

}