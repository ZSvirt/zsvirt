package org.zstack.snmp.agent;

import org.zstack.snmp.SnmpAgentInnerMessage;

/**
 *
 * @Author : jingwang
 * @create 2023/7/20 10:52 AM
 */
public class UpdateSnmpAgentMsg extends SnmpAgentInnerMessage {
    private SnmpAgentVO agentVO;

    public SnmpAgentVO getAgentVO() {
        return agentVO;
    }

    public void setAgentVO(SnmpAgentVO config) {
        this.agentVO = config;
    }
}
