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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import static humer.UvcCamera.UsbIso64.Ioctl._IO;
import static humer.UvcCamera.UsbIso64.Ioctl._IOR;
import static humer.UvcCamera.UsbIso64.Ioctl._IOW;
import static humer.UvcCamera.UsbIso64.Ioctl._IOWR;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface usbdevice_fs {
    public enum USBDEVFS_URB_TYPE {
        ISO((byte) 0),
        INTERRUPT((byte) 1),
        CONTROL((byte) 2),
        BULK((byte) 3);

        private final byte value;

        private USBDEVFS_URB_TYPE(byte value) {
            this.value = value;
        }

        public byte getValue() {
            return value;
        }

        public static USBDEVFS_URB_TYPE fromNative(byte value) {
            for(USBDEVFS_URB_TYPE val: USBDEVFS_URB_TYPE.values()) {
                if(val.getValue() == value) {
                    return val;
                }
            }
            return null;
        }
    }

    public enum USBDEVFS_URB_FLAG {
        SHORT_NOT_OK(0x01),
        ISO_ASAP(0x02),
        BULK_CONTINUATION(0x04),
        /**
         * Not used
         */
        NO_FSBR(0x20),
        ZERO_PACKET(0x40),
        NO_INTERRUPT(0x80),;

        private final int value;

        USBDEVFS_URB_FLAG(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static int toNative(USBDEVFS_URB_FLAG... flags) {
            int nativeValue = 0;
            for(USBDEVFS_URB_FLAG flag: flags) {
                nativeValue |= flag.value;
            }
            return nativeValue;
        }

        public static Set<USBDEVFS_URB_FLAG> fromNative(int nativeValue) {
            Set<USBDEVFS_URB_FLAG> result = new HashSet<>();
            for(USBDEVFS_URB_FLAG flag: USBDEVFS_URB_FLAG.values()) {
                if((nativeValue & flag.getValue()) == flag.getValue()) {
                    result.add(flag);
                }
            }
            return result;
        }
    }

    public enum USBDEVFS_CAP {
        ZERO_PACKET(0x01),
        BULK_CONTINUATION(0x02),
        NO_PACKET_SIZE_LIM(0x04),
        BULK_SCATTER_GATHER(0x08),
        REAP_AFTER_DISCONNECT(0x10),
        MMAP(0x20),
        DROP_PRIVILEGES(0x40),
        ;

        private final int value;

        USBDEVFS_CAP(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static int toNative(USBDEVFS_CAP... caps) {
            int nativeValue = 0;
            for(USBDEVFS_CAP cap: caps) {
                nativeValue |= cap.value;
            }
            return nativeValue;
        }

        public static Set<USBDEVFS_CAP> fromNative(int nativeValue) {
            Set<USBDEVFS_CAP> result = new HashSet<>();
            for(USBDEVFS_CAP cap: USBDEVFS_CAP.values()) {
                if((nativeValue & cap.getValue()) == cap.getValue()) {
                    result.add(cap);
                }
            }
            return result;
        }
    }

    public static final int USBDEVFS_MAXDRIVERNAME = 255;

    // IOCTL function codes:
    public static final int USBDEVFS_CONTROL = _IOWR('U', 0, new usbdevfs_ctrltransfer().size());
    public static final int USBDEVFS_BULK = _IOWR('U', 2, new usbdevfs_bulktransfer().size());
    public static final int USBDEVFS_RESETEP = _IOR('U', 3, 4);
    public static final int USBDEVFS_SETINTERFACE = _IOR('U', 4, new usbdevfs_setinterface().size());
    public static final int USBDEVFS_SETCONFIGURATION = _IOR('U', 5, 4);
    public static final int USBDEVFS_GETDRIVER = _IOW('U', 8, new usbdevfs_getdriver().size());
    public static final int USBDEVFS_SUBMITURB = _IOR('U', 10, new usbdevfs_urb().size());
    public static final int USBDEVFS_DISCARDURB = _IO('U', 11);
    public static final int USBDEVFS_REAPURB = _IOW('U', 12, Native.POINTER_SIZE);
    public static final int USBDEVFS_REAPURBNDELAY = _IOW('U', 13, Native.POINTER_SIZE);
    public static final int USBDEVFS_DISCSIGNAL = _IOR('U', 14, new usbdevfs_disconnectsignal().size());
    public static final int USBDEVFS_CLAIMINTERFACE = _IOR('U', 15, 4);
    public static final int USBDEVFS_RELEASEINTERFACE = _IOR('U', 16, 4);
    public static final int USBDEVFS_CONNECTINFO = _IOR('U', 17, new usbdevfs_connectinfo().size());
    public static final int USBDEVFS_IOCTL = _IOWR('U', 18, new usbdevfs_ioctl().size());
    public static final int USBDEVFS_HUB_PORTINFO = _IOWR('U', 18, new usbdevfs_hub_portinfo().size());
    public static final int USBDEVFS_RESET = _IO('U', 20);
    public static final int USBDEVFS_CLEAR_HALT = _IOR('U', 21, 4);
    public static final int USBDEVFS_DISCONNECT = _IO('U', 22);
    public static final int USBDEVFS_CONNECT = _IO('U', 23);
    public static final int USBDEVFS_CLAIM_PORT = _IOR('U', 24, 4);
    public static final int USBDEVFS_RELEASE_PORT = _IOR('U', 25, 4);
    public static final int USBDEVFS_GET_CAPABILITIES = _IOR('U', 26, 4);
    public static final int USBDEVFS_DISCONNECT_CLAIM = _IOR('U', 27, new usbdevfs_disconnect_claim().size());
    public static final int USBDEVFS_ALLOC_STREAMS = _IOR('U', 28, new usbdevfs_streams().size());
    public static final int USBDEVFS_FREE_STREAMS = _IOR('U', 29, new usbdevfs_streams().size());
    public static final int USBDEVFS_DROP_PRIVILEGES = _IOW('U', 30, 4);
    public static final int USBDEVFS_GET_SPEED = _IO('U', 31);

    /**
     * disconnect-and-claim if the driver matches the driver field
     */
    public static final int USBDEVFS_DISCONNECT_CLAIM_IF_DRIVER = 0x01;
    /**
     * disconnect-and-claim except when the driver matches the driver field
     */
    public static final int USBDEVFS_DISCONNECT_CLAIM_EXCEPT_DRIVER = 0x02;

    @FieldOrder({"bRequestType", "bRequest", "wValue", "wIndex", "wLength", "timeout", "data"})
    public static class usbdevfs_ctrltransfer extends Structure {
        public byte bRequestType;
        public byte bRequest;
        public short wValue;
        public short wIndex;
        public short wLength;
        public int timeout; /* in ms */
        public Pointer data;
    }

    @FieldOrder({"ep", "len", "timeout", "data"})
    public static class usbdevfs_bulktransfer extends Structure {
        public int ep;
        public int len;
        public int timeout; /* in ms */
        public Pointer data;
    }

    /**
     * Modeled after struct usbdevfs_setinterface in <linuxKernel>/include/uapi/linux/usbdevice_fs.h.
     */
    @FieldOrder({"interfaceId", "altsetting"})
    public static class usbdevfs_setinterface extends Structure {
        public int interfaceId;
        public int altsetting;
    }

    @FieldOrder({"signr", "context"})
    public static class usbdevfs_disconnectsignal extends Structure {
        public int signr;
        public Pointer context;
    }

    @FieldOrder({"ifno", "driver"})
    public static class usbdevfs_getdriver extends Structure {
        public int ifno;
        public byte[] driver = new byte[USBDEVFS_MAXDRIVERNAME + 1];
    }

    @FieldOrder({"devnum", "slow"})
    public static class usbdevfs_connectinfo extends Structure {
        public int devnum;
        public byte slow;
    }

    /**
     * At the end of usbdevfs_urb follows an array of usbdevfs_iso_packet_desc
     * these are not modelled in this case, as JNA gets the offsets wrong in
     * this case
     */
    @FieldOrder({"type", "endpoint", "status", "flags", "buffer",
        "buffer_length", "actual_length", "start_frame",
        "number_of_packets_stream_id", "error_count", "signr",
        "usercontext"})
    public static final class usbdevfs_urb extends Structure {

        public byte type;
        public byte endpoint;
        public int status;
        public int flags;
        public Pointer buffer;
        public int buffer_length;
        public int actual_length;
        public int start_frame;
        public int number_of_packets_stream_id; // this is a union
        public int error_count;
        public int signr;
        public Pointer usercontext;

        @Override
        protected int fieldOffset(String field) {
            return super.fieldOffset(field);
        }
    }
    
    @FieldOrder({"length", "actual_length", "status"})
    public static final class usbdevfs_iso_packet_desc extends Structure {

        public int length;
        public int actual_length;
        public int status;

        @Override
        public int fieldOffset(String field) {
            return super.fieldOffset(field);
        }
    }

    @FieldOrder({"ifno", "ioctl_code", "data"})
    public static class usbdevfs_ioctl extends Structure {
        public int ifno;
        public int ioctl_code;
        public Pointer data;
    }

    @FieldOrder({"nports", "port"})
    public static class usbdevfs_hub_portinfo extends Structure {
        public byte nports;
        public byte[] port = new byte[127];
    }

    @FieldOrder({"iface", "flags", "driver"})
    public static class usbdevfs_disconnect_claim extends Structure {
        public int iface;
        public int flags;
        public byte[] driver = new byte[USBDEVFS_MAXDRIVERNAME + 1];

        public String getDriver() {
            return Native.toString(driver);
        }

        public void setDriver(String driverString) {
            byte[] data = Native.toByteArray(driverString);
            Arrays.fill(driver, (byte) 0);
            System.arraycopy(data, 0, driver, 0, Math.min(data.length, USBDEVFS_MAXDRIVERNAME));
        }
    }

    @FieldOrder({"num_streams", "num_eps", "eps"})
    public static class usbdevfs_streams extends Structure {
        public int num_streams;
        public int num_eps;
        public byte[] eps = new byte[1];

        @Override
        public void write() {
            num_eps = eps.length;
            super.write();
        }

        @Override
        public void read() {
            readField("num_eps");
            if(eps.length != num_eps) {
                eps = new byte[num_eps];
            }
            super.read();
        }
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
        public static final int urbBaseSize;

        /**
         * Size of struct usbdevfs_iso_packet_desc
         */
        private static final int packetDescSize;

        public static final int usbdevfs_urb_type;
        public static final int usbdevfs_urb_endpoint;
        public static final int usbdevfs_urb_status;
        public static final int usbdevfs_urb_flags;
        public static final int usbdevfs_urb_buffer;
        public static final int usbdevfs_urb_buffer_length;
        public static final int usbdevfs_urb_actual_length;
        public static final int usbdevfs_urb_start_frame;
        public static final int usbdevfs_urb_number_of_packets_stream_id;
        public static final int usbdevfs_urb_error_count;
        public static final int usbdevfs_urb_signr;
        public static final int usbdevfs_urb_usercontext;
        public static final int usbdevfs_iso_packet_desc_length;
        public static final int usbdevfs_iso_packet_desc_actual_length;
        public static final int usbdevfs_iso_packet_desc_status;

        static {
            usbdevfs_urb urb = new usbdevfs_urb();
            usbdevfs_iso_packet_desc desc = new usbdevfs_iso_packet_desc();
            urbBaseSize = urb.size();
            packetDescSize = desc.size();
            usbdevfs_urb_type = urb.fieldOffset("type");
            usbdevfs_urb_endpoint = urb.fieldOffset("endpoint");
            usbdevfs_urb_status = urb.fieldOffset("status");
            usbdevfs_urb_flags = urb.fieldOffset("flags");
            usbdevfs_urb_buffer = urb.fieldOffset("buffer");
            usbdevfs_urb_buffer_length = urb.fieldOffset("buffer_length");
            usbdevfs_urb_actual_length = urb.fieldOffset("actual_length");
            usbdevfs_urb_start_frame = urb.fieldOffset("start_frame");
            usbdevfs_urb_number_of_packets_stream_id = urb.fieldOffset("number_of_packets_stream_id");
            usbdevfs_urb_error_count = urb.fieldOffset("error_count");
            usbdevfs_urb_signr = urb.fieldOffset("signr");
            usbdevfs_urb_usercontext = urb.fieldOffset("usercontext");
            usbdevfs_iso_packet_desc_length = desc.fieldOffset("length");
            usbdevfs_iso_packet_desc_actual_length = desc.fieldOffset("actual_length");
            usbdevfs_iso_packet_desc_status = desc.fieldOffset("status");
        }

        private ByteBuffer urbBuf;
        private final Pointer urbBufPointer;
        private int maxPackets;

        public Urb(int maxPackets) {
            this.maxPackets = maxPackets;
            int urbSize = urbBaseSize + maxPackets * packetDescSize;
            urbBuf = ByteBuffer.allocateDirect(urbSize);
            urbBuf.order(ByteOrder.nativeOrder());
            urbBufPointer = Native.getDirectBufferPointer(urbBuf);
        }

        public Urb(Pointer urbPointer) {
            //this.maxPackets = this.getNumberOfPackets();
            int urbSize = urbBaseSize + maxPackets * packetDescSize;
            this.urbBufPointer = urbPointer;
            this.urbBuf = urbBufPointer.getByteBuffer(0, urbSize);
        }

        public Pointer getNativeUrbAddr() {
            return urbBufPointer;
        }

        public void setType(byte type) {
            urbBuf.put(usbdevfs_urb_type, type);
        }

        public void setEndpoint(byte endpoint) {
            urbBuf.put(usbdevfs_urb_endpoint,  endpoint);
        }

        public int getStatus() {
            return urbBuf.getInt(usbdevfs_urb_status);
        }

        public void setStatus(int status) {
            urbBuf.putInt(usbdevfs_urb_status, status);
        }

        public void setFlags(int flags) {
            urbBuf.putInt(usbdevfs_urb_flags, flags);
        }

        public void setBuffer(Pointer buffer) {
            if(Native.POINTER_SIZE == 4) {
                urbBuf.putInt(usbdevfs_urb_buffer, (int) Pointer.nativeValue(buffer));
            } else if (Native.POINTER_SIZE == 8) {
                urbBuf.putLong(usbdevfs_urb_buffer, Pointer.nativeValue(buffer));
            } else {
                throw new IllegalStateException("Unhandled Pointer Size: " + Native.POINTER_SIZE);
            }
        }

        public void setBufferLength(int bufferLength) {
            urbBuf.putInt(usbdevfs_urb_buffer_length, bufferLength);
        }

        public void setActualLength(int actualLength) {
            urbBuf.putInt(usbdevfs_urb_actual_length, actualLength);
        }

        public void setStartFrame(int startFrame) {
            urbBuf.putInt(usbdevfs_urb_start_frame, startFrame);
        }

        public int getNumberOfPackets() {
            return urbBuf.getInt(usbdevfs_urb_number_of_packets_stream_id);
        }

        public void setNumberOfPackets(int numberOfPackets) {
            if (numberOfPackets < 0 || numberOfPackets > maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(usbdevfs_urb_number_of_packets_stream_id, numberOfPackets);
        }

        public void setErrorCount(int errorCount) {
            urbBuf.putInt(usbdevfs_urb_error_count, errorCount);
        }

        /**
         * signal to be sent on completion, or 0 if none should be sent
         *
         * @param signr sigNr
         */
        public void setSigNr(int signr) {
            urbBuf.putInt(usbdevfs_urb_signr, signr);
        }

        public int getUserContext() {
            if(Native.POINTER_SIZE == 4) {
                return urbBuf.getInt(usbdevfs_urb_usercontext);
            } else if (Native.POINTER_SIZE == 8) {
                return (int) urbBuf.getLong(usbdevfs_urb_usercontext);
            } else {
                throw new IllegalStateException("Unhandled Pointer Size: " + Native.POINTER_SIZE);
            }
        }

        public void setUserContext(int userContext) {
            if(Native.POINTER_SIZE == 4) {
                urbBuf.putInt(usbdevfs_urb_usercontext, userContext);
            } else if (Native.POINTER_SIZE == 8) {
                urbBuf.putLong(usbdevfs_urb_usercontext, userContext);
            } else {
                throw new IllegalStateException("Unhandled Pointer Size: " + Native.POINTER_SIZE);
            }
        }

        public void setPacketLength(int packetNo, int length) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_length, length);    // für packetNo = 0 == urbBaseSize = 44 packetDescSize = 12 packetNo = 0
        }

        public int getPacketLength(int packetNo) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_length);
        }

        public void setPacketActualLength(int packetNo, int actualLength) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_actual_length, actualLength);
        }

        public int getPacketActualLength(int packetNo) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_actual_length);
        }

        public void setPacketStatus(int packetNo, int status) {
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            urbBuf.putInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_status, status);
        }

        public int getPacketStatus(int packetNo) {
           // System.out.println("sadcasdor = ");
            if (packetNo < 0 || packetNo >= maxPackets) {
                throw new IllegalArgumentException();
            }
            return urbBuf.getInt(urbBaseSize + packetNo * packetDescSize + usbdevfs_iso_packet_desc_status);
            //System.out.println("Endpunktadresse vo");
        }
    }
}
