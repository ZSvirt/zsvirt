package org.zstack.ovf.datatype;

import org.zstack.header.image.ImageBootMode;

public enum OvfFirmware {
    BIOS(ImageBootMode.Legacy),
    EFI(ImageBootMode.UEFI);

    final ImageBootMode mode;
    OvfFirmware(ImageBootMode mode) {
        this.mode = mode;
    }

    public ImageBootMode toImageBootMode() {
        return mode;
    }
}
