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
@Inventory(mappingVOClass = MonitorGroupAlarmVO.class, collectionValueOfMethod = "valueOf1")
public class MonitorGroupAlarmInventory implements Serializable {
    private String groupUuid;
    private String alarmUuid;
    private String metricRuleTemplateUuid;
    private Timestamp createDate;
    private String uuid;

    protected MonitorGroupAlarmInventory(MonitorGroupAlarmVO vo) {
        this.setGroupUuid(vo.getGroupUuid());
        this.setAlarmUuid(vo.getAlarmUuid());
        this.setMetricRuleTemplateUuid(vo.getMetricRuleTemplateUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setUuid(vo.getUuid());
    }

    public static MonitorGroupAlarmInventory valueOf(MonitorGroupAlarmVO vo) {
        return new MonitorGroupAlarmInventory(vo);
    }

    public static List<MonitorGroupAlarmInventory> valueOf1(Collection<MonitorGroupAlarmVO> vos) {
        List<MonitorGroupAlarmInventory> invs = new ArrayList<MonitorGroupAlarmInventory>(vos.size());
        for (MonitorGroupAlarmVO vo : vos) {
            invs.add(MonitorGroupAlarmInventory.valueOf(vo));
        }
        return invs;
    }

    public MonitorGroupAlarmInventory() {
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String $paramName) {
        groupUuid = $paramName;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String $paramName) {
        alarmUuid = $paramName;
    }

    public String getMetricRuleTemplateUuid() {
        return metricRuleTemplateUuid;
    }

    public void setMetricRuleTemplateUuid(String $paramName) {
        metricRuleTemplateUuid = $paramName;
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
