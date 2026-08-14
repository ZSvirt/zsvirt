package org.zstack.storage.device.localRaid;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = PhysicalDriveSmartSelfTestHistoryVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "raidPhysicalDrive", inventoryClass = RaidPhysicalDriveInventory.class,
                foreignKey = "raidControllerUuid", expandedInventoryKey = "uuid"),
})
public class PhysicalDriveSmartSelfTestHistoryInventory implements Serializable {
    private Long id;

    private String raidPhysicalDriveUuid;

    private RunningState runningState;

    private String testResult;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public PhysicalDriveSmartSelfTestHistoryInventory() {
    }

    public PhysicalDriveSmartSelfTestHistoryInventory(PhysicalDriveSmartSelfTestHistoryVO vo) {
        this.setId(vo.getId());
        this.setRaidPhysicalDriveUuid(vo.getRaidPhysicalDriveUuid());
        this.setRunningState(vo.getRunningState());
        this.setTestResult(vo.getTestResult());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static PhysicalDriveSmartSelfTestHistoryInventory valueOf(PhysicalDriveSmartSelfTestHistoryVO vo) {
        return new PhysicalDriveSmartSelfTestHistoryInventory(vo);
    }

    public static List<PhysicalDriveSmartSelfTestHistoryInventory> valueOf(Collection<PhysicalDriveSmartSelfTestHistoryVO> vos) {
        List<PhysicalDriveSmartSelfTestHistoryInventory> invs = new ArrayList<>(vos.size());
        for (PhysicalDriveSmartSelfTestHistoryVO vo : vos) {
            invs.add(vo.toInventory());
        }
        return invs;
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
