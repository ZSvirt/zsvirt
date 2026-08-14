package org.zstack.network.l3;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmNicVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by boce.wang on 2023/04/23
 */
@TagDefinition
public class VmNicSystemTags {
    public static String VM_NIC_INTERNAL_IP_TOKEN = "internalIp";
    @Deprecated
    public static PatternedSystemTag VM_NIC_INTERNAL_IP =
            new PatternedSystemTag(String.format(
                    "internalIp::{%s}", VM_NIC_INTERNAL_IP_TOKEN),
                    VmNicVO.class);

    public static String VM_NIC_INTERNAL_IPV6_TOKEN = "internalIpv6";
    @Deprecated
    public static PatternedSystemTag VM_NIC_INTERNAL_IPV6 =
            new PatternedSystemTag(String.format(
                    "internalIpv6::{%s}", VM_NIC_INTERNAL_IPV6_TOKEN),
                    VmNicVO.class);
}
