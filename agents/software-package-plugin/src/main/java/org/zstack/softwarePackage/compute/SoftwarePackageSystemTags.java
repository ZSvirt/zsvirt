package org.zstack.softwarePackage.compute;

import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class SoftwarePackageSystemTags {
    public static final String SOFTWARE_PACKAGE_ID = "softwarePackageId";
    public static PatternedSystemTag SOFTWARE_PACKAGE_INFO =
            new PatternedSystemTag(String.format("uploadSoftwarePackage::{%s}", SOFTWARE_PACKAGE_ID), LongJobVO.class);

    public static final String UPLOAD_URL_TOKEN = "uploadUrl";
    public static PatternedSystemTag UPLOAD_URL =
            new PatternedSystemTag(String.format("uploadUrl::{%s}", UPLOAD_URL_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_UPLOAD_CONFIG_TOKEN = "uploadConfig";
    public static PatternedSystemTag SOFTWARE_PACKAGE_UPLOAD_CONFIG = new PatternedSystemTag(String.format("uploadConfig::{%s}", SOFTWARE_PACKAGE_UPLOAD_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_INSTALL_CONFIG_TOKEN = "installConfig";
    public static PatternedSystemTag SOFTWARE_PACKAGE_INSTALL_CONFIG = new PatternedSystemTag(String.format("installConfig::{%s}", SOFTWARE_PACKAGE_INSTALL_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_INIT_CONFIG_TOKEN = "initConfig";
    public static PatternedSystemTag SOFTWARE_PACKAGE_INIT_CONFIG = new PatternedSystemTag(String.format("initConfig::{%s}", SOFTWARE_PACKAGE_INIT_CONFIG_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN = "backupStorageHostUuid";
    public static PatternedSystemTag SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID =
            new PatternedSystemTag(String.format("backupStorageHostUuid::{%s}", SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_HOST_UUID_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN = "backupStorageUuid";
    public static PatternedSystemTag SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID =
            new PatternedSystemTag(String.format("backupStorageUuid::{%s}", SOFTWARE_PACKAGE_ON_BACKUP_STORAGE_UUID_TOKEN), SoftwarePackageVO.class);

    public static final String SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN = "upgradePackagePath";
    public static PatternedSystemTag SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH =
            new PatternedSystemTag(String.format("upgradePackagePath::{%s}", SOFTWARE_PACKAGE_UPGRADE_PACKAGE_PATH_TOKEN), SoftwarePackageVO.class);
}
