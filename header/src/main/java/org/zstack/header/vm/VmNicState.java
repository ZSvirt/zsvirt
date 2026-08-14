package org.zstack.header.vm;

/**
 * Created by boce.wang on 11/23/2022.
 */
public enum VmNicState {
    enable,
    disable;

    public static VmNicState fromState(String state) {
        if (state.equals(VmNicState.enable.toString())) {
            return VmNicState.enable;
        } else if (state.equals(VmNicState.disable.toString())) {
            return VmNicState.disable;
        } else {
            return VmNicState.enable;
        }
    }

}
