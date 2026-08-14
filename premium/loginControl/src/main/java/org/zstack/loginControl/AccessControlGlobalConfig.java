package org.zstack.loginControl;

import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.header.core.encrypt.GlobalConfigEncrypt;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class AccessControlGlobalConfig {
    public static final String CATEGORY = "accessControl";

    @GlobalConfigEncrypt(category = CATEGORY, name = "enable.request.source.ip.address.check")
    @GlobalConfigDef(defaultValue = "true", type = Boolean.class, description = "Enable request source ip address check")
    public static PremiumGlobalConfig ENABLE_REQUEST_SOURCE_IP_ADDRESS_CHECK = new PremiumGlobalConfig(CATEGORY, "enable.request.source.ip.address.check");

}
