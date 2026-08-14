package org.zstack.header.bonding;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class CreateBondingMsg extends NeedReplyMessage {
    private String hostUuid;

    private String bondingName;

    private List<String> slaveUuids;

    private String type = "LinuxBonding";

    private String mode = "active-backup";

    private String xmitHashPolicy;

    private String accountUuid;

    private String description;

    private boolean dbOnly = false;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getBondingName() {
        return bondingName;
    }

    public void setBondingName(String bondingName) {
        this.bondingName = bondingName;
    }

    public List<String> getSlaveUuids() {
        return slaveUuids;
    }

    public void setSlaveUuids(List<String> slaveUuids) {
        this.slaveUuids = slaveUuids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getXmitHashPolicy() {
        return xmitHashPolicy;
    }

    public void setXmitHashPolicy(String xmitHashPolicy) {
        this.xmitHashPolicy = xmitHashPolicy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public boolean isDbOnly() {
        return dbOnly;
    }

    public void setDbOnly(boolean dbOnly) {
        this.dbOnly = dbOnly;
    }
}
