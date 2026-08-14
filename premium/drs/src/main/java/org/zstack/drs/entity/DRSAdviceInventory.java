package org.zstack.drs.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
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
@Inventory(mappingVOClass = DRSAdviceVO.class, collectionValueOfMethod = "valueOf1")
public class DRSAdviceInventory implements Serializable {
    private String uuid;
    private String drsUuid;
    private String adviceGroupUuid;
    private String vmUuid;
    private String vmSourceHostUuid;
    private String vmTargetHostUuid;
    private String reason;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected DRSAdviceInventory(DRSAdviceVO vo) {
        this.setUuid(vo.getUuid());
        this.setDrsUuid(vo.getDrsUuid());
        this.setAdviceGroupUuid(vo.getAdviceGroupUuid());
        this.setVmUuid(vo.getVmUuid());
        this.setVmSourceHostUuid(vo.getVmSourceHostUuid());
        this.setVmTargetHostUuid(vo.getVmTargetHostUuid());
        this.setReason(vo.getReason());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static DRSAdviceInventory valueOf(DRSAdviceVO vo) {
        return new DRSAdviceInventory(vo);
    }

    public static List<DRSAdviceInventory> valueOf1(Collection<DRSAdviceVO> vos) {
        List<DRSAdviceInventory> invs = new ArrayList<DRSAdviceInventory>(vos.size());
        for (DRSAdviceVO vo : vos) {
            invs.add(DRSAdviceInventory.valueOf(vo));
        }
        return invs;
    }

    public DRSAdviceInventory() {
    }

    public String getDrsUuid() {
        return drsUuid;
    }

    public void setDrsUuid(String $paramName) {
        drsUuid = $paramName;
    }

    public String getAdviceGroupUuid() {
        return adviceGroupUuid;
    }

    public void setAdviceGroupUuid(String $paramName) {
        adviceGroupUuid = $paramName;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String $paramName) {
        reason = $paramName;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
