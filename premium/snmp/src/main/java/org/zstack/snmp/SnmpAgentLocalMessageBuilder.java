package org.zstack.snmp;

/**
 * @Author : jingwang
 * @date 2023/8/29 13:44
 */
public interface SnmpAgentLocalMessageBuilder {
    SnmpAgentInnerMessage buildLocalMessage();
}
