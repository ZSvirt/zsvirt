package org.zstack.compute.cluster;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class MiniClusterSystemTags {
    public static final String HOST_NUMBERS_TOKEN = "numberOfHosts";
    public static SystemTag HOST_NUMBERS = new SystemTag(
            String.format("numberOfHosts::{%s}", HOST_NUMBERS_TOKEN),
            ClusterVO.class
    );
}
