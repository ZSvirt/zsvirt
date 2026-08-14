package org.zstack.monitoring;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/6/16.
 */
@Inventory(mappingVOClass = AlertVO.class)
public class AlertInventory {
    private String uuid;
    private String triggerUuid;
    private String targetResourceUuid;
    private String content;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AlertInventory valueOf(AlertVO vo) {
        AlertInventory inv = new AlertInventory();
        inv.setUuid(vo.getUuid());
        inv.setTriggerUuid(vo.getTriggerUuid());
        inv.setTargetResourceUuid(vo.getTargetResourceUuid());
        inv.setContent(vo.getContent());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AlertInventory> valueOf(Collection<AlertVO> vos)  {
        List<AlertInventory> invs = new ArrayList<>();
        for (AlertVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTriggerUuid() {
        return triggerUuid;
    }

    public void setTriggerUuid(String triggerUuid) {
        this.triggerUuid = triggerUuid;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
