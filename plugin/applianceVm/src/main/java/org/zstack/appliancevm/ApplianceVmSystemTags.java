package org.zstack.appliancevm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by shixin on 2019/06/11
 */
@TagDefinition
public class ApplianceVmSystemTags {
    public static String APPLIANCEVM_HA_UUID_TOKEN = "haUuid";
    public static PatternedSystemTag APPLIANCEVM_HA_UUID =
            new PatternedSystemTag(String.format(
                    "haUuid::{%s}", APPLIANCEVM_HA_UUID_TOKEN),
                    ApplianceVmVO.class);

    public static String APPLIANCEVM_STATIC_VIP_TOKEN = "staticVip";
    public static String APPLIANCEVM_STATIC_VIP_L3_TOKEN = "l3Uuid";
    public static PatternedSystemTag APPLIANCEVM_STATIC_VIP = new PatternedSystemTag(String.format(
            "staticVip::{%s}::{%s}", APPLIANCEVM_STATIC_VIP_L3_TOKEN, APPLIANCEVM_STATIC_VIP_TOKEN), ApplianceVmVO.class);
    public static String APPLIANCEVM_STATIC_IP_TOKEN = "staticIp";
    public static String APPLIANCEVM_STATIC_IP_L3_TOKEN = "l3Uuid";
    public static PatternedSystemTag APPLIANCEVM_STATIC_IP = new PatternedSystemTag(String.format(
            "staticIp::{%s}::{%s}", APPLIANCEVM_STATIC_IP_L3_TOKEN, APPLIANCEVM_STATIC_IP_TOKEN), ApplianceVmVO.class);
}
