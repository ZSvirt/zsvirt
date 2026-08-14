package org.zstack.header.host;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class DetachDataVolumeFromHostMsg extends NeedReplyMessage implements HostMessage {
    private String volumeInstallPath;
    private String mountPath;
    private String hostUuid;
    private String device;
    private String volumeUuid;


    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVolumeInstallPath() {
        return volumeInstallPath;
    }

    public void setVolumeInstallPath(String volumeInstallPath) {
        this.volumeInstallPath = volumeInstallPath;
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

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
