package org.zstack.allocator.domain.header;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class AllocatorDomainGlobalConfig {
    public static final String CATEGORY = "vm";
    public static final String DOMAIN_UUID_NAME = "scheduling.domainUuid";
    public static final String DOMAIN_UUID_IDENTITY = CATEGORY + "." + DOMAIN_UUID_NAME;

    @BindResourceConfig({VmInstanceVO.class})
    @GlobalConfigDef(defaultValue = "None", type = String.class,
            description = "Configured allocator domain uuid for a VM. None means no domain. This is the desired domain, not the currently effective domain. Change it only via resource config; the global value cannot be updated.")
    @GlobalConfigValidation
    public static GlobalConfig SCHEDULING_DOMAIN_UUID = new GlobalConfig(CATEGORY, DOMAIN_UUID_NAME);
}
