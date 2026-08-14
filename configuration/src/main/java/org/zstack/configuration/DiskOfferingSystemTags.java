package org.zstack.configuration;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by lining on 2019/4/23.
 */

@TagDefinition
public class DiskOfferingSystemTags {
    public static String DISK_OFFERING_USER_CONFIG_TOKEN = "diskOfferingUserConfig";
    public static PatternedSystemTag DISK_OFFERING_USER_CONFIG = new PatternedSystemTag(String.format("diskOfferingUserConfig::{%s}", DISK_OFFERING_USER_CONFIG_TOKEN), DiskOfferingVO.class);
}
