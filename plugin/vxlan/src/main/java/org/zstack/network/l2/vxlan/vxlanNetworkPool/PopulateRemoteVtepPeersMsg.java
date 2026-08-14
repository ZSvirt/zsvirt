package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkMessage;

import java.util.ArrayList;
import java.util.List;

public class PopulateRemoteVtepPeersMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String poolUuid;
    private String vtepIp;
    private List<HostInventory> hosts = new ArrayList<>();

    public String getPoolUuid() {
        return poolUuid;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    @Override
    public String getL2NetworkUuid() {
        return getPoolUuid();
    }

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }
}
