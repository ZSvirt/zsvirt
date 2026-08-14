package org.zstack.ha;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @Author: DaoDao
 * @Date: 2023/4/4
 */
@Entity
@Table
public class HaStrategyConditionVO implements ToInventory {
    @Column
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String fencerName;

    @Column
    @Enumerated(value = EnumType.STRING)
    private HaStrategyState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getFencerName() {
        return fencerName;
    }

    public void setFencerName(String fencerName) {
        this.fencerName = fencerName;
    }

    public HaStrategyState getState() {
        return state;
    }

    public void setState(HaStrategyState state) {
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
}
