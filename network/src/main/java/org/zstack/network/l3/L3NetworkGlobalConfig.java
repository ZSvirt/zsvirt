package org.zstack.network.l3;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.resourceconfig.BindResourceConfig;

@GlobalConfigDefinition
public class L3NetworkGlobalConfig {
    public static final String CATEGORY = "l3Network";

    @GlobalConfigDef(defaultValue = L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY, type = String.class, description = "ip allocate strategy")
    @GlobalConfigValidation(validValues = {L3NetworkConstant.FIRST_AVAILABLE_IP_ALLOCATOR_STRATEGY, L3NetworkConstant.RANDOM_IP_ALLOCATOR_STRATEGY, L3NetworkConstant.ASC_DELAY_RECYCLE_IP_ALLOCATOR_STRATEGY})
    @BindResourceConfig({L3NetworkVO.class})
    public static GlobalConfig IP_ALLOCATE_STRATEGY = new GlobalConfig(CATEGORY, "ipAllocateStrategy");

    @GlobalConfigDef(defaultValue = L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY, type = String.class, description = "ipv6 allocate strategy")
    @GlobalConfigValidation(validValues = {L3NetworkConstant.FIRST_AVAILABLE_IPV6_ALLOCATOR_STRATEGY, L3NetworkConstant.RANDOM_IPV6_ALLOCATOR_STRATEGY})
    @BindResourceConfig({L3NetworkVO.class})
    public static GlobalConfig IPV6_ALLOCATE_STRATEGY = new GlobalConfig(CATEGORY, "ipv6AllocateStrategy");

    @GlobalConfigValidation
    public static GlobalConfig BASIC_NETWORK_ENABLE_RA = new GlobalConfig(CATEGORY, "basic.network.enable.ra");
}
