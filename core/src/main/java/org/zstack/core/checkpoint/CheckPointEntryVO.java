package org.zstack.core.checkpoint;

import javax.persistence.*;

@Entity
@Table(name="check_point_entry")
public class CheckPointEntryVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="check_point_id")
    private long checkPointId;
    
    @Column(name="name")
    private String name;
    
    @Column(name="state")
    @Enumerated(EnumType.STRING)
    private CheckPointState state;
    
    @Column(name="reason")
    private String reason;
    
    @Column(name="context")
    private byte[] context;
    
    CheckPointEntryVO(long checkPointId, String name) {
        super();
        this.checkPointId = checkPointId;
        this.name = name;
        this.state = CheckPointState.Creating;
    }
    
    CheckPointEntryVO() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCheckPointId() {
        return checkPointId;
    }

    public void setCheckPointId(long checkPointId) {
        this.checkPointId = checkPointId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public byte[] getContext() {
        return context;
    }

    public void setContext(byte[] context) {
        this.context = context;
    }

    public CheckPointState getState() {
        return state;
    }

    public void setState(CheckPointState state) {
        this.state = state;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    
    
}
