package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.core.Platform;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

@PythonClassInventory
@Inventory(mappingVOClass = MonitorGroupVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorGroupInventory implements Serializable {
    private String name;
    private MonitorGroupState state;
    private String actions;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;
    private List<MonitorGroupTemplateRefVO> monitorGroupTemplateRefs = new ArrayList<>();

    protected MonitorGroupInventory(MonitorGroupVO vo) {
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setActions(vo.getActions());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setMonitorGroupTemplateRefs(vo.getMonitorGroupTemplateRefs());
    }

    public static MonitorGroupInventory valueOf(MonitorGroupVO vo) {
        return new MonitorGroupInventory(vo);
    }

    public static List<MonitorGroupInventory> valueOf1(Collection<MonitorGroupVO> vos) {
        List<MonitorGroupInventory> invs = new ArrayList<MonitorGroupInventory>(vos.size());
        for (MonitorGroupVO vo : vos) {
            invs.add(MonitorGroupInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorGroupInventory() {
    }

    public static MonitorGroupInventory __example__() {
        MonitorGroupInventory inventory = new MonitorGroupInventory();
        inventory.setName("group-vm");
        inventory.setDescription("desc");
        inventory.setCreateDate(new Timestamp(System.currentTimeMillis()));
        inventory.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        inventory.setUuid(Platform.getUuid());
        inventory.setMonitorGroupTemplateRefs(asList(new MonitorGroupTemplateRefVO()));
        return inventory;
    }

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public MonitorGroupState getState() {
        return state;
    }

    public void setState(MonitorGroupState $paramName) {
        state = $paramName;
    }

    public String getActions() {
        return actions;
    }

    public void setActions(String $paramName) {
        actions = $paramName;
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
