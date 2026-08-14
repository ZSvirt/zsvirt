package org.zstack.core.job;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class JobQueueVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column
    private String name;
    
    @Column
    private String owner;

    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String workerManagementNodeId;

    @Column
    private Date takenDate;
    
    public JobQueueVO(String name, String owner) {
        super();
        this.name = name;
        this.owner = owner;
    }

    public JobQueueVO() {
    }
    
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Date getTakenDate() {
        return takenDate;
    }

    public void setTakenDate(Date takenDate) {
        this.takenDate = takenDate;
    }

    public String getWorkerManagementNodeId() {
        return workerManagementNodeId;
    }

    public void setWorkerManagementNodeId(String workerManagementNodeId) {
        this.workerManagementNodeId = workerManagementNodeId;
    }
}
