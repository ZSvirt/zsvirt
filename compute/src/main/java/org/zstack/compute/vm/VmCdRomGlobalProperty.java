package org.zstack.compute.vm;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by lining on 2019/1/6.
 */
@GlobalPropertyDefinition
public class VmCdRomGlobalProperty {
    @GlobalProperty(name="addCdRomToHistoricalVm", defaultValue = "false")
    public static boolean addCdRomToHistoricalVm;

    @GlobalProperty(name = "syncVmIsoSystemTag", defaultValue = "false")
    public static boolean syncVmIsoSystemTag;
}
