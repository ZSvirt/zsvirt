package org.zstack.compute.cluster;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class ClusterSystemTags {
    public static PatternedSystemTag HOST_RESERVED_CPU_CAPACITY = new PatternedSystemTag("host::reservedCpu::{capacity}", ClusterVO.class);
    public static PatternedSystemTag HOST_RESERVED_MEMORY_CAPACITY = new PatternedSystemTag("host::reservedMemory::{capacity}", ClusterVO.class);
}
