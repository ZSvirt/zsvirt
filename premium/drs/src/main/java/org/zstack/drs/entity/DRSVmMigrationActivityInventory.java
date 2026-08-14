package org.zstack.drs.entity;

import org.zstack.drs.data.DRSVmMigrationStatus;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by lining on 2019/12/12.
 */

@PythonClassInventory
@Inventory(mappingVOClass = DRSVmMigrationActivityVO.class, collectionValueOfMethod = "valueOf1")
public class DRSVmMigrationActivityInventory implements Serializable {
    private String drsUuid;
    private String uuid;
    private String vmUuid;
    private String vmSourceHostUuid;
    private String vmTargetHostUuid;
    private String status;
    private String result;
    private String reason;
    private String adviceUuid;
    private String cause;
    private Timestamp endDate;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected DRSVmMigrationActivityInventory(DRSVmMigrationActivityVO vo) {
        this.setDrsUuid(vo.getDrsUuid());
        this.setUuid(vo.getUuid());
        this.setVmUuid(vo.getVmUuid());
        this.setVmSourceHostUuid(vo.getVmSourceHostUuid());
        this.setVmTargetHostUuid(vo.getVmTargetHostUuid());
        this.setStatus(vo.getStatus().toString());
        this.setResult(vo.getResult());
        this.setReason(vo.getReason());
        this.setAdviceUuid(vo.getAdviceUuid());
        this.setCause(vo.getCause().toString());
        this.setEndDate(vo.getEndDate());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static DRSVmMigrationActivityInventory valueOf(DRSVmMigrationActivityVO vo) {
        return new DRSVmMigrationActivityInventory(vo);
    }

    public static List<DRSVmMigrationActivityInventory> valueOf1(Collection<DRSVmMigrationActivityVO> vos) {
        List<DRSVmMigrationActivityInventory> invs = new ArrayList<DRSVmMigrationActivityInventory>(vos.size());
        for (DRSVmMigrationActivityVO vo : vos) {
            invs.add(DRSVmMigrationActivityInventory.valueOf(vo));
        }
        return invs;
    }

    public DRSVmMigrationActivityInventory() {
    }

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String $paramName) {
        drsUuid = $paramName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String $paramName) {
        vmUuid = $paramName;
    }

    public String getVmSourceHostUuid() {
        return vmSourceHostUuid;
    }

    public void setVmSourceHostUuid(String $paramName) {
        vmSourceHostUuid = $paramName;
    }

    public String getVmTargetHostUuid() {
        return vmTargetHostUuid;
    }

    public void setVmTargetHostUuid(String $paramName) {
        vmTargetHostUuid = $paramName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String $paramName) {
        status = $paramName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String $paramName) {
        result = $paramName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String $paramName) {
        reason = $paramName;
    }

    public String getAdviceUuid() {
        return adviceUuid;
    }

    public void setAdviceUuid(String $paramName) {
        adviceUuid = $paramName;
    }

    public Timestamp getEndDate() {
        return endDate;
    }

    public void setEndDate(Timestamp $paramName) {
        endDate = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp $paramName) {
        lastOpDate = $paramName;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}
