package org.zstack.header.image;

/**
 */
public enum ImagePlatform {
    Linux(true),
    Windows(false),
    WindowsVirtio(true),
    Other(false),
    Paravirtualization(true);

    public static boolean isType(String actual, ImagePlatform... expected) {
        for (ImagePlatform imagePlatform : expected) {
            if (imagePlatform.toString().equals(actual)) {
                return true;
            }
        }

        return false;
    }

    private boolean paraVirtualization;

    private ImagePlatform(boolean para) {
        paraVirtualization = para;
    }

    public boolean isParaVirtualization() {
        return paraVirtualization;
    }
}
