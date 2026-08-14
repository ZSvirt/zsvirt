package org.zstack.compute.vm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.identity.QuotaGlobalConfig;

/**
 * Created by kayo on 2018/4/21.
 */
@GlobalConfigDefinition
public class VmQuotaGlobalConfig extends QuotaGlobalConfig {

    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_TOTAL_NUM = new GlobalConfig(CATEGORY, VmQuotaConstant.VM_TOTAL_NUM);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_RUNNING_NUM = new GlobalConfig(CATEGORY, VmQuotaConstant.VM_RUNNING_NUM);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_RUNNING_MEMORY_SIZE = new GlobalConfig(CATEGORY, VmQuotaConstant.VM_RUNNING_MEMORY_SIZE);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VM_RUNNING_CPU_NUM = new GlobalConfig(CATEGORY, VmQuotaConstant.VM_RUNNING_CPU_NUM);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig DATA_VOLUME_NUM = new GlobalConfig(CATEGORY, VmQuotaConstant.DATA_VOLUME_NUM);
    @GlobalConfigValidation(min = 0)
    public static GlobalConfig VOLUME_SIZE = new GlobalConfig(CATEGORY, VmQuotaConstant.VOLUME_SIZE);
}
