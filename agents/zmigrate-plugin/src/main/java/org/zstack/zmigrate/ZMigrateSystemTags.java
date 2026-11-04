package org.zstack.zmigrate;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class ZMigrateSystemTags {
    public static SystemTag ZMIGRATE_MANAGEMENT = new SystemTag("ZMigrateManagementNodeVm", VmInstanceVO.class);
    public static SystemTag ZMIGRATE_GATEWAY = new SystemTag("ZMigrateGatewayVm", VmInstanceVO.class);

    public static final String ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN = "uploadTaskUuid";
    public static SystemTag ZMIGRATE_VDDK_UPLOADED = new SystemTag(
            String.format("ZMigrateVddkUploaded::{%s}", ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN), VmInstanceVO.class);
    public static SystemTag ZMIGRATE_VDDK_INSTALLED = new SystemTag(
            String.format("ZMigrateVddkInstalled::{%s}", ZMIGRATE_VDDK_UPLOAD_TASK_UUID_TOKEN), VmInstanceVO.class);

    public static final String ZMIGRATE_GATEWAY_IMAGE_TOKEN = "ZMigrateGatewayImage";
    public static SystemTag ZMIGRATE_GATEWAY_IMAGE =
            new SystemTag(String.format("ZMigrateGatewayImage::{%s}", ZMIGRATE_GATEWAY_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN = "ZMigrateLinuxBootImage";
    public static SystemTag ZMIGRATE_LINUX_BOOT_IMAGE =
            new SystemTag(String.format("ZMigrateLinuxBootImage::{%s}", ZMIGRATE_LINUX_BOOT_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN = "ZMigrateWindowsBootImage";
    public static SystemTag ZMIGRATE_WINDOWS_BOOT_IMAGE =
            new SystemTag(String.format("ZMigrateWindowsBootImage::{%s}", ZMIGRATE_WINDOWS_BOOT_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN = "ZMigrateGatewayUpgradeImage";
    public static SystemTag ZMIGRATE_GATEWAY_UPGRADE_IMAGE =
            new SystemTag(String.format("ZMigrateGatewayUpgradeImage::{%s}", ZMIGRATE_GATEWAY_UPGRADE_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN = "ZMigrateLinuxBootUpgradeImage";
    public static SystemTag ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE =
            new SystemTag(String.format("ZMigrateLinuxBootUpgradeImage::{%s}", ZMIGRATE_LINUX_BOOT_UPGRADE_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN = "ZMigrateWindowsBootUpgradeImage";
    public static SystemTag ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE =
            new SystemTag(String.format("ZMigrateWindowsBootUpgradeImage::{%s}", ZMIGRATE_WINDOWS_BOOT_UPGRADE_IMAGE_TOKEN), SoftwarePackageVO.class);

    public static final String ZMIGRATE_LICENSE_KEY_TOKEN = "ZMigrateLicenseKey";
    public static SystemTag ZMIGRATE_LICENSE_KEY =
            new SystemTag(String.format("ZMigrateLicenseKey::{%s}", ZMIGRATE_LICENSE_KEY_TOKEN), SoftwarePackageVO.class);
}
