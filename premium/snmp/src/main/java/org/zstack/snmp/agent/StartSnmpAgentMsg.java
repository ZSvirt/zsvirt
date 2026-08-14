package org.zstack.snmp.agent;

import org.zstack.snmp.SnmpAgentInnerMessage;

/**
 * @Author : jingwang
 * @create 2023/7/20 10:51 AM
 */
public class StartSnmpAgentMsg extends SnmpAgentInnerMessage {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
