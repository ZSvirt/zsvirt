package org.zstack.monitoring;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.monitoring.actions.MonitorTriggerActionInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/6/12.
 */
@Inventory(mappingVOClass = MonitorTriggerActionRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "trigger", inventoryClass = MonitorTriggerInventory.class,
                foreignKey = "triggerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "action", inventoryClass = MonitorTriggerActionInventory.class,
                foreignKey = "actionUuid", expandedInventoryKey = "uuid")
})
public class MonitorTriggerActionRefInventory {
    private String triggerUuid;
    private String actionUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static MonitorTriggerActionRefInventory valueOf(MonitorTriggerActionRefVO vo) {
        MonitorTriggerActionRefInventory inv = new MonitorTriggerActionRefInventory();
        inv.setTriggerUuid(vo.getTriggerUuid());
        inv.setActionUuid(vo.getActionUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<MonitorTriggerActionRefInventory> valueOf(Collection<MonitorTriggerActionRefVO> vos) {
        List<MonitorTriggerActionRefInventory> invs = new ArrayList<>();
        for (MonitorTriggerActionRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
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
