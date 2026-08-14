package org.zstack.header.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = SchedulerJobGroupJobRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "jobGroup", inventoryClass = SchedulerJobGroupInventory.class,
                foreignKey = "schedulerJobGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "job", inventoryClass = SchedulerJobInventory.class,
                foreignKey = "schedulerJobUuid", expandedInventoryKey = "uuid")
})
public class SchedulerJobGroupJobRefInventory {
    private String schedulerJobGroupUuid;
    private String schedulerJobUuid;
    private Integer priority;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected SchedulerJobGroupJobRefInventory(SchedulerJobGroupJobRefVO vo) {
        this.setSchedulerJobUuid(vo.getSchedulerJobUuid());
        this.setSchedulerJobGroupUuid(vo.getSchedulerJobGroupUuid());
        this.setPriority(vo.getPriority());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public SchedulerJobGroupJobRefInventory() {
    }

    public static SchedulerJobGroupJobRefInventory valueOf(SchedulerJobGroupJobRefVO vo) {
        return new SchedulerJobGroupJobRefInventory(vo);
    }

    public static List<SchedulerJobGroupJobRefInventory> valueOf(Collection<SchedulerJobGroupJobRefVO> vos) {
        List<SchedulerJobGroupJobRefInventory> invs = new ArrayList<SchedulerJobGroupJobRefInventory>(vos.size());
        for (SchedulerJobGroupJobRefVO vo : vos) {
            invs.add(SchedulerJobGroupJobRefInventory.valueOf(vo));
        }
        return invs;
    }
    public String getSchedulerJobGroupUuid() {
        return schedulerJobGroupUuid;
    }

    public void setSchedulerJobGroupUuid(String schedulerJobGroupUuid) {
        this.schedulerJobGroupUuid = schedulerJobGroupUuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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
