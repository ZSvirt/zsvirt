package org.zstack.network.securitygroup;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public enum SecurityGroupState {
    Enabled,
    Disabled;

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            SecurityGroupState.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
