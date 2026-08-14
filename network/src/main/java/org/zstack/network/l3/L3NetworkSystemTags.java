package org.zstack.network.l3;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class L3NetworkSystemTags {
    // NOTE(WeiW): specify the router interface ip in guest l3 network, if admin dont want it same as network gateway
    public static String ROUTER_INTERFACE_IP_TOKEN = "routerInterfaceIp";
    public static PatternedSystemTag ROUTER_INTERFACE_IP = new PatternedSystemTag(String.format("routerInterfaceIp::{%s}", ROUTER_INTERFACE_IP_TOKEN), L3NetworkVO.class);

    public static String NETWORK_SERVICE_TYPE_TOKEN = "networkservices";
    public static PatternedSystemTag NETWORK_SERVICE_TYPE = new PatternedSystemTag(String.format("networkservices::{%s}", NETWORK_SERVICE_TYPE_TOKEN), L3NetworkVO.class);

    public static String PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN = "dhcpServerUuid";
    public static PatternedSystemTag PUBLIC_NETWORK_DHCP_SERVER_UUID = new PatternedSystemTag(String.format("dhcpServerUuid::{%s}", PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN), L3NetworkVO.class);

    public static String NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN = "ascDelayNormalNextIpRangeUuid";
    public static String NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN = "ascDelayNormalNextIp";
    public static PatternedSystemTag NETWORK_ASC_DELAY_NORMAL_NEXT_IP = new PatternedSystemTag(String.format("ascDelayNormalNextIp::{%s}::{%s}", NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN, NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN), L3NetworkVO.class);

    public static String NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN = "ascDelayAddressPoolNextIpRangeUuid";
    public static String NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN = "ascDelayAddressPoolNextIp";
    public static PatternedSystemTag NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP = new PatternedSystemTag(String.format("ascDelayAddressPoolNextIp::{%s}::{%s}", NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN, NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN), L3NetworkVO.class);

    public static String ENABLE_DHCP_TOKEN = "enableDHCP";

    public static PatternedSystemTag ENABLE_DHCP = new PatternedSystemTag(String.format("enableDHCP::{%s}",ENABLE_DHCP_TOKEN), L3NetworkVO.class);
}
