package com.modvault.app;

public class NativeSecrets {
    static {
        System.loadLibrary("secrets");
    }
    public static native String getCurseForgeKey();
}
