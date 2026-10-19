package org.zstack.compute.cluster;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class ClusterSystemTags {
    public static SystemTag HOST_RESERVED_CPU_CAPACITY = new SystemTag("host::reservedCpu::{capacity}", ClusterVO.class);
    public static SystemTag HOST_RESERVED_MEMORY_CAPACITY = new SystemTag("host::reservedMemory::{capacity}", ClusterVO.class);
}
