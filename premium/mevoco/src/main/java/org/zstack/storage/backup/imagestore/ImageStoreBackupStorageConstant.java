package org.zstack.storage.backup.imagestore;

import org.zstack.header.configuration.PythonClass;

@PythonClass
public class ImageStoreBackupStorageConstant {
    public static final String SERVICE_ID = "storage.backup.imagestore";

    @PythonClass
    public static final String IMAGE_STORE_BACKUP_STORAGE_TYPE = "ImageStoreBackupStorage";
    public static final String ECHO_PATH = "/imagestore/echo/";
    public static final String PING_PATH = "/imagestore/ping/";
    public static final String EXPORT_IMAGE_PATH = "/imagestore/export/";
    public static final String EXPORT_IMAGE_AS_REMOTE_TARGET_PATH = "/imagestore/export/remotetarget/";
    public static final String DELETE_REMOTE_TARGET_PATH =  "/imagestore/remotetarget/delete/";
    public static final String PACKAGE_EXPORTED_IMAGES_PATH = "/imagestore/pack-exported-images/";
    public static final String DELETE_IMAGE_PACKAGE = "/imagestore/delete-image-package";
    public static final String DELEXP_IMAGE_PATH = "/imagestore/delexp/";
    public static final String GET_DOWNLOAD_PROGRESS_PATH = "/imagestore/progress/";
    public static final String RUNGC_IMAGE_PATH = "/imagestore/gc/";
    public static final String CONNECT_PATH = "/imagestore/connect/";
    public static final String DOWNLOAD_IMAGE_PATH = "/imagestore/import/";
    public static final String CANCEL_DOWNLOAD_IMAGE_PATH = "/imagestore/import/cancel/";
    public static final String IMPORT_BACKUP_PATH = "/imagestore/import-backup/";
    public static final String INSTALL_PATH_ALLOCATE = "/imagestore/installpath/allocate";
    public static final String UPLOAD_IMAGE_TO_REMOTE_PATH = "/imagestore/remote/upload/";
    public static final String DELETE_IMAGE_PATH = "/imagestore/delete/";
    public static final String GET_IMAGE_INFO = "/imagestore/imginfo/";
    public static final String DEFAULT_ARCH = "amd64";
    public static final String ANSIBLE_PLAYBOOK_NAME = "imagestorebackupstorage.py";
    public static final String ANSIBLE_MODULE_PATH = "ansible/imagestorebackupstorage";
    public static final String GENERATE_IMAGE_METADATA_FILE = "/imagestore/generateimagemetadatafile/";
    public static final String CHECK_IMAGE_METADATA_FILE_EXIST = "/imagestore/checkimagemetadatafileexist/";
    public static final String DUMP_IMAGE_METADATA_TO_FILE = "/imagestore/dumpimagemetadatatofile/";
    public static final String GET_IMAGES_METADATA = "/imagestore/getimagesmetadata/";
    public static final String DELETE_IMAGES_METADATA = "/imagestore/deleteimagesmetadata/";
    public static final String GET_LOCAL_FILE_SIZE = "/imagestore/getlocalfilesize/";
    public static final String ALLOCATE_UPLOAD_DIR = "/imagestore/allocuploaddir/";
    public static final String EXPORT_NBD_IMAGE =  "/imagestore/exportnbdimage/";
    public static final String CANCEL_EXPORT_NBD_IMAGE =  "/imagestore/exportnbdimage/cancel";
    public static final String Get_IMAGE_CHAIN_INFO = "/imagestore/imgchain/";
    public static final String SET_STORAGE_QUOTA = "/imagestore/setquota/";
    public static final String ARCHIVE_STORAGE_DATA = "/imagestore/archivedata/";
    public static final String UNPACK_STORAGE_DATA = "/imagestore/unpackdata/";
    public static final String SYNC_STORAGE_DATA = "/imagestore/syncdata/";
    public static final String GET_IMAGE_HASH = "/imagestore/getimagehash/";

    public static final String FILE_DOWNLOAD_PATH = "/imagestore/file/download";
    public static final String FILE_UPLOAD_PATH = "/imagestore/file/upload";
    public static final String FILE_DOWNLOAD_PROGRESS_PATH = "/imagestore/file/progress";
    public static final String FILE_CANCEL_PATH = "/imagestore/file/cancel";
    public static final String DELETE_FILES_PATH = "/imagestore/files/delete";
    public static final String UNZIP_FILE_PATH = "/imagestore/file/unzip";
    public static final String SOFTWARE_UPGRADE_PACKAGE_DEPLOY_PATH = "/imagestore/upgrade/deploy";

    public static final String SYNC_PING_PATH = "/imagestore/ping-sync/";
    public static final String SYNC_DELETE_IMAGE_PATH = "/imagestore/delete-sync/";
    public static final String SYNC_GET_IMAGE_INFO = "/imagestore/imginfo-sync/";
    public static final String SYNC_GENERATE_IMAGE_METADATA_FILE = "/imagestore/generateimagemetadatafile-sync/";
    public static final String SYNC_CHECK_IMAGE_METADATA_FILE_EXIST = "/imagestore/checkimagemetadatafileexist-sync/";
    public static final String SYNC_DUMP_IMAGE_METADATA_TO_FILE = "/imagestore/dumpimagemetadatatofile-sync/";
    public static final String SYNC_EXPORT_IMAGE_PATH = "/imagestore/export-sync/";
    public static final String SYNC_DELEXP_IMAGE_PATH = "/imagestore/delexp-sync/";
    public static final String SYNC_RUNGC_IMAGE_PATH = "/imagestore/gc-sync/";
    public static final String SYNC_GET_LOCAL_CACHE_DIR = "/imagestore/getlocalcachedir-sync/";
    public static final String SYNC_CANCEL_JOB = "/imagestore/canceljob-sync/";
    public static final String SYNC_SET_STORAGE_QUOTA = "/imagestore/setquota-sync/";

    public static final String RESPONSE_TASK_ID_STR = "X-Sync-Task-ID";
    public static final String RESPONSE_TASK_EXISTED_FLAG = "existed";
    public static final long RESPONSE_TASK_RESERVE_TIME = 60 * 1000L;
    public static final long RESPONSE_TASK_RUN_INTERVAL = 5 * 60L;

    public static final String CEPH_IMAGESTORE_COMMIT_PATH = "/ceph/primarystorage/imagestore/backupstorage/commit";
    public static final String CEPH_IMAGESTORE_DOWNLOAD_PATH = "/ceph/primarystorage/imagestore/backupstorage/download";
    public static final String CLEAN_IMAGESTORE_LOCAL_CACHE = "/host/imagestore/cleancache";

    public static final String IPTABLES_COMMENTS = "zstore.allow.port";

    public static final String VOLUME_BACKUP_PACKAGE_NAME = "org.zstack.header.storage.volume.backup";
}
