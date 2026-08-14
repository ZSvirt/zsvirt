package org.zstack.utils.string;

import java.util.UUID;

public class Uuid {
    public static String generate() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String generate(byte[] name) {
        return UUID.nameUUIDFromBytes(name).toString().replace("-", "");
    }
}
