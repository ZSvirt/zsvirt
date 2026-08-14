package org.zstack.snmp.agent;

import org.zstack.header.message.NeedReplyMessage;

/**
 *
 * @Author : jingwang
 * @create 2023/8/18 10:47
 */
public interface SnmpAgentMessage {
    NeedReplyMessage convertToLocalMessage();
}
