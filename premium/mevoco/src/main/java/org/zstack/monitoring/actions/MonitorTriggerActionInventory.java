package org.zstack.monitoring.actions;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.monitoring.MonitorTriggerActionRefInventory;
import org.zstack.monitoring.MonitorTriggerActionRefVO;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by xing5 on 2017/6/11.
 */
@Inventory(mappingVOClass = MonitorTriggerActionVO.class)
@ExpandedQueries(
        @ExpandedQuery(expandedField = "triggerRef", inventoryClass = MonitorTriggerActionRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "actionUuid", hidden = true)
)
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "trigger", expandedField = "triggerRef.trigger")
})
public class MonitorTriggerActionInventory {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @TypeField
    private String type;
    private List<String> triggerUuids;

    public MonitorTriggerActionInventory() {
    }

    public MonitorTriggerActionInventory(MonitorTriggerActionVO vo) {
        setUuid(vo.getUuid());
        setName(vo.getName());
        setState(vo.getState().toString());
        setDescription(vo.getDescription());
        setCreateDate(vo.getCreateDate());
        setLastOpDate(vo.getLastOpDate());
        setType(vo.getType());
        setTriggerUuids(vo.getTriggerRefs().stream().map(MonitorTriggerActionRefVO::getTriggerUuid).collect(Collectors.toList()));
    }

    public MonitorTriggerActionInventory(MonitorTriggerActionInventory other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.state = other.state;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.type = other.type;
        this.triggerUuids = other.triggerUuids;
    }

    public List<String> getTriggerUuids() {
        return triggerUuids;
    }

    public void setTriggerUuids(List<String> triggerUuids) {
        this.triggerUuids = triggerUuids;
    }

    public static List<MonitorTriggerActionInventory> valueOf(Collection<MonitorTriggerActionVO> vos) {
        List<MonitorTriggerActionInventory> invs = new ArrayList<>();
        for (MonitorTriggerActionVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static MonitorTriggerActionInventory valueOf(MonitorTriggerActionVO vo) {
        return new MonitorTriggerActionInventory(vo);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
