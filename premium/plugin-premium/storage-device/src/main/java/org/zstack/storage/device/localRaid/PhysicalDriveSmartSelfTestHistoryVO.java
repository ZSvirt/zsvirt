package org.zstack.storage.device.localRaid;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = RaidPhysicalDriveVO.class, myField = "raidPhysicalDriveUuid", targetField = "uuid"),
        }
)
public class PhysicalDriveSmartSelfTestHistoryVO implements ToInventory {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = RaidPhysicalDriveVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String raidPhysicalDriveUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private RunningState runningState;

    @Column
    private String testResult;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRaidPhysicalDriveUuid() {
        return raidPhysicalDriveUuid;
    }

    public void setRaidPhysicalDriveUuid(String raidPhysicalDriveUuid) {
        this.raidPhysicalDriveUuid = raidPhysicalDriveUuid;
    }

    public RunningState getRunningState() {
        return runningState;
    }

    public void setRunningState(RunningState runningState) {
        this.runningState = runningState;
    }

    public String getTestResult() {
        return testResult;
    }

    public void setTestResult(String testResult) {
        this.testResult = testResult;
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
