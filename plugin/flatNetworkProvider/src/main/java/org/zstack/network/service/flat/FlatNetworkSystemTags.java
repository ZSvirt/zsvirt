package org.zstack.network.service.flat;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.network.service.vip.VipVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 9/20/2015.
 */
@TagDefinition
public class FlatNetworkSystemTags {
    public static final String L3_NETWORK_DHCP_IP_TOKEN = "ip";
    public static final String L3_NETWORK_DHCP_IP_UUID_TOKEN = "ipUuid";
    public static PatternedSystemTag L3_NETWORK_DHCP_IP = new PatternedSystemTag(
            String.format("flatNetwork::DhcpServer::{%s}::ipUuid::{%s}", L3_NETWORK_DHCP_IP_TOKEN, L3_NETWORK_DHCP_IP_UUID_TOKEN),
            L3NetworkVO.class
    );

    public static final String VIP_NETWORK_GUEST_NAMESPACE_TOKEN = "namespace";
    public static final String VIP_NETWORK_GUEST_L3_TOKEN = "l3";
    public static PatternedSystemTag VIP = new PatternedSystemTag(
            String.format("flatNetwork::vip::{%s}::{%s}", VIP_NETWORK_GUEST_L3_TOKEN, VIP_NETWORK_GUEST_NAMESPACE_TOKEN),
            VipVO.class
    );
}
