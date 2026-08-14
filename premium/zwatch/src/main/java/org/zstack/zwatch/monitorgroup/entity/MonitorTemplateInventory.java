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
@Inventory(mappingVOClass = MonitorTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorTemplateInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<MonitorGroupTemplateRefVO> monitorGroupTemplateRefs = new ArrayList<>();

    protected MonitorTemplateInventory(MonitorTemplateVO vo) {
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setMonitorGroupTemplateRefs(vo.getMonitorGroupTemplateRefs());
    }

    public static MonitorTemplateInventory valueOf(MonitorTemplateVO vo) {
        return new MonitorTemplateInventory(vo);
    }

    public static List<MonitorTemplateInventory> valueOf1(Collection<MonitorTemplateVO> vos) {
        List<MonitorTemplateInventory> invs = new ArrayList<MonitorTemplateInventory>(vos.size());
        for (MonitorTemplateVO vo : vos) {
            invs.add(MonitorTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorTemplateInventory() {
    }

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String $paramName) {
        description = $paramName;
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

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public List<MonitorGroupTemplateRefVO> getMonitorGroupTemplateRefs() {
        return monitorGroupTemplateRefs;
    }

    public void setMonitorGroupTemplateRefs(List<MonitorGroupTemplateRefVO> monitorGroupTemplateRefs) {
        this.monitorGroupTemplateRefs = monitorGroupTemplateRefs;
    }
}
