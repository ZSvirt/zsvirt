package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public enum SecurityGroupRuleType {
    Ingress,
    Egress;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            SecurityGroupRuleType.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getAllType() {
        return String.format("%s/%s", Ingress.toString(), Egress.toString());
    }
}
