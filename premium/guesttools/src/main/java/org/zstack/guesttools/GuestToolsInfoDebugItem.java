package org.zstack.guesttools;

import javax.annotation.Nullable;

public enum GuestToolsInfoDebugItem {
    LIGHTTPD("lighttpd"),
    PUSH_GATEWAY("pushgateway"),
    ;

    public static final String ALL = "all";

    public final String item;
    GuestToolsInfoDebugItem(String item) {
        this.item = item;
    }

    @Nullable
    public static GuestToolsInfoDebugItem findByItemName(String item) {
        for (GuestToolsInfoDebugItem i : values()) {
            if (i.item.equals(item)) {
                return i;
            }
        }
        return null;
    }
}
