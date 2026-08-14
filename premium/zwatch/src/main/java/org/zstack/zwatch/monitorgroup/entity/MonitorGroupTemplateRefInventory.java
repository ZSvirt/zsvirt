package org.zstack.zwatch.monitorgroup.entity;

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
@Inventory(mappingVOClass = MonitorGroupTemplateRefVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorGroupTemplateRefInventory implements Serializable {
    private String uuid;
    private String templateUuid;
    private String groupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private boolean isApplied;

    protected MonitorGroupTemplateRefInventory(MonitorGroupTemplateRefVO vo) {
        this.setUuid(vo.getUuid());
        this.setTemplateUuid(vo.getTemplateUuid());
        this.setGroupUuid(vo.getGroupUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setApplied(vo.isApplied());
    }

    public static MonitorGroupTemplateRefInventory valueOf(MonitorGroupTemplateRefVO vo) {
        return new MonitorGroupTemplateRefInventory(vo);
    }

    public static List<MonitorGroupTemplateRefInventory> valueOf1(Collection<MonitorGroupTemplateRefVO> vos) {
        List<MonitorGroupTemplateRefInventory> invs = new ArrayList<MonitorGroupTemplateRefInventory>(vos.size());
        for (MonitorGroupTemplateRefVO vo : vos) {
            invs.add(MonitorGroupTemplateRefInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorGroupTemplateRefInventory() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String $paramName) {
        templateUuid = $paramName;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String $paramName) {
        groupUuid = $paramName;
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

    public boolean isApplied() {
        return isApplied;
    }

    public void setApplied(boolean applied) {
        isApplied = applied;
    }
}
