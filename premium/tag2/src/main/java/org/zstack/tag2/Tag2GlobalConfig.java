package org.zstack.tag2;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

@GlobalConfigDefinition
public class Tag2GlobalConfig {
    private static String CATEGORY = "tag2";

    @GlobalConfigValidation(max = 200)
    public static PremiumGlobalConfig ATTACHED_TAG_LIMIT = new PremiumGlobalConfig(CATEGORY, "tag.resource.attached");
}