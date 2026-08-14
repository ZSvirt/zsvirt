package org.zstack.autoscaling.template;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Create by weiwang at 2018/8/16
 */
@TagDefinition
public class AutoScalingVmTemplateSystemTags {
    public static String HOSTNAME_TOKEN = "hostname";
    public static PatternedSystemTag HOSTNAME = new PatternedSystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), AutoScalingVmTemplateVO.class);

    public static PatternedSystemTag WINDOWS_VOLUME_ON_VIRTIO = new PatternedSystemTag("windows::virtioVolume", AutoScalingVmTemplateVO.class);

    public static String USERDATA_TOKEN = "userdata";
    public static PatternedSystemTag USERDATA = new PatternedSystemTag(String.format("userdata::{%s}", USERDATA_TOKEN), AutoScalingVmTemplateVO.class);

    public static String SSHKEY_TOKEN = "sshkey";
    public static PatternedSystemTag SSHKEY = new PatternedSystemTag(String.format("sshkey::{%s}", SSHKEY_TOKEN), AutoScalingVmTemplateVO.class);

    public static String ROOT_PASSWORD_TOKEN = "rootPassword";
    public static PatternedSystemTag ROOT_PASSWORD = new PatternedSystemTag(String.format("rootPassword::{%s}", ROOT_PASSWORD_TOKEN), AutoScalingVmTemplateVO.class);

    public static String ISO_DEVICEID_TOKEN = "deviceId";
    public static String ISO_TOKEN = "iso";
    public static PatternedSystemTag ISO = new PatternedSystemTag(String.format("iso::{%s}::{%s}", ISO_TOKEN, ISO_DEVICEID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String BOOT_ORDER_TOKEN = "bootOrder";
    public static PatternedSystemTag BOOT_ORDER = new PatternedSystemTag(String.format("bootOrder::{%s}", BOOT_ORDER_TOKEN), AutoScalingVmTemplateVO.class);

    public static String CDROM_BOOT_ONCE_TOKEN = "cdromBootOnce";
    public static PatternedSystemTag CDROM_BOOT_ONCE = new PatternedSystemTag(String.format("cdromBootOnce::{%s}", CDROM_BOOT_ONCE_TOKEN), AutoScalingVmTemplateVO.class);

    public static String CONSOLE_PASSWORD_TOKEN = "consolePassword";
    public static PatternedSystemTag CONSOLE_PASSWORD = new PatternedSystemTag(String.format("consolePassword::{%s}",CONSOLE_PASSWORD_TOKEN),AutoScalingVmTemplateVO.class);

    public static String INSTANCEOFFERING_ONLINECHANGE_TOKEN = "instanceOfferingOnliechange";
    public static PatternedSystemTag INSTANCEOFFERING_ONLIECHANGE = new PatternedSystemTag(String.format("instanceOfferingOnlinechange::{%s}",INSTANCEOFFERING_ONLINECHANGE_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_INJECT_QEMUGA_TOKEN = "qemuga";
    public static PatternedSystemTag VM_INJECT_QEMUGA = new PatternedSystemTag(String.format("%s", VM_INJECT_QEMUGA_TOKEN), AutoScalingVmTemplateVO.class);

    public static String PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN = "primaryStorageUuidForDataVolume";
    public static PatternedSystemTag PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME = new PatternedSystemTag(String.format("primaryStorageUuidForDataVolume::{%s}", PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_SYSTEM_SERIAL_NUMBER_TOKEN = "vmSystemSerialNumber";
    public static PatternedSystemTag VM_SYSTEM_SERIAL_NUMBER = new PatternedSystemTag(String.format("vmSystemSerialNumber::{%s}", VM_SYSTEM_SERIAL_NUMBER_TOKEN), AutoScalingVmTemplateVO.class);

    public static final String LOAD_BALANCER_LISTENER_UUIDS_TOKEN = "loadBalancerListenerUuids";
    public static PatternedSystemTag LOAD_BALANCER_LISTENER_UUIDS = new PatternedSystemTag(
            String.format("loadBalancerListenerUuids::{%s}", LOAD_BALANCER_LISTENER_UUIDS_TOKEN), AutoScalingVmTemplateVO.class);

    public static final String SECURITY_GROUP_UUID_TOKEN = "securityGroupUuid";
    public static PatternedSystemTag SECURITY_GROUP_UUID = new PatternedSystemTag(
            String.format("securityGroupUuid::{%s}", SECURITY_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String AFFINITY_GROUP_UUID_TOKEN = "affinityGroupUuid";
    public static PatternedSystemTag AFFINITY_GROUP_UUID = new PatternedSystemTag(String.format("affinityGroupUuid::{%s}", AFFINITY_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_SCHEDULING_RULE_GROUP_UUID_TOKEN = "vmSchedulingRuleGroupUuid";
    public static PatternedSystemTag VM_SCHEDULING_RULE_GROUP_UUID = new PatternedSystemTag(String.format("vmSchedulingRuleGroupUuid::{%s}", VM_SCHEDULING_RULE_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

}
