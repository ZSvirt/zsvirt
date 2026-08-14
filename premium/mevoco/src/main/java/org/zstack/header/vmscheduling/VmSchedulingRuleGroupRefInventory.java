package org.zstack.header.vmscheduling;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmSchedulingRuleGroupRefVO.class)
public class VmSchedulingRuleGroupRefInventory {
    private String vmGroupUuid;
    private String vmUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmSchedulingRuleGroupRefInventory valueOf(VmSchedulingRuleGroupRefVO vo) {
        VmSchedulingRuleGroupRefInventory ref = new VmSchedulingRuleGroupRefInventory();
        ref.vmGroupUuid = vo.getVmGroupUuid();
        ref.vmUuid = vo.getVmUuid();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        return ref;
    }

    public static List<VmSchedulingRuleGroupRefInventory> valueOf(Collection<VmSchedulingRuleGroupRefVO> vos) {
        return vos.stream().map(VmSchedulingRuleGroupRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
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
