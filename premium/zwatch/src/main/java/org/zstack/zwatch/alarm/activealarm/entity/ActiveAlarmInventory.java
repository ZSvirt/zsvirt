package org.zstack.zwatch.alarm.activealarm.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ActiveAlarmVO.class, collectionValueOfMethod = "valueOf1")
public class ActiveAlarmInventory implements Serializable {
    private String templateUuid;
    private String alarmUuid;
    private String namespace;
    private Timestamp createDate;
    private String uuid;

    protected ActiveAlarmInventory(ActiveAlarmVO vo) {
        this.setTemplateUuid(vo.getTemplateUuid());
        this.setAlarmUuid(vo.getAlarmUuid());
        this.setNamespace(vo.getNamespace());
        this.setCreateDate(vo.getCreateDate());
        this.setUuid(vo.getUuid());
    }

    public static ActiveAlarmInventory valueOf(ActiveAlarmVO vo) {
        return new ActiveAlarmInventory(vo);
    }

    public static List<ActiveAlarmInventory> valueOf1(Collection<ActiveAlarmVO> vos) {
        List<ActiveAlarmInventory> invs = new ArrayList<ActiveAlarmInventory>(vos.size());
        for (ActiveAlarmVO vo : vos) {
            invs.add(ActiveAlarmInventory.valueOf(vo));
        }
        return invs;
    }

    public ActiveAlarmInventory() {
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String $paramName) {
        templateUuid = $paramName;
    }

    public String getAlarmUuid() {
        return alarmUuid;
    }

    public void setAlarmUuid(String $paramName) {
        alarmUuid = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
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
