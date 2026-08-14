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
@Inventory(mappingVOClass = MonitorGroupEventSubscriptionVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorGroupEventSubscriptionInventory implements Serializable {
    private String groupUuid;
    private String eventSubscriptionUuid;
    private String eventRuleTemplateUuid;
    private Timestamp createDate;
    private String uuid;

    protected MonitorGroupEventSubscriptionInventory(MonitorGroupEventSubscriptionVO vo) {
        this.setGroupUuid(vo.getGroupUuid());
        this.setEventSubscriptionUuid(vo.getEventSubscriptionUuid());
        this.setEventRuleTemplateUuid(vo.getEventRuleTemplateUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setUuid(vo.getUuid());
    }

    public static MonitorGroupEventSubscriptionInventory valueOf(MonitorGroupEventSubscriptionVO vo) {
        return new MonitorGroupEventSubscriptionInventory(vo);
    }

    public static List<MonitorGroupEventSubscriptionInventory> valueOf1(Collection<MonitorGroupEventSubscriptionVO> vos) {
        List<MonitorGroupEventSubscriptionInventory> invs = new ArrayList<MonitorGroupEventSubscriptionInventory>(vos.size());
        for (MonitorGroupEventSubscriptionVO vo : vos) {
            invs.add(MonitorGroupEventSubscriptionInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorGroupEventSubscriptionInventory() {
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String $paramName) {
        groupUuid = $paramName;
    }

    public String getEventSubscriptionUuid() {
        return eventSubscriptionUuid;
    }

    public void setEventSubscriptionUuid(String $paramName) {
        eventSubscriptionUuid = $paramName;
    }

    public String getEventRuleTemplateUuid() {
        return eventRuleTemplateUuid;
    }

    public void setEventRuleTemplateUuid(String $paramName) {
        eventRuleTemplateUuid = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }
}
