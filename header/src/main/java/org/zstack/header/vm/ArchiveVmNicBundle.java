package org.zstack.header.vm;

/**
 * Created by LiangHanYu on 2022/7/1 13:33
 */
public class ArchiveVmNicBundle extends ArchiveBundle {
    VmNicInventory vmNicInventory;
    long outboundBandwidth;
    long inboundBandwidth;
    boolean isVmDefaultL3Network = false;

    public ArchiveVmNicBundle() {
    }

    public ArchiveVmNicBundle(VmNicInventory vmNicInventory) {
        this.vmNicInventory = vmNicInventory;
    }

    public ArchiveVmNicBundle(VmNicInventory vmNicInventory, boolean isVmDefaultL3Network) {
        this.vmNicInventory = vmNicInventory;
        this.isVmDefaultL3Network = isVmDefaultL3Network;
    }

    public ArchiveVmNicBundle(VmNicInventory vmNicInventory, long outboundBandwidth, long inboundBandwidth) {
        this.vmNicInventory = vmNicInventory;
        this.outboundBandwidth = outboundBandwidth;
        this.inboundBandwidth = inboundBandwidth;
    }

    public VmNicInventory getVmNicInventory() {
        return vmNicInventory;
    }

    public void setVmNicInventory(VmNicInventory vmNicInventory) {
        this.vmNicInventory = vmNicInventory;
    }

    public long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public boolean isVmDefaultL3Network() {
        return isVmDefaultL3Network;
    }

    public void setVmDefaultL3Network(boolean vmDefaultL3Network) {
        isVmDefaultL3Network = vmDefaultL3Network;
    }
}
