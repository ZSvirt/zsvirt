package org.zstack.header.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by root on 7/18/16.
 */
@Inventory(mappingVOClass = SchedulerJobVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "schedulerJobSchedulerTriggerRef", inventoryClass = SchedulerJobSchedulerTriggerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "schedulerJobUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "trigger", expandedField = "schedulerJobSchedulerTriggerRef.trigger"),
})
public class SchedulerJobInventory implements Serializable {
    private String uuid;
    private String targetResourceUuid;
    private String name;
    private String description;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    /**
     * @desc jobClassName define the job
     */
    private String jobData;
    private String jobClassName;

    @Queryable(mappingClass = SchedulerJobSchedulerTriggerInventory.class,
            joinColumn = @JoinColumn(name = "schedulerJobUuid", referencedColumnName = "schedulerTriggerUuid"))
    private List<String> triggersUuid;

    @Queryable(mappingClass = SchedulerJobGroupJobRefInventory.class,
            joinColumn = @JoinColumn(name = "schedulerJobUuid", referencedColumnName = "schedulerJobGroupUuid"))
    private List<String> schedulerJobGroupUuids;

    protected SchedulerJobInventory(SchedulerJobVO vo) {
        uuid = vo.getUuid();
        name = vo.getName();
        description = vo.getDescription();
        targetResourceUuid = vo.getTargetResourceUuid();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
        jobData = vo.getJobData();
        jobClassName = vo.getJobClassName();
        state = vo.getState();

        triggersUuid = new ArrayList<String>(vo.getAddedTriggerRefs().size());
        for (SchedulerJobSchedulerTriggerRefVO ref : vo.getAddedTriggerRefs()) {
            triggersUuid.add(ref.getSchedulerTriggerUuid());
        }

        schedulerJobGroupUuids = new ArrayList<String>(vo.getJobGroupRefs().size());
        for (SchedulerJobGroupJobRefVO ref : vo.getJobGroupRefs()) {
            schedulerJobGroupUuids.add(ref.getSchedulerJobGroupUuid());
        }
    }

    public SchedulerJobInventory() {

    }

    public static SchedulerJobInventory valueOf(SchedulerJobVO vo) {
        return new SchedulerJobInventory(vo);
    }

    public static List<SchedulerJobInventory> valueOf(Collection<SchedulerJobVO> vos) {
        List<SchedulerJobInventory> invs = new ArrayList<SchedulerJobInventory>(vos.size());
        for (SchedulerJobVO vo : vos) {
            invs.add(SchedulerJobInventory.valueOf(vo));
        }
        return invs;
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

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getJobClassName() {
        return jobClassName;
    }

    public void setJobClassName(String jobClassName) {
        this.jobClassName = jobClassName;
    }

    public List<String> getTriggersUuid() {
        return triggersUuid;
    }

    public void setTriggersUuid(List<String> triggersUuid) {
        this.triggersUuid = triggersUuid;
    }

    public List<String> getSchedulerJobGroupUuids() {
        return schedulerJobGroupUuids;
    }

    public void setSchedulerJobGroupUuids(List<String> schedulerJobGroupUuids) {
        this.schedulerJobGroupUuids = schedulerJobGroupUuids;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
