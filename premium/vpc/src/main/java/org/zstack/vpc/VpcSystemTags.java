package org.zstack.vpc;

import org.zstack.header.tag.TagDefinition;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.tag.SystemTag;

/**
 * Created by weiwang on 07/12/2017
 */
@TagDefinition
public class VpcSystemTags {
    public static String VPC_DISTRIBUTED_ROUTING_ENABLED_TOKEN = "drEnabled";
    public static SystemTag VPC_DISTRIBUTED_ROUTING_ENABLED =
            new SystemTag(String.format(
                    "vpcDistributedRoutingEnabled::{%s}", VPC_DISTRIBUTED_ROUTING_ENABLED_TOKEN),
                    VirtualRouterVmVO.class);

    public static String VROUTER_ROUTER_ID_TOKEN = "routerId";
    public static SystemTag VROUTER_ROUTER_ID =
            new SystemTag(String.format(
                    "routeProtocolRouterId::{%s}", VROUTER_ROUTER_ID_TOKEN ),
                    VirtualRouterVmVO.class);

}
