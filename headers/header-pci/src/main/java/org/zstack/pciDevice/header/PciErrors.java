package org.zstack.pciDevice.header;

public enum PciErrors {
    GENERAL_ERROR(1000),

    // 2XXX: PCI sync errors
    PCI_SYNC_ERROR(2000),

    ;

    public final String code;

    PciErrors(int id) {
        code = String.format("PCI.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
