package org.zstack.snmp.agent;

/**
 *
 * @Author : jingwang
 * @create 2023/8/29 11:45
 */
public interface NewSnmpAgentMessage {
    String getVersion();
    String getReadCommunity();
    String getUserName();
    String getAuthAlgorithm();
    String getAuthPassword();
    String getPrivacyAlgorithm();
    String getPrivacyPassword();
    int getPort();
}
