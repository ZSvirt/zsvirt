package org.zstack.externalStorage.primary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Xingwei Yu
 * @date 2024/8/21 10:38
 */
public class ExternalStorageFencerType {
    private static Map<String, String> types = Collections.synchronizedMap(new HashMap<String, String>());

    private final String identity;
    private final String protocol;

    public ExternalStorageFencerType(String identity, String protocol) {
        if (types.containsKey(identity)) {
            throw new IllegalArgumentException(String.format("duplicate ExternalStorageNodeServer for identity[%s]", identity));
        }

        this.identity = identity;
        this.protocol = protocol;
        types.put(identity, protocol);
    }

    public static String getProtocolFromIdentity(String identity) {
        String protocol = types.get(identity);
        if (protocol == null) {
            throw new IllegalArgumentException("ExternalStorageNodeServer identity: " + identity + " was not registered by any ExternalStorageFactory");
        }
        return protocol;
    }
}
