package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.namespace.VmNamespace;
import org.zstack.zwatch.ruleengine.ComparisonOperator;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Inventory(mappingVOClass = AlarmVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "labels", inventoryClass = AlarmLabelInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "alarmUuid"),
        @ExpandedQuery(expandedField = "actions", inventoryClass = AlarmActionInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "alarmUuid")
})
public class AlarmInventory {
    private String uuid;
    private String name;
    private String description;
    private ComparisonOperator comparisonOperator;
    private Integer period;
    private String namespace;
    private String metricName;
    private Double threshold;
    private Integer repeatInterval;
    private Integer repeatCount;
    private AlarmStatus status;
    private AlarmState state;
    private boolean enableRecovery;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<AlarmLabelInventory> labels;
    private List<AlarmActionInventory> actions;
    private String emergencyLevel;

    public static AlarmInventory __example__() {
        AlarmInventory ret = new AlarmInventory();
        ret.uuid = DocUtils.createFixedUuid(AlarmVO.class);
        ret.name = "VM CPU Alarm";
        ret.comparisonOperator = ComparisonOperator.LessThanOrEqualTo;
        ret.state = AlarmState.Enabled;
        ret.status = AlarmStatus.Alarm;
        ret.period = 60;
        ret.namespace = "ZStack/VM";
        ret.metricName = VmNamespace.CPUIdleUtilization.getName();
        ret.threshold = 30D;
        ret.enableRecovery = true;
        ret.repeatInterval = (int)TimeUnit.MINUTES.toSeconds(30);
        ret.createDate = DocUtils.timestamp();
        ret.lastOpDate = DocUtils.timestamp();
        ret.repeatCount = -1;
        ret.labels = asList(AlarmLabelInventory.__example__());
        ret.actions = asList(AlarmActionInventory.__example__());
        return ret;
    }

    public static AlarmInventory valueOf(AlarmVO vo) {
        AlarmInventory inv = new AlarmInventory();
        inv.repeatInterval = vo.getRepeatInterval();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.description = vo.getDescription();
        inv.comparisonOperator = vo.getComparisonOperator();
        inv.period = vo.getPeriod();
        inv.namespace = vo.getNamespace();
        inv.metricName = vo.getMetricName();
        inv.threshold = vo.getThreshold();
        inv.state = vo.getState();
        inv.enableRecovery = vo.isEnableRecovery();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.labels = AlarmLabelInventory.valueOf(vo.getLabels());
        inv.actions = AlarmActionInventory.valueOf(vo.getActions());
        inv.status = vo.getStatus();
        inv.setRepeatCount(vo.getRepeatCount());
        if (vo.getEmergencyLevel() != null) {
            inv.setEmergencyLevel(vo.getEmergencyLevel().name());
        }

        return inv;
    }

    public static List<AlarmInventory> valueOf(Collection<AlarmVO> vos) {
        return vos.stream().map(AlarmInventory::valueOf).collect(Collectors.toList());
    }

    public AlarmState getState() {
        return state;
    }

    public void setState(AlarmState state) {
        this.state = state;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(Integer repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ComparisonOperator getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ComparisonOperator comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public List<AlarmLabelInventory> getLabels() {
        return labels;
    }

    public void setLabels(List<AlarmLabelInventory> labels) {
        this.labels = labels;
    }

    public List<AlarmActionInventory> getActions() {
        return actions;
    }

    public void setActions(List<AlarmActionInventory> actions) {
        this.actions = actions;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public boolean isEnableRecovery() {
        return enableRecovery;
    }

    public void setEnableRecovery(boolean enableRecovery) {
        this.enableRecovery = enableRecovery;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
}

