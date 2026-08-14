package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiwang on 21/04/2017.
 */
public class PopulateVtepPeersMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String poolUuid;
    private List<HostInventory> hosts = new ArrayList<>();

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
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
