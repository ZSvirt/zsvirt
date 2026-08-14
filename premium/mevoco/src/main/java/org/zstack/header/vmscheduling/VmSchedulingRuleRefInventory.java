package org.zstack.header.vmscheduling;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmSchedulingRuleRefVO.class)
public class VmSchedulingRuleRefInventory {
    private String vmGroupUuid;
    private String hostGroupUuid;
    private String vmSchedulingRuleUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;


    public static VmSchedulingRuleRefInventory valueOf(VmSchedulingRuleRefVO vo) {
        VmSchedulingRuleRefInventory ref = new VmSchedulingRuleRefInventory();
        ref.vmGroupUuid = vo.getVmGroupUuid();
        ref.hostGroupUuid = vo.getHostGroupUuid();
        ref.vmSchedulingRuleUuid = vo.getVmSchedulingRuleUuid();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        return ref;
    }

    public static List<VmSchedulingRuleRefInventory> valueOf(Collection<VmSchedulingRuleRefVO> vos) {
        return vos.stream().map(VmSchedulingRuleRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getVmGroupUuid() {
        return vmGroupUuid;
    }

    public void setVmGroupUuid(String vmGroupUuid) {
        this.vmGroupUuid = vmGroupUuid;
    }

    public String getHostGroupUuid() {
        return hostGroupUuid;
    }

    public void setHostGroupUuid(String hostGroupUuid) {
        this.hostGroupUuid = hostGroupUuid;
    }

    public String getVmSchedulingRuleUuid() {
        return vmSchedulingRuleUuid;
    }

    public void setVmSchedulingRuleUuid(String vmSchedulingRuleUuid) {
        this.vmSchedulingRuleUuid = vmSchedulingRuleUuid;
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
