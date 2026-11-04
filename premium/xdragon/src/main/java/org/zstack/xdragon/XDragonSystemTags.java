package org.zstack.xdragon;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class XDragonSystemTags {
    public static String CLUSTER_BRIDGE_MODE_TOKEN = "mode";
    public static SystemTag CLUSTER_BRIDGE_MODE = new SystemTag(
            String.format("cluster::bridge::{%s}", CLUSTER_BRIDGE_MODE_TOKEN),
            ClusterVO.class);
}
