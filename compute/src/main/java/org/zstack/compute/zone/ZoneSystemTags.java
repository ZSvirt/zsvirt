package org.zstack.compute.zone;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class ZoneSystemTags {
    public static SystemTag HOST_RESERVED_CPU_CAPACITY = new SystemTag("host::reservedCpu::{capacity}", ZoneVO.class);
    public static SystemTag HOST_RESERVED_MEMORY_CAPACITY = new SystemTag("host::reservedMemory::{capacity}", ZoneVO.class);
}
