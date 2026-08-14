package org.zstack.snmp.agent;

import org.snmp4j.security.UsmUser;

/**
 *
 * @Author : jingwang
 * @create 2023/7/18 11:52 AM
 */
public interface SnmpV3Agent {
    boolean addUsmUser(UsmUser usm);
    boolean removeUsmUser(String userName);
    boolean clearUsmUser();
}
