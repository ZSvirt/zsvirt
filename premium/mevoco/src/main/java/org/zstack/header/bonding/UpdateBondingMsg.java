package org.zstack.header.bonding;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

public class UpdateBondingMsg extends NeedReplyMessage implements BondingMessage {
    private String uuid;
    private List<String> slaveUuids;
    private String type;
    private String mode;
    private String xmitHashPolicy;
    private String description;

    @Override
    public String getBondingUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
}
