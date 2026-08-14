package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.zone.ZoneVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

@TagDefinition
public class VirtualSwitchSystemTags {
    public static String BONDING_MODE_TOKEN = "bondingMode";
    public static String XMIT_HASH_POLICY_TOKEN = "xmitHashPolicy";
    public static PatternedSystemTag UPLINK_BONDING = new PatternedSystemTag(
            String.format("uplink::bonding::{%s}::{%s}", BONDING_MODE_TOKEN, XMIT_HASH_POLICY_TOKEN),
            L2NetworkVO.class);

    @Deprecated
    public static SystemTag L2_DEFAULT_NETWORK = new SystemTag("l2::default", L2NetworkVO.class);

    public static SystemTag VIRTUAL_SWITCH_DEFAULT = new SystemTag("virtualSwitch::default", L2VirtualSwitchNetworkVO.class);

    public static SystemTag PORT_GROUP_DEFAULT = new SystemTag("portGroup::default", PortGroupVO.class);

    public static SystemTag HOST_KERNEL_DEFAULT_INTERFACE = new SystemTag("zskernel::default", HostKernelInterfaceVO.class);

    public static String VIRTUAL_SWITCH_INDEX_TOKEN = "vSwitchIndex";
    public static PatternedSystemTag VIRTUAL_SWITCH_INDEX = new PatternedSystemTag(String.format("virtualSwitch::index::{%s}", VIRTUAL_SWITCH_INDEX_TOKEN),
            ZoneVO.class);
}
