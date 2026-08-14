package org.zstack.zwatch.monitorgroup.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.datatype.EmergencyLevel;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = EventRuleTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class EventRuleTemplateInventory implements Serializable {
    private String name;
    private String monitorTemplateUuid;
    private String namespace;
    private String eventName;
    private EmergencyLevel emergencyLevel;
    private String labels;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;

    protected EventRuleTemplateInventory(EventRuleTemplateVO vo) {
        this.setName(vo.getName());
        this.setMonitorTemplateUuid(vo.getMonitorTemplateUuid());
        this.setNamespace(vo.getNamespace());
        this.setEventName(vo.getEventName());
        this.setEmergencyLevel(vo.getEmergencyLevel());
        this.setLabels(vo.getLabels());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
    }

    public static EventRuleTemplateInventory valueOf(EventRuleTemplateVO vo) {
        return new EventRuleTemplateInventory(vo);
    }

    public static List<EventRuleTemplateInventory> valueOf1(Collection<EventRuleTemplateVO> vos) {
        List<EventRuleTemplateInventory> invs = new ArrayList<EventRuleTemplateInventory>(vos.size());
        for (EventRuleTemplateVO vo : vos) {
            invs.add(EventRuleTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public EventRuleTemplateInventory() {
    }

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public String getMonitorTemplateUuid() {
        return monitorTemplateUuid;
    }

    public void setMonitorTemplateUuid(String $paramName) {
        monitorTemplateUuid = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String $paramName) {
        eventName = $paramName;
    }

    public EmergencyLevel getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(EmergencyLevel $paramName) {
        emergencyLevel = $paramName;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String $paramName) {
        labels = $paramName;
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
}
