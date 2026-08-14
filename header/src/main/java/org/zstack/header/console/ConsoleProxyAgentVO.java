package org.zstack.header.console;

import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/3/15.
 */
@Table
@Entity
@BaseResource
public class ConsoleProxyAgentVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = ManagementNodeVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private String description;

    @Column
    private String managementIp;

    @Column
    private String consoleProxyOverriddenIp;

    @Column
    private Integer consoleProxyPort;

    @Column
    private String type;

    @Column
    @Enumerated(EnumType.STRING)
    private ConsoleProxyAgentStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private ConsoleProxyAgentState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getConsoleProxyOverriddenIp() {
        return consoleProxyOverriddenIp;
    }

    public void setConsoleProxyOverriddenIp(String consoleProxyOverriddenIp) {
        this.consoleProxyOverriddenIp = consoleProxyOverriddenIp;
    }

    public Integer getConsoleProxyPort() {
        return consoleProxyPort;
    }

    public void setConsoleProxyPort(Integer consoleProxyPort) {
        this.consoleProxyPort = consoleProxyPort;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ConsoleProxyAgentStatus getStatus() {
        return status;
    }

    public void setStatus(ConsoleProxyAgentStatus status) {
        this.status = status;
    }

    public ConsoleProxyAgentState getState() {
        return state;
    }

    public void setState(ConsoleProxyAgentState state) {
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
}
