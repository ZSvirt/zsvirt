package org.zstack.core.checkpoint;

import org.zstack.header.vo.Uuid;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="check_point")
public class CheckPointVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="name")
    private String name;
    
    @Column(name="uuid")
    @Uuid
    private String uuid;
    
    @Column(name="state")
    @Enumerated(EnumType.STRING)
    private CheckPointState state;
    
    @Column(name="context")
    private byte[] context;
    
    @Column(name="op_date")
    private Date opDate;
    
    public CheckPointVO(String name) {
        super();
        this.name = name;
        this.state = CheckPointState.Creating;
    }
    
    CheckPointVO() {
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public byte[] getContext() {
        return context;
    }

    public void setContext(byte[] context) {
        this.context = context;
    }

    public Date getOpDate() {
        return opDate;
    }

    public void setOpDate(Date opDate) {
        this.opDate = opDate;
    }

    public CheckPointState getState() {
        return state;
    }

    public void setState(CheckPointState state) {
        this.state = state;
    }
}
