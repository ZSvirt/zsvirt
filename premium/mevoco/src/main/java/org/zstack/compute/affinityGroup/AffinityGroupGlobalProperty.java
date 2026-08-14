package org.zstack.compute.affinityGroup;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by shixin.ruan on 2019/09/21.
 */
@GlobalPropertyDefinition
public class AffinityGroupGlobalProperty {
    @GlobalProperty(name="affinity.group.host.count.all.vms", defaultValue = "false")
    public static boolean AFFINITY_GROUP_HOST_COUNT_ALL_VMS;
}
