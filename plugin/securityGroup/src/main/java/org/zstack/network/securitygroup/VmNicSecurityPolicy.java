package org.zstack.network.securitygroup;

public enum VmNicSecurityPolicy {
    ALLOW,
    DENY;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            VmNicSecurityPolicy.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
