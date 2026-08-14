package org.zstack.storage.migration;

import org.zstack.header.configuration.PythonClass;

/**
 * Created by GuoYi on 8/31/17.
 */
@PythonClass
public interface StorageMigrationConstant {
    String SERVICE_ID = "storage.migration";
    String ACTION_CATEGORY = "storage.migration";

    // VOLUME
    String VOLUME_UUID = "volume_uuid";
    String VOLUME_SIZE = "volume_size";
    String VOLUME_SNAPSHOT_SIZE = "volume_snapshot_size";
    String SRC_PS_UUID = "src_primary_storage_uuid";
    String DST_PS_UUID = "dst_primary_storage_uuid";
    String SRC_VOLUME = "src_volume";
    String SRC_VOLUME_SNAPSHOTS = "src_volume_snapshots";

    String SRC_VOLUME_INSTALL_PATH = "src_volume_install_path";
    String SRC_VOLUME_INSTALL_PATHS = "src_volume_install_paths";
    String SRC_VOLUME_SNAPSHOT_PATHS = "src_volume_snapshot_paths";
    String DST_VOLUME_INSTALL_PATH = "dst_volume_install_path";
    String DST_VOLUME_SNAPSHOT_PATHS = "dst_volume_snapshot_paths";
    String SRC_VOLUME_SNAPSHOTS_SIZE = "src_volume_snapshots_size";
    String INDEPENDENT_PATH = "independent_path";
    String VOLUME_MIGRATED = "volume_migrated";
    String SYSTEM_TAGS = "system_tags";
    String ALLOCATED_INSTALL_URL = "allocated_install_url";

    // IMAGE
    String IMAGE_UUID = "image_uuid";
    String IMAGE_SIZE = "image_size";
    String SRC_BS_UUID = "src_backup_storage_uuid";
    String DST_BS_UUID = "dst_backup_storage_uuid";
    String SRC_IMAGE_INSTALL_PATH = "src_image_install_path";
    String DST_IMAGE_INSTALL_PATH = "dst_image_install_path";

    // IMAGE CACHE TEMPLATE
    String DST_IMAGE_CACHE_TEMPLATE_INSTALL_PATH = "dst_image_cache_template_install_path";

    // CEPH
    String SRC_VOLUME_CEPH_POOL_NAME = "src_volume_ceph_pool_name";
    String DST_VOLUME_CEPH_POOL_NAME = "dst_volume_ceph_pool_name";
    String CEPH_TO_CEPH_MIGRATE_IMAGE_TYPE = "CEPHTOCEPHIMAGE";
    String CEPH_TO_CEPH_MIGRATE_VOLUME_TYPE = "CEPHTOCEPHVOLUME";
    String PREPARE_CEPH_TO_CEPH_MIGRATE_IMAGE_PROGRESS = "10";
    String BEFORE_CEPH_TO_CEPH_MIGRATE_IMAGE_PROGRESS = "20";
    String BEFORE_UPDATE_IMAGEVO_PROGRESS = "80";
    String BEFORE_DELETE_CEPH_IMAGE_PROGRESS = "90";

    // NFS
    String NFS_TO_NFS_MIGRATE_VOLUME_TYPE = "NFSTONFSVOLUME";
    String NFS_TO_NFS_MIGRATE_VOLUME_CHOOSEN_HOST = "NFSTONFSVOLUMECHOOSENHOST";

    // SharedBlock
    // TODO(weiw): this is a hack! should refactoring in the future
    String SHAREDBLOCK_TO_SHAREDBLOCK_MIGRATE_VOLUME_TYPE = "SBLKTOSBLKVOLUME";
    String SHAREDBLOCK_STORAGE_TYPE = "SharedBlock";
    String SHAREDBLOCK_TO_SHAREDBLOCK_COPY_IMAGE_CACHE = "SBLKTOSBLKIMAGECACHE";

    String DISCARD_SOURCE = "discardSource";
    String DISCARD_DESTINATION = "discardDestination";
    String WITH_SNAPSHOTS = "withSnapshots";
    String OBJECTS_TO_TRASH = "trashObjects";

    // LOCAL
    String LOCAL_TO_LOCAL_MIGRATE_VOLUME_TYPE = "LOCALTOLOCALVOLUME";
}
