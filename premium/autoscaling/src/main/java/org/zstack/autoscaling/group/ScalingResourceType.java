package org.zstack.autoscaling.group;

/**
 * Created by lining on 2018/9/10.
 */
public enum ScalingResourceType {
    VmInstance(AutoScalingGroupConstants.SCALING_RESOURCE_TYPE_VM_INSTANCE);

    private String value;

    ScalingResourceType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
