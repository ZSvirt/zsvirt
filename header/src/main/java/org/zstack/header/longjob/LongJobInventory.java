package org.zstack.header.longjob;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 11/13/17.
 */
@PythonClassInventory
@Inventory(mappingVOClass = LongJobVO.class)
public class LongJobInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String apiId;
    private String jobName;
    private String jobData;
    private String jobResult;
    @NoJsonSchema
    private LongJobState state;
    private String targetResourceUuid;
    private String managementNodeUuid;
    private String parentUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private Long executeTime;

    public LongJobInventory() {
    }

    public LongJobInventory(LongJobVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setApiId(vo.getApiId());
        this.setJobData(vo.getJobData());
        this.setJobResult(vo.getJobResult());
        this.setJobName(vo.getJobName());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setExecuteTime(vo.getExecuteTime());
        this.setDescription(vo.getDescription());
        this.setTargetResourceUuid(vo.getTargetResourceUuid());
        this.setManagementNodeUuid(vo.getManagementNodeUuid());
        this.setParentUuid(vo.getParentUuid());
    }

    public static LongJobInventory valueOf(LongJobVO vo) {
        return new LongJobInventory(vo);
    }

    public static List<LongJobInventory> valueOf(Collection<LongJobVO> vos) {
        List<LongJobInventory> invs = new ArrayList<>();
        for (LongJobVO vo : vos) {
            invs.add(valueOf(vo));
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

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobData() {
        return jobData;
    }

    public void setJobData(String jobData) {
        this.jobData = jobData;
    }

    public String getJobResult() {
        return jobResult;
    }

    public void setJobResult(String jobResult) {
        this.jobResult = jobResult;
    }

    public LongJobState getState() {
        return state;
    }

    public void setState(LongJobState state) {
        this.state = state;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
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

    public Long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(Long executeTime) {
        this.executeTime = executeTime;
    }
}
