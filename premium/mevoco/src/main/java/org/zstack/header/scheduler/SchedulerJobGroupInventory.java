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

@Inventory(mappingVOClass = SchedulerJobGroupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "schedulerJobSchedulerTriggerRef", inventoryClass = SchedulerJobGroupSchedulerTriggerRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "schedulerJobGroupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "trigger", expandedField = "schedulerJobSchedulerTriggerRef.trigger"),
})
public class SchedulerJobGroupInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String jobType;
    private String jobData;
    private String zoneUuid;
    /**
     * @desc jobClassName define the job
     */
    @APINoSee
    private String jobClassName;

    @Queryable(mappingClass = SchedulerJobSchedulerTriggerInventory.class,
            joinColumn = @JoinColumn(name = "schedulerJobGroupUuid", referencedColumnName = "schedulerTriggerUuid"))
    private List<String> triggersUuid;

    @Queryable(mappingClass = SchedulerJobInventory.class,
            joinColumn = @JoinColumn(name = "schedulerJobGroupUuid", referencedColumnName = "uuid"))
    private List<String> jobsUuid;

    protected SchedulerJobGroupInventory(SchedulerJobGroupVO vo) {
        uuid = vo.getUuid();
        name = vo.getName();
        description = vo.getDescription();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
        jobData = vo.getJobData();
        jobType = vo.getJobType();
        jobClassName = vo.getJobClassName();
        state = vo.getState();
        zoneUuid= vo.getZoneUuid();

        triggersUuid = new ArrayList<String>(vo.getAddedTriggerRefs().size());
        for (SchedulerJobGroupSchedulerTriggerRefVO ref : vo.getAddedTriggerRefs()) {
            triggersUuid.add(ref.getSchedulerTriggerUuid());
        }

        jobsUuid = new ArrayList<>(vo.getAddedJobsRefs().size());
        for (SchedulerJobGroupJobRefVO ref : vo.getAddedJobsRefs()) {
            jobsUuid.add(ref.getSchedulerJobUuid());
        }
    }

    public SchedulerJobGroupInventory() {
    }

    public static SchedulerJobGroupInventory valueOf(SchedulerJobGroupVO vo) {
        return new SchedulerJobGroupInventory(vo);
    }

    public static List<SchedulerJobGroupInventory> valueOf(Collection<SchedulerJobGroupVO> vos) {
        List<SchedulerJobGroupInventory> invs = new ArrayList<SchedulerJobGroupInventory>(vos.size());
        for (SchedulerJobGroupVO vo : vos) {
            invs.add(SchedulerJobGroupInventory.valueOf(vo));
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

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
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

    public List<String> getJobsUuid() {
        return jobsUuid;
    }

    public void setJobsUuid(List<String> jobsUuid) {
        this.jobsUuid = jobsUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
