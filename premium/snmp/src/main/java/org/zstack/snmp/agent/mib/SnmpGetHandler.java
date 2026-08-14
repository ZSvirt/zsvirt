package org.zstack.snmp.agent.mib;

import org.snmp4j.smi.OctetString;

/**
 *
 * @Author : jingwang
 * @create 2023/8/10 19:23
 */
public interface SnmpGetHandler {
    OctetString handle(String namespace, String metricName, String uuid);
}
