package org.zstack.network.securitygroup;

/**
 */
public enum SecurityGroupRuleState {
    Enabled,
    Disabled;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            SecurityGroupRuleState.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getAllState() {
        return String.format("%s/%s", Enabled.toString(), Disabled.toString());
    }
}
