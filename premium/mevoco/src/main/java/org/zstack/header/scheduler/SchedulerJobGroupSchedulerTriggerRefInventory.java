package org.zstack.header.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SchedulerJobGroupSchedulerTriggerRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "trigger", inventoryClass = SchedulerTriggerInventory.class,
                foreignKey = "schedulerTriggerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "jobGroup", inventoryClass = SchedulerJobGroupInventory.class,
                foreignKey = "schedulerJobGroupUuid", expandedInventoryKey = "uuid")
})
public class SchedulerJobGroupSchedulerTriggerRefInventory {
    private String schedulerJobGroupUuid;
    private String schedulerTriggerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public SchedulerJobGroupSchedulerTriggerRefInventory() {
    }

    protected SchedulerJobGroupSchedulerTriggerRefInventory(SchedulerJobGroupSchedulerTriggerRefVO vo) {
        this.setSchedulerJobGroupUuid(vo.getSchedulerJobGroupUuid());
        this.setSchedulerTriggerUuid(vo.getSchedulerTriggerUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static SchedulerJobGroupSchedulerTriggerRefInventory valueOf(SchedulerJobGroupSchedulerTriggerRefVO vo) {
        return new SchedulerJobGroupSchedulerTriggerRefInventory(vo);
    }

    public static List<SchedulerJobGroupSchedulerTriggerRefInventory> valueOf(Collection<SchedulerJobGroupSchedulerTriggerRefVO> vos) {
        List<SchedulerJobGroupSchedulerTriggerRefInventory> invs = new ArrayList<SchedulerJobGroupSchedulerTriggerRefInventory>(vos.size());
        for (SchedulerJobGroupSchedulerTriggerRefVO vo : vos) {
            invs.add(SchedulerJobGroupSchedulerTriggerRefInventory.valueOf(vo));
        }
        return invs;
    }

    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
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
