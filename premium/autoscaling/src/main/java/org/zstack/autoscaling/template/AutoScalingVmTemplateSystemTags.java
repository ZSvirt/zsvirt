package org.zstack.autoscaling.template;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Create by weiwang at 2018/8/16
 */
@TagDefinition
public class AutoScalingVmTemplateSystemTags {
    public static String HOSTNAME_TOKEN = "hostname";
    public static SystemTag HOSTNAME = new SystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), AutoScalingVmTemplateVO.class);

    public static SystemTag WINDOWS_VOLUME_ON_VIRTIO = new SystemTag("windows::virtioVolume", AutoScalingVmTemplateVO.class);

    public static String USERDATA_TOKEN = "userdata";
    public static SystemTag USERDATA = new SystemTag(String.format("userdata::{%s}", USERDATA_TOKEN), AutoScalingVmTemplateVO.class);

    public static String SSHKEY_TOKEN = "sshkey";
    public static SystemTag SSHKEY = new SystemTag(String.format("sshkey::{%s}", SSHKEY_TOKEN), AutoScalingVmTemplateVO.class);

    public static String ROOT_PASSWORD_TOKEN = "rootPassword";
    public static SystemTag ROOT_PASSWORD = new SystemTag(String.format("rootPassword::{%s}", ROOT_PASSWORD_TOKEN), AutoScalingVmTemplateVO.class);

    public static String ISO_DEVICEID_TOKEN = "deviceId";
    public static String ISO_TOKEN = "iso";
    public static SystemTag ISO = new SystemTag(String.format("iso::{%s}::{%s}", ISO_TOKEN, ISO_DEVICEID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String BOOT_ORDER_TOKEN = "bootOrder";
    public static SystemTag BOOT_ORDER = new SystemTag(String.format("bootOrder::{%s}", BOOT_ORDER_TOKEN), AutoScalingVmTemplateVO.class);

    public static String CDROM_BOOT_ONCE_TOKEN = "cdromBootOnce";
    public static SystemTag CDROM_BOOT_ONCE = new SystemTag(String.format("cdromBootOnce::{%s}", CDROM_BOOT_ONCE_TOKEN), AutoScalingVmTemplateVO.class);

    public static String CONSOLE_PASSWORD_TOKEN = "consolePassword";
    public static SystemTag CONSOLE_PASSWORD = new SystemTag(String.format("consolePassword::{%s}",CONSOLE_PASSWORD_TOKEN),AutoScalingVmTemplateVO.class);

    public static String INSTANCEOFFERING_ONLINECHANGE_TOKEN = "instanceOfferingOnliechange";
    public static SystemTag INSTANCEOFFERING_ONLIECHANGE = new SystemTag(String.format("instanceOfferingOnlinechange::{%s}",INSTANCEOFFERING_ONLINECHANGE_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_INJECT_QEMUGA_TOKEN = "qemuga";
    public static SystemTag VM_INJECT_QEMUGA = new SystemTag(String.format("%s", VM_INJECT_QEMUGA_TOKEN), AutoScalingVmTemplateVO.class);

    public static String PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN = "primaryStorageUuidForDataVolume";
    public static SystemTag PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME = new SystemTag(String.format("primaryStorageUuidForDataVolume::{%s}", PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_SYSTEM_SERIAL_NUMBER_TOKEN = "vmSystemSerialNumber";
    public static SystemTag VM_SYSTEM_SERIAL_NUMBER = new SystemTag(String.format("vmSystemSerialNumber::{%s}", VM_SYSTEM_SERIAL_NUMBER_TOKEN), AutoScalingVmTemplateVO.class);

    public static final String LOAD_BALANCER_LISTENER_UUIDS_TOKEN = "loadBalancerListenerUuids";
    public static SystemTag LOAD_BALANCER_LISTENER_UUIDS = new SystemTag(
            String.format("loadBalancerListenerUuids::{%s}", LOAD_BALANCER_LISTENER_UUIDS_TOKEN), AutoScalingVmTemplateVO.class);

    public static final String SECURITY_GROUP_UUID_TOKEN = "securityGroupUuid";
    public static SystemTag SECURITY_GROUP_UUID = new SystemTag(
            String.format("securityGroupUuid::{%s}", SECURITY_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String AFFINITY_GROUP_UUID_TOKEN = "affinityGroupUuid";
    public static SystemTag AFFINITY_GROUP_UUID = new SystemTag(String.format("affinityGroupUuid::{%s}", AFFINITY_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

    public static String VM_SCHEDULING_RULE_GROUP_UUID_TOKEN = "vmSchedulingRuleGroupUuid";
    public static SystemTag VM_SCHEDULING_RULE_GROUP_UUID = new SystemTag(String.format("vmSchedulingRuleGroupUuid::{%s}", VM_SCHEDULING_RULE_GROUP_UUID_TOKEN), AutoScalingVmTemplateVO.class);

}
