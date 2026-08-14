package org.zstack.header.host;

import org.zstack.header.message.Message;

public interface Host {
    String idPrefix = "Host-";
    static String getUuidFromId(String id) {
        return id.startsWith(idPrefix) && id.length() == 32 + idPrefix.length() ? id.substring(idPrefix.length()) : null;
    }

    static String buildId(String uuid) {
        return idPrefix + uuid;
    }

    void handleMessage(Message msg);
    String getId();
}
