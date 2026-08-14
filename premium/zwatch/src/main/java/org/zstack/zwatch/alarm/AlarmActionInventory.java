package org.zstack.zwatch.alarm;

import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.alarm.sns.SNSActionFactory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = AlarmActionVO.class)
public class AlarmActionInventory {
    private String alarmUuid;
    private String actionType;
    private String actionUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AlarmActionInventory __example__() {
        AlarmActionInventory ret = new AlarmActionInventory();
        ret.alarmUuid = DocUtils.createFixedUuid(AlarmVO.class);
        ret.actionType = SNSActionFactory.type.toString();
        ret.actionUuid = DocUtils.createFixedUuid(AlarmActionVO.class);
        ret.lastOpDate = DocUtils.timestamp();
        ret.createDate = DocUtils.timestamp();
        return ret;
    }

    public static AlarmActionInventory valueOf(AlarmActionVO vo) {
        AlarmActionInventory inv = new AlarmActionInventory();
        inv.alarmUuid = vo.getAlarmUuid();
        inv.actionType = vo.getActionType();
        inv.actionUuid = vo.getActionUuid();
        inv.lastOpDate = vo.getLastOpDate();
        inv.createDate = vo.getCreateDate();
        return inv;
    }

    public static List<AlarmActionInventory> valueOf(Collection<AlarmActionVO> vos) {
        return vos.stream().map(AlarmActionInventory::valueOf).collect(Collectors.toList());
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String alarmUuid) {
        this.alarmUuid = alarmUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionUuid() {
        return actionUuid;
    }

    public void setActionUuid(String actionUuid) {
        this.actionUuid = actionUuid;
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
}
