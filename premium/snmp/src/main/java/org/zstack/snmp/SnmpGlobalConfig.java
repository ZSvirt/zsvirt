package org.zstack.snmp;

import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * @Author : jingwang
 * @create 2023/7/20 10:06 AM
 */
@GlobalConfigDefinition
public class SnmpGlobalConfig {
    public static final String CATEGORY = "snmp";

    @GlobalConfigValidation(min = 1, max = 128)
    @GlobalConfigDef(defaultValue = "10", type = Integer.class, description = "SNMP sync level")
    public static PremiumGlobalConfig SNMP_SYNC_LEVEL = new PremiumGlobalConfig(CATEGORY, "snmp.syncLevel");
}
