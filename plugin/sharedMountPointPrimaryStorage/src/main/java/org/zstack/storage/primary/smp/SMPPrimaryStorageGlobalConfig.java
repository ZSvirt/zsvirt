package org.zstack.storage.primary.smp;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 * Created by AlanJager on 2017/3/14.
 */
@GlobalConfigDefinition
public class SMPPrimaryStorageGlobalConfig {
    public static final String CATEGORY = "sharedMountPointPrimaryStorage";

    @GlobalConfigValidation
    public static GlobalConfig GC_INTERVAL = new GlobalConfig(CATEGORY, "deletion.gcInterval");

    @GlobalConfigValidation(validValues = {"none", "metadata", "falloc", "full"})
    @BindResourceConfig({PrimaryStorageVO.class})
    public static GlobalConfig QCOW2_ALLOCATION = new GlobalConfig(CATEGORY, "qcow2.allocation");
}
