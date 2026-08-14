package org.zstack.zwatch.migratedb;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.datatype.AlarmData;
import org.zstack.zwatch.datatype.AlarmDataV2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = AlarmRecordsVO.class, collectionValueOfMethod = "valueOf1")
public class AlarmRecordsInventory implements Serializable {
    private long id;
    private long createTime;
    private String accountUuid;
    private String alarmName;
    private String alarmStatus;
    private String alarmUuid;
    private String comparisonOperator;
    private String context;
    private String dataUuid;
    private String emergencyLevel;
    private String labels;
    private String metricName;
    private Double metricValue;
    private String namespace;
    private int period;
    private Boolean readStatus;
    private String operatorAccountUuid;
    private String resourceUuid;
    private Double threshold;

    protected AlarmRecordsInventory(AlarmRecordsVO vo) {
        this.setId(vo.getId());
        this.setCreateTime(vo.getCreateTime());
        this.setAccountUuid(vo.getAccountUuid());
        this.setAlarmName(vo.getAlarmName());
        this.setAlarmStatus(vo.getAlarmStatus());
        this.setAlarmUuid(vo.getAlarmUuid());
        this.setComparisonOperator(vo.getComparisonOperator());
        this.setContext(vo.getContext());
        this.setDataUuid(vo.getDataUuid());
        this.setEmergencyLevel(vo.getEmergencyLevel());
        this.setLabels(vo.getLabels());
        this.setMetricName(vo.getMetricName());
        this.setMetricValue(vo.getMetricValue());
        this.setNamespace(vo.getNamespace());
        this.setPeriod(vo.getPeriod());
        this.setReadStatus(vo.getReadStatus());
        this.setOperatorAccountUuid(vo.getOperatorAccountUuid());
        this.setResourceUuid(vo.getResourceUuid());
        this.setThreshold(vo.getThreshold());
    }

    public static AlarmRecordsInventory valueOf(AlarmRecordsVO vo) {
        return new AlarmRecordsInventory(vo);
    }

    public static List<AlarmRecordsInventory> valueOf1(Collection<AlarmRecordsVO> vos) {
        List<AlarmRecordsInventory> invs = new ArrayList<AlarmRecordsInventory>(vos.size());
        for (AlarmRecordsVO vo : vos) {
            invs.add(AlarmRecordsInventory.valueOf(vo));
        }
        return invs;
    }

    public AlarmRecordsInventory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long $paramName) {
        id = $paramName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long $paramName) {
        createTime = $paramName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String $paramName) {
        accountUuid = $paramName;
    }

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String $paramName) {
        alarmName = $paramName;
    }

    public String getAlarmStatus() {
        return alarmStatus;
    }

    public void setAlarmStatus(String $paramName) {
        alarmStatus = $paramName;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String $paramName) {
        alarmUuid = $paramName;
    }

    public String getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(String $paramName) {
        comparisonOperator = $paramName;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String $paramName) {
        context = $paramName;
    }

    public String getDataUuid() {
        return dataUuid;
    }

    public void setDataUuid(String $paramName) {
        dataUuid = $paramName;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String $paramName) {
        emergencyLevel = $paramName;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String $paramName) {
        labels = $paramName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String $paramName) {
        metricName = $paramName;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double $paramName) {
        metricValue = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int $paramName) {
        period = $paramName;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean $paramName) {
        readStatus = $paramName;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String $paramName) {
        resourceUuid = $paramName;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double $paramName) {
        threshold = $paramName;
    }

    public static AlarmData toAlarmData(AlarmRecordsVO vo) {
        AlarmData data = new AlarmDataV2();
        data.setAccountUuid(vo.getAccountUuid());
        data.setAlarmName(vo.getAlarmName());
        data.setAlarmStatus(vo.getAlarmStatus());
        data.setAlarmUuid(vo.getAlarmUuid());
        data.setComparisonOperator(vo.getComparisonOperator());
        data.setContext(vo.getContext());
        data.setDataUuid(vo.getDataUuid());
        data.setEmergencyLevel(vo.getEmergencyLevel());
        data.setLabels(vo.getLabels());
        data.setMetricName(vo.getMetricName());
        data.setMetricValue(vo.getMetricValue());
        data.setNamespace(vo.getNamespace());
        data.setReadStatus(vo.getReadStatus() ? "Read": "Unread");
        data.setPeriod(vo.getPeriod());
        data.setAlarmUuid(vo.getAlarmUuid());
        data.setResourceType(vo.getResourceType());
        data.setResourceUuid(vo.getResourceUuid());
        data.setThreshold(vo.getThreshold());
        data.setTime(vo.getCreateTime());
        return data;
    }
}
