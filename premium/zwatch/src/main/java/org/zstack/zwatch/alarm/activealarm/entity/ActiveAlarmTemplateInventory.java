package org.zstack.zwatch.alarm.activealarm.entity;

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
@Inventory(mappingVOClass = ActiveAlarmTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class ActiveAlarmTemplateInventory implements Serializable {
    private String uuid;
    private String alarmName;
    private ComparisonOperator comparisonOperator;
    private int period;
    private int repeatInterval;
    private int repeatCount;
    private String namespace;
    private String metricName;
    private double threshold;
    private EmergencyLevel emergencyLevel;
    private String labels;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected ActiveAlarmTemplateInventory(ActiveAlarmTemplateVO vo) {
        this.setUuid(vo.getUuid());
        this.setAlarmName(vo.getAlarmName());
        this.setComparisonOperator(vo.getComparisonOperator());
        this.setPeriod(vo.getPeriod());
        this.setRepeatInterval(vo.getRepeatInterval());
        this.setRepeatCount(vo.getRepeatCount());
        this.setNamespace(vo.getNamespace());
        this.setMetricName(vo.getMetricName());
        this.setThreshold(vo.getThreshold());
        this.setEmergencyLevel(vo.getEmergencyLevel());
        this.setLabels(vo.getLabels());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static ActiveAlarmTemplateInventory valueOf(ActiveAlarmTemplateVO vo) {
        return new ActiveAlarmTemplateInventory(vo);
    }

    public static List<ActiveAlarmTemplateInventory> valueOf1(Collection<ActiveAlarmTemplateVO> vos) {
        List<ActiveAlarmTemplateInventory> invs = new ArrayList<ActiveAlarmTemplateInventory>(vos.size());
        for (ActiveAlarmTemplateVO vo : vos) {
            invs.add(ActiveAlarmTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public ActiveAlarmTemplateInventory() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String $paramName) {
        alarmName = $paramName;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator $paramName) {
        comparisonOperator = $paramName;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int $paramName) {
        period = $paramName;
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int $paramName) {
        repeatInterval = $paramName;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int $paramName) {
        repeatCount = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String $paramName) {
        metricName = $paramName;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double $paramName) {
        threshold = $paramName;
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
}
