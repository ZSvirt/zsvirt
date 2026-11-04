package org.zstack.softwarePackage.compute;

import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.tag.SystemTag;

@TagDefinition
public class SoftwarePackageSystemTags {
    public static final String SOFTWARE_PACKAGE_ID = "softwarePackageId";
    public static SystemTag SOFTWARE_PACKAGE_INFO =
            new SystemTag(String.format("uploadSoftwarePackage::{%s}", SOFTWARE_PACKAGE_ID), LongJobVO.class);

    public static final String UPLOAD_URL_TOKEN = "uploadUrl";
    public static SystemTag UPLOAD_URL =
            new SystemTag(String.format("uploadUrl::{%s}", UPLOAD_URL_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_UPLOAD_CONFIG_TOKEN = "uploadConfig";
    public static SystemTag SOFTWARE_PACKAGE_UPLOAD_CONFIG = new SystemTag(String.format("uploadConfig::{%s}", SOFTWARE_PACKAGE_UPLOAD_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_INSTALL_CONFIG_TOKEN = "installConfig";
    public static SystemTag SOFTWARE_PACKAGE_INSTALL_CONFIG = new SystemTag(String.format("installConfig::{%s}", SOFTWARE_PACKAGE_INSTALL_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_INIT_CONFIG_TOKEN = "initConfig";
    public static SystemTag SOFTWARE_PACKAGE_INIT_CONFIG = new SystemTag(String.format("initConfig::{%s}", SOFTWARE_PACKAGE_INIT_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN = "backupStorageHostUuid";
    public static SystemTag SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID =
            new SystemTag(String.format("backupStorageHostUuid::{%s}", SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN = "backupStorageUuid";
    public static SystemTag SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID =
            new SystemTag(String.format("backupStorageUuid::{%s}", SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN = "upgradePackagePath";
    public static SystemTag SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH =
            new SystemTag(String.format("upgradePackagePath::{%s}", SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN), SoftwarePackageVO.class);
}
