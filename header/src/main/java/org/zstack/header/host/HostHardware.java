package org.zstack.header.host;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/27 17:44
 */
public enum HostHardware {
    CPU,
    MEMORY,
    DISK,
    GPU,
    POWERSUPPLY,
    FAN,
    RAID,
    PHYSICAL_VOLUME,
    UNKNOWN;

    public static HostHardware fromString(String name) {
        try {
            return HostHardware.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return UNKNOWN;
        }
    }
}
