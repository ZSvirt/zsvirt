package org.zstack.snmp.agent.mib;

import org.snmp4j.agent.mo.MOTable;

/**
 *
 * @Author : jingwang
 * @create 2023/7/28 11:50 AM
 */
public interface MOTableFactory {
    MOTable createMOTable();
    String getType();
    String getOID();
}
