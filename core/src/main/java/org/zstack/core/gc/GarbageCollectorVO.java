package org.zstack.core.gc;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 8/5/2015.
 */
@Entity
@Table
@BaseResource
public class GarbageCollectorVO extends ResourceVO {
    @Column
    private String name;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String managementNodeUuid;

    @Column
    private String runnerClass;

    @Column
    private String context;

    @Column
    @Enumerated(EnumType.STRING)
    private GCStatus status;

    @Column
    private String type;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRunnerClass() {
        return runnerClass;
    }

    public void setRunnerClass(String runnerClass) {
        this.runnerClass = runnerClass;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public GCStatus getStatus() {
        return status;
    }

    public void setStatus(GCStatus status) {
        this.status = status;
    }

    public String getManagementNodeUuid() {
        return managementNodeUuid;
    }

    public void setManagementNodeUuid(String managementNodeUuid) {
        this.managementNodeUuid = managementNodeUuid;
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
