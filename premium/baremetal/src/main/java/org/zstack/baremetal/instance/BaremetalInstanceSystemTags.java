package org.zstack.baremetal.instance;

import org.zstack.header.baremetal.instance.BaremetalInstanceVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by GuoYi on 7/11/18.
 */
@TagDefinition
public class BaremetalInstanceSystemTags {
    public static String STATIC_IP_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String STATIC_IP_TOKEN = "staticIp";
    public static PatternedSystemTag STATIC_IP = new PatternedSystemTag(String.format("staticIp::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, STATIC_IP_TOKEN), BaremetalInstanceVO.class);

    public static final String FORCE_INSTALL_TOKEN = "forceInstall";
    public static PatternedSystemTag FORCE_INSTALL = new PatternedSystemTag(FORCE_INSTALL_TOKEN, BaremetalInstanceVO.class);

    public static String SWITCH_INFO_MAC_TOKEN = "mac";
    public static String SWITCH_INFO_TOKEN = "switchInfo";
    public static PatternedSystemTag SWITCH_INFO = new PatternedSystemTag(String.format("switchInfo::{%s}::{%s}", SWITCH_INFO_MAC_TOKEN, SWITCH_INFO_TOKEN), BaremetalInstanceVO.class);
}
