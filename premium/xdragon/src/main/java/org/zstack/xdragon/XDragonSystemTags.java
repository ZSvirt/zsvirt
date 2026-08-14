package org.zstack.xdragon;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class XDragonSystemTags {
    public static String CLUSTER_BRIDGE_MODE_TOKEN = "mode";
    public static PatternedSystemTag CLUSTER_BRIDGE_MODE = new PatternedSystemTag(
            String.format("cluster::bridge::{%s}", CLUSTER_BRIDGE_MODE_TOKEN),
            ClusterVO.class);
}
