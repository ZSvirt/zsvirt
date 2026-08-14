package org.zstack.header.affinitygroup;

import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 2017-11-16.
 */
public class AffinityGroupRollbackMsg extends NeedReplyMessage implements AffinityGroupMessage {
    private HostInventory       host;
    private HostAllocatorSpec   spec;
    private String              affinityGroupUuid;
    private String              originHostUuid;

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

    public String getOriginHostUuid() {
        return originHostUuid;
    }

    public void setOriginHostUuid(String originHostUuid) {
        this.originHostUuid = originHostUuid;
    }

    @Override
    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }
}
