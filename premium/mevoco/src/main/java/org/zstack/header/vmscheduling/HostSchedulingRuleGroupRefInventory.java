package org.zstack.header.vmscheduling;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


@Inventory(mappingVOClass = HostSchedulingRuleGroupRefVO.class)
public class HostSchedulingRuleGroupRefInventory {
    private String hostGroupUuid;
    private String hostUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostSchedulingRuleGroupRefInventory valueOf(HostSchedulingRuleGroupRefVO vo) {
        HostSchedulingRuleGroupRefInventory ref = new HostSchedulingRuleGroupRefInventory();
        ref.hostGroupUuid = vo.getHostGroupUuid();
        ref.hostUuid = vo.getHostUuid();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        return ref;
    }

    public static List<HostSchedulingRuleGroupRefInventory> valueOf(Collection<HostSchedulingRuleGroupRefVO> vos) {
        return vos.stream().map(HostSchedulingRuleGroupRefInventory::valueOf).collect(Collectors.toList());
    }


    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
