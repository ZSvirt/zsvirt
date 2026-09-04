package org.zstack.loginControl.entity;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccessControlRuleVO.class)
public class AccessControlRuleInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String rule;
    private ControlStrategy strategy;
    private Timestamp createDate;
    private Timestamp lastOpDate;

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

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public ControlStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ControlStrategy strategy) {
        this.strategy = strategy;
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

    public static AccessControlRuleInventory valueOf(AccessControlRuleVO vo) {
        AccessControlRuleInventory inv = new AccessControlRuleInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setRule(vo.getRule());
        inv.setCreateDate(vo.getCreateDate());
        inv.setUuid(vo.getUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setStrategy(vo.getStrategy());

        return inv;
    }

    public static List<AccessControlRuleInventory> valueOf(Collection<AccessControlRuleVO> vos) {
        List<AccessControlRuleInventory> invs = new ArrayList<>(vos.size());
        for (AccessControlRuleVO vo : vos) {
            invs.add(AccessControlRuleInventory.valueOf(vo));
        }
        return invs;
    }

}
