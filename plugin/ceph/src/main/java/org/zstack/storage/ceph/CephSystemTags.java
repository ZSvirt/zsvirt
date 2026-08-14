package org.zstack.storage.ceph;

import org.zstack.header.core.NonCloneable;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/11/2015.
 */
@TagDefinition
public class CephSystemTags {
    public static PatternedSystemTag PREDEFINED_BACKUP_STORAGE_POOL = new PatternedSystemTag("ceph::predefinedPool", BackupStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag("ceph::predefinedImageCachePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag("ceph::predefinedRootVolumePool", PrimaryStorageVO.class);
    public static PatternedSystemTag PREDEFINED_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag("ceph::predefinedDataVolumePool", PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN = "dataVolumePoolName";
    public static PatternedSystemTag DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL = new PatternedSystemTag(String.format("ceph::default::dataVolumePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_DATA_VOLUME_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN = "rootVolumePoolName";
    public static PatternedSystemTag DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL = new PatternedSystemTag(String.format("ceph::default::rootVolumePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_ROOT_VOLUME_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN = "imageCachePoolName";
    public static PatternedSystemTag DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL = new PatternedSystemTag(String.format("ceph::default::imageCachePoolName::{%s}", DEFAULT_CEPH_PRIMARY_STORAGE_IMAGE_CACHE_POOL_TOKEN), PrimaryStorageVO.class);

    public static final String USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN = "poolName";
    @NonCloneable
    public static PatternedSystemTag USE_CEPH_PRIMARY_STORAGE_POOL = new PatternedSystemTag(String.format("ceph::pool::{%s}", USE_CEPH_PRIMARY_STORAGE_POOL_TOKEN), VolumeVO.class);

    public static final String KVM_SECRET_UUID_TOKEN = "uuid";
    public static PatternedSystemTag KVM_SECRET_UUID = new PatternedSystemTag(String.format("ceph::kvm::secret::{%s}", KVM_SECRET_UUID_TOKEN), PrimaryStorageVO.class);

    public static final String NO_CEPHX_TOKEN = "ceph::nocephx";
    public static PatternedSystemTag NO_CEPHX = new PatternedSystemTag(NO_CEPHX_TOKEN, PrimaryStorageVO.class);

    public static final String CEPH_MANUFACTURER_TOKEN = "name";
    public static PatternedSystemTag CEPH_MANUFACTURER = new PatternedSystemTag(String.format("ceph::manufacturer::{%s}", CEPH_MANUFACTURER_TOKEN), PrimaryStorageVO.class);

    public static final String USE_CEPH_ROOT_POOL_TOKEN = "rootPoolName";
    @NonCloneable
    public static PatternedSystemTag USE_CEPH_ROOT_POOL = new PatternedSystemTag(String.format("ceph::rootPoolName::{%s}", USE_CEPH_ROOT_POOL_TOKEN), VolumeVO.class);

    public static final String THIRDPARTY_PLATFORM_TOKEN = "thirdPartyPlatformToken";
    public static PatternedSystemTag THIRDPARTY_PLATFORM = new PatternedSystemTag(String.format("ceph::thirdPartyPlatform::{%s}", THIRDPARTY_PLATFORM_TOKEN), PrimaryStorageVO.class);

    public static final String CUSTOM_INITIALIZATION_POOL_NAME_TOKEN= "customInitializationPoolNameToken";
    public static PatternedSystemTag CUSTOM_INITIALIZATION_POOL_NAME = new PatternedSystemTag(String.format("ceph::customInitializationPoolName::{%s}", CUSTOM_INITIALIZATION_POOL_NAME_TOKEN), PrimaryStorageVO.class);
}
