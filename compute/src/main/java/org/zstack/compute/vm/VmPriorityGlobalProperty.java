package org.zstack.compute.vm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * @ Author : yh.w
 * @ Date   : Created in 14:22 2019/9/24
 */
@GlobalPropertyDefinition
public class VmPriorityGlobalProperty {

    @GlobalProperty(name="initRunningVmPriority", defaultValue = "false")
    public static boolean initRunningVmPriority;
    @GlobalProperty(name="initRunningApplianceVmPriority", defaultValue = "false")
    public static boolean initRunningApplianceVmPriority;
}
