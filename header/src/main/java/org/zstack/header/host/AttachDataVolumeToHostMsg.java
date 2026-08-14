package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class AttachDataVolumeToHostMsg extends NeedReplyMessage implements HostMessage {
    private String volumeUuid;
    private String mountPath;
    private String hostUuid;
    private String device;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
