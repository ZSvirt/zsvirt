package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.datatype.Label;
import org.zstack.zwatch.namespace.VmNamespace;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = AlarmLabelVO.class)
public class AlarmLabelInventory {
    private String uuid;
    private String key;
    private String operator;
    private String value;
    @APINoSee
    private String alarmUuid;

    public static AlarmLabelInventory __example__() {
        AlarmLabelInventory ret = new AlarmLabelInventory();
        ret.uuid = DocUtils.createFixedUuid(AlarmLabelVO.class);
        ret.key = VmNamespace.LabelNames.VMUuid.toString();
        ret.operator = Label.Operator.Equal.toString();
        ret.value = "41cf44200832452aaa35cafe5eca1ac1";
        return ret;
    }

    public static AlarmLabelInventory valueOf(AlarmLabelVO vo) {
        AlarmLabelInventory inventory = new AlarmLabelInventory();
        inventory.uuid = vo.getUuid();
        inventory.key = vo.getKey();
        inventory.operator = vo.getOperator().name();
        inventory.value = vo.getValue();
        inventory.alarmUuid = vo.getAlarmUuid();
        return inventory;
    }

    public static List<AlarmLabelInventory> valueOf(Collection<AlarmLabelVO> vos) {
        return vos.stream().map(AlarmLabelInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
