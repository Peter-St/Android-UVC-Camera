// Changed by Peter Stoiber, Austria, on 3.8.2019
// https://github.com/Peter-St/
// Added Support for 64 bit devices.
//
//
// Original Version from:
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
//

package humer.UvcCamera.UsbIso64;

import android.util.Log;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.ptr.PointerByReference;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_DISCARDURB;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_REAPURB;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_REAPURBNDELAY;
import static humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_SUBMITURB;

import humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_URB_FLAG;
import humer.UvcCamera.UsbIso64.usbdevice_fs.USBDEVFS_URB_TYPE;
import humer.UvcCamera.UsbIso64.usbdevice_fs.Urb;

import java.io.IOException;
import java.util.ArrayList;

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
public class USBIso {
    //--- Native data structures ---------------------------------------------------
    private static final int EAGAIN = 11;
    private static final int ENODEV = 19;

    //--- Request object -----------------------------------------------------------
    private static final int EINVAL = 22;

    // Request types (bmRequestType):
    public static final int RT_STANDARD_INTERFACE_SET = 0x01;
    public static final int RT_CLASS_INTERFACE_SET = 0x21;
    public static final int RT_CLASS_INTERFACE_GET = 0xA1;
    // Video interface subclass codes:
    public static final int SC_VIDEOCONTROL = 0x01;
    public static final int SC_VIDEOSTREAMING = 0x02;
    public static final int CLASS_VIDEO  = 0x14;
    // Standard request codes:
    public static final int SET_INTERFACE = 0x0b;
    // Video class-specific request codes:
    public static final int SET_CUR = 0x01;
    public static final int GET_CUR = 0x81;
    // VideoControl interface control selectors (CS):
    public static final int VC_REQUEST_ERROR_CODE_CONTROL = 0x02;
    // VideoStreaming interface control selectors (CS):
    public static final int VS_PROBE_CONTROL = 0x01;
    public static final int VS_COMMIT_CONTROL = 0x02;
    public static final int VS_STILL_PROBE_CONTROL = 0x03;
    public static final int VS_STILL_COMMIT_CONTROL = 0x04;
    public static final int VS_STREAM_ERROR_CODE_CONTROL = 0x06;
    public static final int VS_STILL_IMAGE_TRIGGER_CONTROL = 0x05;

    public static final int UVC_STREAM_EOH = (1 << 7);
    public static final int UVC_STREAM_ERR = (1 << 6);
    public static final int UVC_STREAM_STI = (1 << 5);
    public static final int UVC_STREAM_RES = (1 << 4);
    public static final int UVC_STREAM_SCR = (1 << 3);
    public static final int UVC_STREAM_PTS = (1 << 2);
    public static final int UVC_STREAM_EOF = (1 << 1);
    public static final int UVC_STREAM_FID = (1 << 0);

    private final ArrayList<Request> requests = new ArrayList<>();
    private final int fileDescriptor;
    private final int maxPacketsPerRequest;
    private final int maxPacketSize;
    private final byte endpointAddress;

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
    public USBIso(int fileDescriptor, int maxPacketsPerRequest, int maxPacketSize, byte endpointAddress) {
        this.fileDescriptor = fileDescriptor;
        this.maxPacketsPerRequest = maxPacketsPerRequest;
        this.maxPacketSize = maxPacketSize;
        this.endpointAddress = endpointAddress;
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
            allocateRequest();
        }
    }

    public void submitUrbs() throws IOException {
        for (Request req: requests) {
            req.initialize();
            req.submit();
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
        return allocateRequest();
    }

    private Request allocateRequest() {
        Request request = new Request();
        request.setUserContext(requests.size());
        requests.add(request);
        return request;
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
      
        rc = Libc.INSTANCE.ioctl(fileDescriptor, func, urbPointer);
          
        if (rc != 0) {
            throw new IOException("ioctl(USBDEVFS_REAPURB*) failed, rc=" + rc + ".");
        }
        
        Urb urb = new Urb(urbPointer.getValue());
        int urbNdx = urb.getUserContext();
        if (urbNdx < 0 || urbNdx >= requests.size()) {
            throw new IOException("URB.userContext returned by ioctl(USBDEVFS_REAPURB*) is out of range.");
        }
        Request req = requests.get(urbNdx);
        if (! req.getNativeUrbAddr().equals(urbPointer.getValue())) {
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
    public class Request extends Urb {
        private boolean initialized;
        private boolean queued;
        private Memory buffer;

        private Request() {
            super(maxPacketsPerRequest);
            int bufSize = maxPacketsPerRequest * maxPacketSize;
            buffer = new Memory(bufSize);
        }

        /**
         * Initializes this <code>Request</code> object for the next {@link #submit}.
         * For input, an initialized <code>Request</code> object can usually be submitted without change.
         * For output, data has to be copied into the packet data buffers before the <code>Request</code> object is submitted.
         */
        public void initialize() {
            if (queued) {
                throw new IllegalStateException();
            }
            setEndpoint(endpointAddress);
            setType(USBDEVFS_URB_TYPE.ISO.getValue());
            setFlags(USBDEVFS_URB_FLAG.ISO_ASAP.getValue());
            setBuffer(buffer);
            setBufferLength((int) buffer.size());
            setActualLength(0);
            setStartFrame(0);
            setNumberOfPackets(maxPacketsPerRequest);
            setErrorCount(0);
            setSigNr(0);
            setStatus(-1);
            for (int packetNo = 0; packetNo < maxPacketsPerRequest; packetNo++) {
                setPacketLength(packetNo, maxPacketSize);
                setPacketActualLength(packetNo, 0);
                setPacketStatus(packetNo, -1);
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
            //System.out.println("urbBufAddr in long = " + a);
            // System.out.println("vor IOCTL Submit URBAdresse = " + urb.getNativeUrbAddr());
           // urbAddr = urb.getNativeUrbAddr();
            //System.out.println("nach native get URBAdresse = " +urbAddr);
            int rc;
            try {
                rc = (Libc.INSTANCE).ioctl(fileDescriptor, USBDEVFS_SUBMITURB, getNativeUrbAddr());
                if (rc != 0) {
                    throw new IOException("ioctl(USBDEVFS_SUBMITURB) failed, rc=" + rc + ".");
                }
            } catch (Exception e) {
                Log.d("ERROR", "ERROR: " + e);
            }


            //int rc = nativeIOCTLsenden(fileDescriptor, USBDEVFS_SUBMITURB);
            //System.out.println("nach URBAdresse = " +urbAddr);
           // System.out.println("URBAdresse");

            queued = true;
        }

        /**
         * Cancels a queued request.
         * The request remains within the queue amd must be removed from the queue by calling {@link UsbIso#reapRequest}.
         */
        public void cancel() throws IOException {
            int rc;
            try {
                rc = (Libc.INSTANCE).ioctl(fileDescriptor, USBDEVFS_DISCARDURB, getNativeUrbAddr());
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
         * May be used to modify the packet count.
         * The default packet count is <code>maxPacketsPerRequest</code> (see <code>UsbIso</code> constructor).
         */
        @Override
        public void setNumberOfPackets(int n) {
            if (n < 1 || n > maxPacketsPerRequest) {
                throw new IllegalArgumentException();
            }
            super.setNumberOfPackets(n);
        }

        /**
         * May be used to modify the length of data to request for the packet.
         * The default packet length is <code>maxPacketSize</code> (see <code>UsbIso</code> constructor).
         */
        @Override
        public void setPacketLength(int packetNo, int length) {
            if (length < 0 || length > maxPacketSize) {
                throw new IllegalArgumentException();
            }
            super.setPacketLength(packetNo, length);
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
