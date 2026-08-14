package org.zstack.snmp.agent;

import org.zstack.core.Platform;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

/**
 * @Author : jingwang
 * @date 2023/8/29 13:27
 */
public class SnmpAgentUtils {
    public static SnmpAgentVO buildSnmpAgentVOFromNewSnmpAgentMessage(NewSnmpAgentMessage msg, String uuid) {
        SnmpAgentVO vo = new SnmpAgentVO();
        vo.setVersion(SnmpAgentVersion.valueOf(msg.getVersion()));
        if (null != msg.getReadCommunity()) {
            vo.setSecurityLevel(SecurityLevel.undefined);
            vo.setReadCommunity(msg.getReadCommunity());
        }
        if (null != msg.getUserName()) {
            vo.setSecurityLevel(SecurityLevel.noAuthNoPriv);
            vo.setUserName(msg.getUserName());
        }
        if (null != msg.getAuthAlgorithm()) {
            vo.setSecurityLevel(SecurityLevel.authNoPriv);
            vo.setAuthAlgorithm(SnmpAgentAuthAlgorithm.valueOf(msg.getAuthAlgorithm()));
        }
        if (null != msg.getAuthPassword()) {
            vo.setAuthPassword(msg.getAuthPassword());
        }
        if (null != msg.getPrivacyAlgorithm()) {
            vo.setSecurityLevel(SecurityLevel.authPriv);
            vo.setPrivacyAlgorithm(SnmpAgentPrivacyAlgorithm.getByName(msg.getPrivacyAlgorithm()));
        }
        if (null != msg.getPrivacyPassword()) {
            vo.setPrivacyPassword(msg.getPrivacyPassword());
        }
        if (null != uuid) {
            vo.setUuid(uuid);
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setStatus(SnmpAgentStatus.Disable);
        vo.setPort(msg.getPort());
        return vo;

    }
}
