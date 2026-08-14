package org.zstack.header.image;

public enum ImageArchitecture {
    x86_64,
    aarch64,
    mips64el,
    loongarch64;

    private static String defaultArch = System.getProperty("os.arch").equals("amd64") ?
            "x86_64" : System.getProperty("os.arch");

    public static String defaultArch() {
        return defaultArch;
    }
}
