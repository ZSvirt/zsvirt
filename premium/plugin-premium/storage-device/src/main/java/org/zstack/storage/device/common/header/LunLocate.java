package org.zstack.storage.device.common.header;

public enum LunLocate {
    Local,
    Remote,
    Unknown, // for compatibility
    ;

    public static final String TRANSPORT_PCIE = "PCIE";
    public static final String TRANSPORT_TCP = "TCP";
    public static final String TRANSPORT_RDMA = "RDMA";

    public static LunLocate fromTransport(String transport) {
        if (transport == null || "".equals(transport)) {
            return null;
        }

        transport = transport.toUpperCase();
        switch (transport) {
        case TRANSPORT_PCIE:
            return Local;
        case TRANSPORT_TCP: case TRANSPORT_RDMA:
            return Remote;
        default:
            return null;
        }
    }

}
