package org.zstack.image;

import org.zstack.header.image.ImageBootMode;
import org.zstack.header.image.ImagePlatform;

public class ImageDefaultBehavior {
    static String defaultPlatform = ImagePlatform.Linux.toString();
    static String defaultBootMode = ImageBootMode.Legacy.toString();

    public static String getDefaultPlatform() {
        return defaultPlatform;
    }

    public static void setDefaultPlatform(String defaultPlatform) {
        ImageDefaultBehavior.defaultPlatform = defaultPlatform;
    }

    public static String getDefaultBootMode() {
        return defaultBootMode;
    }

    public static void setDefaultBootMode(String defaultBootMode) {
        ImageDefaultBehavior.defaultBootMode = defaultBootMode;
    }
}
