package org.zstack.network.l3;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class L3NetworkSystemTags {
    // NOTE(WeiW): specify the router interface ip in guest l3 network, if admin dont want it same as network gateway
    public static String ROUTER_INTERFACE_IP_TOKEN = "routerInterfaceIp";
    public static SystemTag ROUTER_INTERFACE_IP = new SystemTag(String.format("routerInterfaceIp::{%s}", ROUTER_INTERFACE_IP_TOKEN), L3NetworkVO.class);

    public static String NETWORK_SERVICE_TYPE_TOKEN = "networkservices";
    public static SystemTag NETWORK_SERVICE_TYPE = new SystemTag(String.format("networkservices::{%s}", NETWORK_SERVICE_TYPE_TOKEN), L3NetworkVO.class);

    public static String PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN = "dhcpServerUuid";
    public static SystemTag PUBLIC_NETWORK_DHCP_SERVER_UUID = new SystemTag(String.format("dhcpServerUuid::{%s}", PUBLIC_NETWORK_DHCP_SERVER_UUID_TOKEN), L3NetworkVO.class);

    public static String NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN = "ascDelayNormalNextIpRangeUuid";
    public static String NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN = "ascDelayNormalNextIp";
    public static SystemTag NETWORK_ASC_DELAY_NORMAL_NEXT_IP = new SystemTag(String.format("ascDelayNormalNextIp::{%s}::{%s}", NETWORK_ASC_DELAY_NORMAL_NEXT_IPRANGE_UUID_TOKEN, NETWORK_ASC_DELAY_NORMAL_NEXT_IP_TOKEN), L3NetworkVO.class);

    public static String NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN = "ascDelayAddressPoolNextIpRangeUuid";
    public static String NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN = "ascDelayAddressPoolNextIp";
    public static SystemTag NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP = new SystemTag(String.format("ascDelayAddressPoolNextIp::{%s}::{%s}", NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IPRANGE_UUID_TOKEN, NETWORK_ASC_DELAY_ADDRESS_POOL_NEXT_IP_TOKEN), L3NetworkVO.class);

    public static String ENABLE_DHCP_TOKEN = "enableDHCP";

    public static SystemTag ENABLE_DHCP = new SystemTag(String.format("enableDHCP::{%s}",ENABLE_DHCP_TOKEN), L3NetworkVO.class);
}
