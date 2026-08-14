package org.zstack.header.volume;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2021/6/4.
 */
public class SetVmBootVolumeMsg extends NeedReplyMessage implements VolumeMessage {
    private String vmInstanceUuid;
    private String volumeUuid;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
