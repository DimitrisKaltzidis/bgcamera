package com.admobilize.lib;

/**
 * Created by Antonio Vanegas @hpsaturn on 11/19/17.
 */

public class NativeProcess {


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("frameprocess");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI(byte[] frame);


}
