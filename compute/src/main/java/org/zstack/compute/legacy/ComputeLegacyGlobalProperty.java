package org.zstack.compute.legacy;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

@GlobalPropertyDefinition
public class ComputeLegacyGlobalProperty {
    /**
     * true when first boot after upgrade to version ZSphere 4.10.0
     */
    @GlobalProperty(name="legacyCpuTopologyFix", defaultValue = "false")
    public static boolean cpuTopologyFix;
    /**
     * if enableNvRamTypeVolume = true, NvRam type volume will be created if UEFI is enabled;
     * if enableNvRamTypeVolume = false, NvRam type volume will not be created, NvRam & TpmState will save in host
     * (not in Primary storage);
     */
    @GlobalProperty(name="enable.nv.ram.type.volume", defaultValue = "false")
    public static boolean enableNvRamTypeVolume;
}
