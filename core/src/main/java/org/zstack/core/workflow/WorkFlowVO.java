package org.zstack.core.workflow;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class WorkFlowVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column
    private String chainUuid;
    
    @Column
    private String name;
    
    @Column
    @Enumerated(EnumType.STRING) 
    private WorkFlowState state;
    
    @Column
    private String reason;
    
    @Column
    private int position;
    
    @Column
    private byte[] context;
    
    @Column
    private Date OperationDate;

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

    public WorkFlowState getState() {
        return state;
    }

    public void setState(WorkFlowState state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public byte[] getContext() {
        return context;
    }

    public void setContext(byte[] context) {
        this.context = context;
    }

    public Date getOperationDate() {
        return OperationDate;
    }

    public void setOperationDate(Date operationDate) {
        OperationDate = operationDate;
    }

    public String getChainUuid() {
        return chainUuid;
    }

    public void setChainUuid(String chainUuid) {
        this.chainUuid = chainUuid;
    }
}
