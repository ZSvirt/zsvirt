package org.zstack.ha.entity;

import org.zstack.ha.header.VmHaLevel;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceEO.class, myField = "uuid", targetField = "uuid")
        }
)
public class VmHaVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    @Enumerated(EnumType.STRING)
    private VmHaLevel haLevel;

    @Column
    private Timestamp haLevelUpdateTime;

    @Column
    private String inhibitionReason;

    @Column
    private Timestamp inhibitionTime;

    @Column
    private String lastFencerName;

    @Column
    private String lastFenceHostUuid;

    @Column
    private Timestamp lastFenceTime;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public VmHaLevel getHaLevel() {
        return haLevel;
    }

    public void setHaLevel(VmHaLevel haLevel) {
        this.haLevel = haLevel;
    }

    public Timestamp getHaLevelUpdateTime() {
        return haLevelUpdateTime;
    }

    public void setHaLevelUpdateTime(Timestamp haLevelUpdateTime) {
        this.haLevelUpdateTime = haLevelUpdateTime;
    }

    public String getInhibitionReason() {
        return inhibitionReason;
    }

    public void setInhibitionReason(String inhibitionReason) {
        this.inhibitionReason = inhibitionReason;
    }

    public Timestamp getInhibitionTime() {
        return inhibitionTime;
    }

    public void setInhibitionTime(Timestamp inhibitionTime) {
        this.inhibitionTime = inhibitionTime;
    }

    public String getLastFencerName() {
        return lastFencerName;
    }

    public void setLastFencerName(String lastFencerName) {
        this.lastFencerName = lastFencerName;
    }

    public String getLastFenceHostUuid() {
        return lastFenceHostUuid;
    }

    public void setLastFenceHostUuid(String lastFenceHostUuid) {
        this.lastFenceHostUuid = lastFenceHostUuid;
    }

    public Timestamp getLastFenceTime() {
        return lastFenceTime;
    }

    public void setLastFenceTime(Timestamp lastFenceTime) {
        this.lastFenceTime = lastFenceTime;
    }
}
