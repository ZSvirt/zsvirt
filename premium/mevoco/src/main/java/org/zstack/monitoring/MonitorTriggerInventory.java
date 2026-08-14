package org.zstack.monitoring;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/6/3.
 */
@Inventory(mappingVOClass = MonitorTriggerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "actionRef", inventoryClass = MonitorTriggerActionRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "triggerUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "action", expandedField = "actionRef.action")
})
public class MonitorTriggerInventory {
    private String name;
    private String uuid;
    private String expression;
    private String recoveryExpression;
    private String description;
    private String status;
    private String state;
    private Integer duration;
    private String targetResourceUuid;
    private Timestamp lastStatusChangeTime;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static MonitorTriggerInventory valueOf(MonitorTriggerVO vo) {
        MonitorTriggerInventory inv = new MonitorTriggerInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setTargetResourceUuid(vo.getTargetResourceUuid());
        inv.setRecoveryExpression(vo.getRecoveryExpression());
        inv.setExpression(vo.getExpression());
        inv.setStatus(vo.getStatus().toString());
        inv.setState(vo.getState().toString());
        inv.setLastStatusChangeTime(vo.getLastStatusChangeTime());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setDuration(vo.getDuration());
        return inv;
    }

    public static List<MonitorTriggerInventory> valueOf(Collection<MonitorTriggerVO> vos) {
        List<MonitorTriggerInventory> invs = new ArrayList<>();
        for (MonitorTriggerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getRecoveryExpression() {
        return recoveryExpression;
    }

    public void setRecoveryExpression(String recoveryExpression) {
        this.recoveryExpression = recoveryExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    public void setLastStatusChangeTime(Timestamp lastStatusChangeTime) {
        this.lastStatusChangeTime = lastStatusChangeTime;
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
