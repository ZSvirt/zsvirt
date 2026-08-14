package org.zstack.configuration;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by lining on 2019/4/23.
 */

@TagDefinition
public class InstanceOfferingSystemTags {
    public static String INSTANCE_OFFERING_USER_CONFIG_TOKEN = "instanceOfferingUserConfig";
    public static PatternedSystemTag INSTANCE_OFFERING_USER_CONFIG = new PatternedSystemTag(String.format("instanceOfferingUserConfig::{%s}", INSTANCE_OFFERING_USER_CONFIG_TOKEN), InstanceOfferingVO.class);
}
