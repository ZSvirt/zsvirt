package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;

public class StartColoSyncMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private Integer blockReplicationPort;
    private Integer nbdServerPort;
    private String secondaryVmHostIp;
    private long checkpointDelay;
    private boolean fullSync;
    private List<VmNicInventory> nics;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Integer getBlockReplicationPort() {
        return blockReplicationPort;
    }

    public void setBlockReplicationPort(Integer blockReplicationPort) {
        this.blockReplicationPort = blockReplicationPort;
    }

    public Integer getNbdServerPort() {
        return nbdServerPort;
    }

    public void setNbdServerPort(Integer nbdServerPort) {
        this.nbdServerPort = nbdServerPort;
    }

    public String getSecondaryVmHostIp() {
        return secondaryVmHostIp;
    }

    public void setSecondaryVmHostIp(String secondaryVmHostIp) {
        this.secondaryVmHostIp = secondaryVmHostIp;
    }

    public long getCheckpointDelay() {
        return checkpointDelay;
    }

    public void setCheckpointDelay(long checkpointDelay) {
        this.checkpointDelay = checkpointDelay;
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public void setFullSync(boolean fullSync) {
        this.fullSync = fullSync;
    }

    public List<VmNicInventory> getNics() {
        return nics;
    }

    public void setNics(List<VmNicInventory> nics) {
        this.nics = nics;
    }
}
