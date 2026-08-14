package org.zstack.header.console;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2016/3/15.
 */
@Inventory(mappingVOClass = ConsoleProxyAgentVO.class)
public class ConsoleProxyAgentInventory {
    private String uuid;
    private String description;
    private String managementIp;
    private String consoleProxyOverriddenIp;
    private Integer consoleProxyPort;
    private String type;
    private String status;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ConsoleProxyAgentInventory valueOf(ConsoleProxyAgentVO vo) {
        ConsoleProxyAgentInventory inv = new ConsoleProxyAgentInventory();
        inv.setUuid(vo.getUuid());
        inv.setType(vo.getType());
        inv.setDescription(vo.getDescription());
        inv.setManagementIp(vo.getManagementIp());
        inv.setConsoleProxyOverriddenIp(vo.getConsoleProxyOverriddenIp());
        inv.setConsoleProxyPort(vo.getConsoleProxyPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setState(vo.getState().toString());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<ConsoleProxyAgentInventory> valueOf(Collection<ConsoleProxyAgentVO> vos) {
        List<ConsoleProxyAgentInventory> invs = new ArrayList<ConsoleProxyAgentInventory>();
        for (ConsoleProxyAgentVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Integer getConsoleProxyPort() {
        return consoleProxyPort;
    }

    public void setConsoleProxyPort(Integer consoleProxyPort) {
        this.consoleProxyPort = consoleProxyPort;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getConsoleProxyOverriddenIp() {
        return consoleProxyOverriddenIp;
    }

    public void setConsoleProxyOverriddenIp(String consoleProxyOverriddenIp) {
        this.consoleProxyOverriddenIp = consoleProxyOverriddenIp;
    }
}
