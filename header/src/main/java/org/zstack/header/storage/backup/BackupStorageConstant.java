package org.zstack.header.storage.backup;

public interface BackupStorageConstant {
    public static final String SERVICE_ID = "storage.backup";

    public static final String SCHEME_NFS = "nfs";
    public static final String SCHEME_HTTP = "http";
    public static final String SCHEME_HTTPS = "https";
    public static final String SCHEME_FILE = "file";
    public static final String SCHEME_SFTP = "sftp";
    public static final String SCHEME_FTP = "ftp";

    String EXTERNAL_BACKUP_STORAGE_TYPE = "Addon";

    public static final String DEFAULT_ALLOCATOR_STRATEGY = "defaultAllocatorStrategy";
    public static final BackupStorageAllocatorStrategyType DEFAULT_ALLOCATOR_STRATEGY_TYPE = new BackupStorageAllocatorStrategyType(DEFAULT_ALLOCATOR_STRATEGY);

    public static enum AllocatorParams {
        SPEC,
        CANDIDATES,
    }

    public static final String ACTION_CATEGORY = "backupStorage";

    public static final String IMPORT_IMAGES_FAKE_RESOURCE_UUID = "0a1150d12623319995c9386bf55cb03c";
    public static final String RESTORE_IMAGES_BACKUP_STORAGE_METADATA_TO_DATABASE = "restore_images_backup_storage_metadata_to_database";
}
