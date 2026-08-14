package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum SecurityGroupRuleProtocolType {
    TCP,
    UDP,
    ICMP,
    ALL;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            SecurityGroupRuleProtocolType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getAllProtocol() {
        return String.format("%s/%s/%s/%s", TCP.toString(), UDP.toString(), ICMP.toString(), ALL.toString());
    }
}
