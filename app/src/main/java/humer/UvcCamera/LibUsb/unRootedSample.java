package humer.UvcCamera.LibUsb;

import com.sun.jna.Library;
import com.sun.jna.Native;

public interface unRootedSample extends Library {

    public static final unRootedSample INSTANCE = Native.load("unRootedAndroid", unRootedSample.class);

    public int main (int fileDescriptor);

}
