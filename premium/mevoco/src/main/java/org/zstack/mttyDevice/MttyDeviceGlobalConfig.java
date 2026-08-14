package org.zstack.mttyDevice;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

@GlobalConfigDefinition
public class MttyDeviceGlobalConfig {
    public static final String CATEGORY = "mttyDevice";

    @GlobalConfigValidation(min = 0)
    @GlobalConfigDef(defaultValue = "64", type= Integer.class, description = "limit the number of segmentation security element")
    public static GlobalConfig SE_NUM = new GlobalConfig(CATEGORY, MttyDeviceConstants.SE_NUMBER);
}