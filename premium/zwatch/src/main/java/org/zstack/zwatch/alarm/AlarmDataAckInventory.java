package org.zstack.zwatch.alarm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.zwatch.ZWatchConstants;
import org.zstack.zwatch.ZWatchGlobalConfig;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(
        mappingVOClass = AlarmDataAckVO.class,
        collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = AlertDataAckInventory.class, type = ZWatchConstants.ALARM_DATA_TYPE)}
)
public class AlarmDataAckInventory extends AlertDataAckInventory {
    private String alarmUuid;

    protected AlarmDataAckInventory(AlarmDataAckVO vo) {
        super(vo);
        this.setAlarmUuid(vo.getAlarmUuid());
    }

    public static AlarmDataAckInventory valueOf(AlarmDataAckVO vo) {
        return new AlarmDataAckInventory(vo);
    }

    public static List<AlarmDataAckInventory> valueOf1(Collection<AlarmDataAckVO> vos) {
        List<AlarmDataAckInventory> invs = new ArrayList<AlarmDataAckInventory>(vos.size());
        for (AlarmDataAckVO vo : vos) {
            invs.add(AlarmDataAckInventory.valueOf(vo));
        }
        return invs;
    }

    public AlarmDataAckInventory() {
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String $paramName) {
        alarmUuid = $paramName;
    }
}
