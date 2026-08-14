package org.zstack.cloudformation;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by mingjian.deng on 2019/11/25.
 */
@GlobalConfigDefinition
public class CloudFormationGlobalConfig {
    public static final String CATEGORY = "cloudformation";

    @GlobalConfigValidation(min = 0)
    public static PremiumGlobalConfig VM_PORT_CHECK_INTERVAL = new PremiumGlobalConfig(CATEGORY, "vm.port.check.interval");
}