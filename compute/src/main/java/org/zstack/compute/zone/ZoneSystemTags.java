package org.zstack.compute.zone;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class ZoneSystemTags {
    public static PatternedSystemTag HOST_RESERVED_CPU_CAPACITY = new PatternedSystemTag("host::reservedCpu::{capacity}", ZoneVO.class);
    public static PatternedSystemTag HOST_RESERVED_MEMORY_CAPACITY = new PatternedSystemTag("host::reservedMemory::{capacity}", ZoneVO.class);
}
