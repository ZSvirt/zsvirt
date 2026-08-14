package org.zstack.header.affinitygroup;

import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 2017-11-16.
 */
public class AffinityGroupReserveMsg extends NeedReplyMessage implements AffinityGroupMessage {
    private HostInventory       host;
    private HostAllocatorSpec   spec;
    private String              affinityGroupUuid;

    public HostInventory getHost() {
        return host;
    }

    public void setHost(HostInventory host) {
        this.host = host;
    }

    public HostAllocatorSpec getSpec() {
        return spec;
    }

    public void setSpec(HostAllocatorSpec spec) {
        this.spec = spec;
    }

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }

    @Override
    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }
}
