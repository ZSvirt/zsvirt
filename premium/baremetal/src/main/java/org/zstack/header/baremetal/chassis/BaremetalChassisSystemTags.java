package org.zstack.header.baremetal.chassis;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:56 2022/12/30
 */
@TagDefinition
public class BaremetalChassisSystemTags {
    public static final String LEGACY_BOOT_TOKEN = "legacyBoot";
    public static PatternedSystemTag LEGACY_BOOT = new PatternedSystemTag(LEGACY_BOOT_TOKEN, BaremetalChassisVO.class);
}
