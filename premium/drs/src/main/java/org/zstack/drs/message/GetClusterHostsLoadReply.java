package org.zstack.drs.message;

import org.zstack.drs.data.HostLoad;
import org.zstack.drs.data.HostNode;
import org.zstack.header.message.MessageReply;

import java.util.List;

/**
 * Created by lining on 2019/12/13.
 */
public class GetClusterHostsLoadReply extends MessageReply {
    private List<HostLoad> hostLoads;

    public List<HostLoad> getHostLoads() {
        return hostLoads;
    }

    public void setHostLoads(List<HostLoad> hostLoads) {
        this.hostLoads = hostLoads;
    }
}
