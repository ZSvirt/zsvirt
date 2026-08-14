package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.alarm.AlarmStatus;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = MonitorGroupInstanceVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorGroupInstanceInventory implements Serializable {
    private String groupUuid;
    private String instanceResourceType;
    private String instanceUuid;
    private AlarmStatus status;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;

    protected MonitorGroupInstanceInventory(MonitorGroupInstanceVO vo) {
        this.setGroupUuid(vo.getGroupUuid());
        this.setInstanceResourceType(vo.getInstanceResourceType());
        this.setInstanceUuid(vo.getInstanceUuid());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static MonitorGroupInstanceInventory valueOf(MonitorGroupInstanceVO vo) {
        return new MonitorGroupInstanceInventory(vo);
    }

    public static List<MonitorGroupInstanceInventory> valueOf1(Collection<MonitorGroupInstanceVO> vos) {
        List<MonitorGroupInstanceInventory> invs = new ArrayList<MonitorGroupInstanceInventory>(vos.size());
        for (MonitorGroupInstanceVO vo : vos) {
            invs.add(MonitorGroupInstanceInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorGroupInstanceInventory() {
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String $paramName) {
        groupUuid = $paramName;
    }

    public String getInstanceResourceType() {
        return instanceResourceType;
    }

    public void setInstanceResourceType(String $paramName) {
        instanceResourceType = $paramName;
    }

    public String getInstanceUuid() {
        return instanceUuid;
    }

    public void setInstanceUuid(String $paramName) {
        instanceUuid = $paramName;
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

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }
}
