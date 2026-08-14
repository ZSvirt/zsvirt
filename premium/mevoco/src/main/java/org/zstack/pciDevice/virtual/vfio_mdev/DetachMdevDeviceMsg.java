package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2019-05-25.
 */
public class DetachMdevDeviceMsg extends NeedReplyMessage implements MdevDeviceMessage {
    private String mdevDeviceUuid;
    private String vmInstanceUuid;

    @Override
    public String getMdevDeviceUuid() {
        return mdevDeviceUuid;
    }

    public void setMdevDeviceUuid(String mdevDeviceUuid) {
        this.mdevDeviceUuid = mdevDeviceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
