package org.zstack.pciDevice.header;

public enum HostIommuStatusType {
    Active,
    Inactive;

    public static HostIommuStatusType valueOf(Boolean status) {
        if (status) {
            return Active;
        }
        return Inactive;
    }
}
