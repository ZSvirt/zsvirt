package org.zstack.pluginpremium.externalapiadapter;

import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.mevoco.PremiumGlobalConfig;

/**
 * Created by lining on 2018/4/20.
 */

@GlobalConfigDefinition
public class ExternalAPIAdapterGlobalConfig {
    public static final String CATEGORY = "externalAPIAdapter";

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static PremiumGlobalConfig ENABLE_EAGLEEYE = new PremiumGlobalConfig(CATEGORY, "enableEagleEye");

    @GlobalConfigValidation(validValues = {"true", "false"})
    public static PremiumGlobalConfig ENABLE_SIGNATURE_CHECKING = new PremiumGlobalConfig(CATEGORY, "enableSignatureChecking");

    @GlobalConfigValidation(notEmpty = false, notNull = false)
    public static PremiumGlobalConfig GPU_SPEC_MAPPING = new PremiumGlobalConfig(CATEGORY, "gpuSpecMapping");
}
