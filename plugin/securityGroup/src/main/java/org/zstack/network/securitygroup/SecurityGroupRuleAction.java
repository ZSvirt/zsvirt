package org.zstack.network.securitygroup;

public enum SecurityGroupRuleAction {
    DROP, ACCEPT;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            SecurityGroupRuleAction.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String getAllAction() {
        return String.format("%s/%s", DROP.toString(), ACCEPT.toString());
    }
}
