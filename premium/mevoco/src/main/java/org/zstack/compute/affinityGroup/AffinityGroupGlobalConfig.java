package org.zstack.compute.affinityGroup;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.affinitygroup.AffinityGroupConstants;
/**
 * Created by shixin.ruan on 04/24/2018
 */
@GlobalConfigDefinition
public class AffinityGroupGlobalConfig {

    private static final String QUOTA_CATEGORY = "quota";

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig AFFINITYGROUP_NUM = new GlobalConfig(QUOTA_CATEGORY, AffinityGroupConstants.AFFINITYGROUP_NUM);
}
