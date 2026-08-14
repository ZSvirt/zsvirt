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
@Inventory(mappingVOClass = MetricRuleTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class MetricRuleTemplateInventory implements Serializable {
    private String name;
    private String monitorTemplateUuid;
    private ComparisonOperator comparisonOperator;
    private int period;
    private int repeatInterval;
    private int repeatCount;
    private String namespace;
    private String metricName;
    private double threshold;
    private EmergencyLevel emergencyLevel;
    private String labels;
    private boolean enableRecovery;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String uuid;

    protected MetricRuleTemplateInventory(MetricRuleTemplateVO vo) {
        this.setName(vo.getName());
        this.setMonitorTemplateUuid(vo.getMonitorTemplateUuid());
        this.setComparisonOperator(vo.getComparisonOperator());
        this.setPeriod(vo.getPeriod());
        this.setRepeatInterval(vo.getRepeatInterval());
        this.setRepeatCount(vo.getRepeatCount());
        this.setNamespace(vo.getNamespace());
        this.setMetricName(vo.getMetricName());
        this.setThreshold(vo.getThreshold());
        this.setEmergencyLevel(vo.getEmergencyLevel());
        this.setLabels(vo.getLabels());
        this.setEnableRecovery(vo.isEnableRecovery());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setUuid(vo.getUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static MetricRuleTemplateInventory valueOf(MetricRuleTemplateVO vo) {
        return new MetricRuleTemplateInventory(vo);
    }

    public static List<MetricRuleTemplateInventory> valueOf1(Collection<MetricRuleTemplateVO> vos) {
        List<MetricRuleTemplateInventory> invs = new ArrayList<MetricRuleTemplateInventory>(vos.size());
        for (MetricRuleTemplateVO vo : vos) {
            invs.add(MetricRuleTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public MetricRuleTemplateInventory() {
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

    public boolean isEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(boolean $paramName) {
        enableRecovery = $paramName;
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
