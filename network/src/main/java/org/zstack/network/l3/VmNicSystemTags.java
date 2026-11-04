package org.zstack.network.l3;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmNicVO;
import org.zstack.tag.SystemTag;

/**
 * Created by boce.wang on 2023/04/23
 */
@TagDefinition
public class VmNicSystemTags {
    public static String VM_NIC_INTERNAL_IP_TOKEN = "internalIp";
    @Deprecated
    public static SystemTag VM_NIC_INTERNAL_IP =
            new SystemTag(String.format(
                    "internalIp::{%s}", VM_NIC_INTERNAL_IP_TOKEN),
                    VmNicVO.class);

    public static String VM_NIC_INTERNAL_IPV6_TOKEN = "internalIpv6";
    @Deprecated
    public static SystemTag VM_NIC_INTERNAL_IPV6 =
            new SystemTag(String.format(
                    "internalIpv6::{%s}", VM_NIC_INTERNAL_IPV6_TOKEN),
                    VmNicVO.class);
}
