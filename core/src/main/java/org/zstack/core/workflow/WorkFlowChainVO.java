package org.zstack.core.workflow;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class WorkFlowChainVO {
    @Id
    @Column
    private String uuid;
    
    @Column
    private String name;
    
    @Column
    private String owner;
    
    @Column
    @Enumerated(EnumType.STRING)
    private WorkFlowChainState state;
    
    @Column
    private String reason;
    
    @Column
    private int totalWorkFlows;
    
    @Column
    private int currentPosition;
    
    @Column
    private Date OperationDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public WorkFlowChainState getState() {
        return state;
    }

    public void setState(WorkFlowChainState state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getTotalWorkFlows() {
        return totalWorkFlows;
    }

    public void setTotalWorkFlows(int totalWorkFlows) {
        this.totalWorkFlows = totalWorkFlows;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Date getOperationDate() {
        return OperationDate;
    }

    public void setOperationDate(Date operationDate) {
        OperationDate = operationDate;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
