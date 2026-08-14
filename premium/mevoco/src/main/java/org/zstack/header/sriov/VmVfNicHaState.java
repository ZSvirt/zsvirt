package org.zstack.header.sriov;

public enum VmVfNicHaState {
    Disabled,   // initial state, vNIC is not created
    Disconnecting,  // old VF is being detached from VM
    Reconnecting,   // new VF is being attached to VM
    Enabled;    // normal state, vNIC is created and attached to VM

    public static Boolean isValid(String value) {
        if (value == null) {
            return false;
        }

        try {
            VmVfNicHaState.valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
