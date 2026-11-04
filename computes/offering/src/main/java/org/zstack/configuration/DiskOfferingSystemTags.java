package org.zstack.configuration;

import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by lining on 2019/4/23.
 */

@TagDefinition
public class DiskOfferingSystemTags {
    public static String DISK_OFFERING_USER_CONFIG_TOKEN = "diskOfferingUserConfig";
    /**
     * use AllocatorDomainVO
     */
    @Deprecated
    public static SystemTag DISK_OFFERING_USER_CONFIG = new SystemTag(String.format("diskOfferingUserConfig::{%s}", DISK_OFFERING_USER_CONFIG_TOKEN), DiskOfferingVO.class);
}
