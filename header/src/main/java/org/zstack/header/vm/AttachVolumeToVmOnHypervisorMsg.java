package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;

public class AttachVolumeToVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private VolumeInventory inventory;
    private List<VolumeInventory> attachedDataVolumes;
    private String hostUuid;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public VolumeInventory getInventory() {
        return inventory;
    }

    public void setInventory(VolumeInventory inventory) {
        this.inventory = inventory;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<VolumeInventory> getAttachedDataVolumes() {
        return attachedDataVolumes;
    }

    public void setAttachedDataVolumes(List<VolumeInventory> attachedDataVolumes) {
        this.attachedDataVolumes = attachedDataVolumes;
    }
}
