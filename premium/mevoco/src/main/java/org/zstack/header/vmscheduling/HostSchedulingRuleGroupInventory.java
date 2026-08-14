package org.zstack.header.vmscheduling;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = HostSchedulingRuleGroupVO.class)
public class HostSchedulingRuleGroupInventory {
    private String uuid;
    private String name;
    private String description;
    private String zoneUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static HostSchedulingRuleGroupInventory valueOf(HostSchedulingRuleGroupVO vo) {
        HostSchedulingRuleGroupInventory ref = new HostSchedulingRuleGroupInventory();
        ref.uuid = vo.getUuid();
        ref.name = vo.getName();
        ref.description = vo.getDescription();
        ref.zoneUuid = vo.getZoneUuid();
        ref.createDate = vo.getCreateDate();
        ref.lastOpDate = vo.getLastOpDate();
        return ref;
    }

    public static List<HostSchedulingRuleGroupInventory> valueOf(Collection<HostSchedulingRuleGroupVO> vos) {
        return vos.stream().map(HostSchedulingRuleGroupInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
