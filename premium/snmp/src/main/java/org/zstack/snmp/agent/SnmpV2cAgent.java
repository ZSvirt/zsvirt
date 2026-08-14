package org.zstack.snmp.agent;

/**
 *
 * @Author : jingwang
 * @create 2023/7/18 11:51 AM
 */
public interface SnmpV2cAgent {
    boolean addReadCommunity(String readCommunity);
    boolean removeReadCommunity(String readCommunity);
    boolean clearReadCommunity();
}
