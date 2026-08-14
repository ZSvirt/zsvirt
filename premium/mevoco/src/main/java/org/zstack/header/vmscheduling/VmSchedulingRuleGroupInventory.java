package org.zstack.header.vmscheduling;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = VmSchedulingRuleGroupVO.class)
public class VmSchedulingRuleGroupInventory {
    private String uuid;
    private String name;
    private String description;
    private String appliance;
    private String zoneUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmSchedulingRuleGroupInventory valueOf(VmSchedulingRuleGroupVO vo) {
        VmSchedulingRuleGroupInventory ref = new VmSchedulingRuleGroupInventory();
        ref.uuid = vo.getUuid();
        ref.name = vo.getName();
        ref.description = vo.getDescription();
        ref.appliance = vo.getAppliance();
        ref.zoneUuid = vo.getZoneUuid();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        return ref;
    }

    public static List<VmSchedulingRuleGroupInventory> valueOf(Collection<VmSchedulingRuleGroupVO> vos) {
        return vos.stream().map(VmSchedulingRuleGroupInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAppliance() {
        return appliance;
    }

    public void setAppliance(String appliance) {
        this.appliance = appliance;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
