package org.zstack.header.identity;

@Deprecated
public enum ShareResourcePermission {
    READ(1),
    WRITE(1 << 1);

    public final int code;
    ShareResourcePermission(int code) {
        this.code = code;
    }
}
