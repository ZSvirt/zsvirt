package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by weiwang on 08/03/2017.
 */
@TagDefinition
public class VxlanSystemTags {
    public static final String VXLAN_POOL_UUID_TOKEN = "poolUuid";
    public static final String CLUSTER_UUID_TOKEN = "clusterUuid";
    public static final String VTEP_CIDR_TOKEN = "vtepCidr";
    public static PatternedSystemTag VXLAN_POOL_CLUSTER_VTEP_CIDR = new PatternedSystemTag(
            String.format("l2NetworkUuid::{%s}::clusterUuid::{%s}::cidr::{%s}",
                    VXLAN_POOL_UUID_TOKEN, CLUSTER_UUID_TOKEN, VTEP_CIDR_TOKEN), L2NetworkVO.class
    );
}
