package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.NeedReplyMessage;

public class CheckAndReserveMdevDeviceMsg extends NeedReplyMessage implements MdevDeviceMessage {
    private String vmUuid;
    private String mdevUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getMdevUuid() {
        return mdevUuid;
    }

    public void setMdevUuid(String mdevUuid) {
        this.mdevUuid = mdevUuid;
    }

    @Override
    public String getMdevDeviceUuid() {
        return mdevUuid;
    }
}
