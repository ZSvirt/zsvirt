package org.zstack.header.vmscheduling;

import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2022/11/30
 */
public class VmSchedulingRuleReserveMsg extends NeedReplyMessage implements VmSchedulingRuleGroupMessage {
    private HostInventory host;
    private HostAllocatorSpec spec;
    private String vmGroupUuid;

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

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    @Override
    public String getVmSchedulingRuleGroupUuid() {
        return vmGroupUuid;
    }
}
