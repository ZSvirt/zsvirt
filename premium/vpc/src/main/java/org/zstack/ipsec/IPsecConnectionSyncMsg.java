package org.zstack.ipsec;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;

/**
 * Created by shixin on 2017-12-02.
 */
public class IPsecConnectionSyncMsg extends NeedReplyMessage implements IPsecConnectionMessage {
    private IPsecConnectionInventory inv;
    private VirtualRouterVmInventory vr;
    private String l3NetworkUuid;
    private boolean skip_vip_release;

    public IPsecConnectionInventory getInv() {
        return inv;
    }

    public void setInv(IPsecConnectionInventory inv) {
        this.inv = inv;
    }

    public VirtualRouterVmInventory getVr() {
        return vr;
    }

    public void setVr(VirtualRouterVmInventory vr) {
        this.vr = vr;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public boolean isSkip_vip_release() {
        return skip_vip_release;
    }

    public void setSkip_vip_release(boolean skip_vip_release) {
        this.skip_vip_release = skip_vip_release;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return inv.getUuid();
    }
}
