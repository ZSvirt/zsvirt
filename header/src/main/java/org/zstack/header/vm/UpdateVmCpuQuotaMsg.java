package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class UpdateVmCpuQuotaMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private long vmCpuQuota;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public long getVmCpuQuota() {
        return vmCpuQuota;
    }

    public void setVmCpuQuota(long vmCpuQuota) {
        this.vmCpuQuota = vmCpuQuota;
    }
}
