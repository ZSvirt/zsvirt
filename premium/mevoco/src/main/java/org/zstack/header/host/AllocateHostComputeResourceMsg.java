package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class AllocateHostComputeResourceMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String strategy;
    private String scene;
    private int vcpu;
    private Long memSize;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getUuid() {
        return hostUuid;
    }

    public Long getMemSize() {
        return memSize;
    }

    public void setMemSize(Long memSize) {
        this.memSize = memSize;
    }

    public int getVcpu() {
        return vcpu;
    }

    public void setVcpu(int vcpu) {
        this.vcpu = vcpu;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
}
