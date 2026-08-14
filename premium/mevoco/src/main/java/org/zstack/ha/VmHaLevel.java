package org.zstack.ha;

/**
 * Created by xing5 on 2016/3/28.
 */
public enum VmHaLevel {
    NeverStop,
    OnHostFailure,
    FaultTolerance,
    None,
    /**
     * <p>This is a special value, only used when the VM is first created.
     *
     * <p>Because when the VM was first created, the cluster to be created was not determined,
     * and it is not possible to determine whether the VM has enabled HA
     * based on the configuration of "whether the VM in this cluster has enabled HA"
     * {@link org.zstack.ha.HaGlobalConfig#VM_HA_LEVEL}
     *
     * <p>The value 'Undefined' indicates that the VM needs to determine
     * the assigned cluster before deciding whether to enable HA
     * </p>
     */
    Undefined;

    public static VmHaLevel valueOfOrNull(String text) {
        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isEnabled() {
        return this != None && this != Undefined;
    }

    public boolean isDisabled() {
        return this == None;
    }
}
