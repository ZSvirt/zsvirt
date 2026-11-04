package org.zstack.storage.ceph;

import org.zstack.header.core.NonCloneable;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.SystemTag;

/**
 * Created by frank on 8/11/2015.
 */
@TagDefinition
public class CephSystemTags {
    public static SystemTag PREDEFINED_BACKUP_STORAGE_POOL = new SystemTag("ceph::predefinedPool", BackupStorageVO.class);
    public static SystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new SystemTag("ceph::predefinedImageCachePool", PrimaryStorageVO.class);
    public static SystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new SystemTag("ceph::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static SystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new SystemTag("ceph::predefinedDataVolumePool", PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN = "dataVolumePoolName";
    public static SystemTag DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL = new SystemTag(String.format("ceph::default::dataVolumePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN = "rootVolumePoolName";
    public static SystemTag DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new SystemTag(String.format("ceph::default::rootVolumePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN = "imageCachePoolName";
    public static SystemTag DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new SystemTag(String.format("ceph::default::imageCachePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN = "poolName";
    @NonCloneable
    public static SystemTag USE_CEPH_PRIMARY_STORAGE_POOL = new SystemTag(String.format("ceph::pool::{%s}", USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN), VolumeVO.class);

    public static final String KVM_SECRET_UUID_TOKEN = "uuid";
    public static SystemTag KVM_SECRET_UUID = new SystemTag(String.format("ceph::kvm::secret::{%s}", KVM_SECRET_UUID_TOKEN), PrimaryStorageVO.class);

    public static final String NO_CEPHX_TOKEN = "ceph::nocephx";
    public static SystemTag NO_CEPHX = new SystemTag(NO_CEPHX_TOKEN, PrimaryStorageVO.class);

    public static final String CEPH_MANUFACTURER_TOKEN = "name";
    public static SystemTag CEPH_MANUFACTURER = new SystemTag(String.format("ceph::manufacturer::{%s}", CEPH_MANUFACTURER_TOKEN), PrimaryStorageVO.class);

    public static final String USE_CEPH_ROOT_POOL_TOKEN = "rootPoolName";
    @NonCloneable
    public static SystemTag USE_CEPH_ROOT_POOL = new SystemTag(String.format("ceph::rootPoolName::{%s}", USE_CEPH_ROOT_POOL_TOKEN), VolumeVO.class);

    public static final String THIRDPARTY_PLATFORM_TOKEN = "thirdPartyPlatformToken";
    public static SystemTag THIRDPARTY_PLATFORM = new SystemTag(String.format("ceph::thirdPartyPlatform::{%s}", THIRDPARTY_PLATFORM_TOKEN), PrimaryStorageVO.class);

    public static final String CUSTOM_INITIALIZATION_POOL_NAME_TOKEN= "customInitializationPoolNameToken";
    public static SystemTag CUSTOM_INITIALIZATION_POOL_NAME = new SystemTag(String.format("ceph::customInitializationPoolName::{%s}", CUSTOM_INITIALIZATION_POOL_NAME_TOKEN), PrimaryStorageVO.class);
}
