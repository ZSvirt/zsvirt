package org.zstack.storage.backup.imagestore;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTag;

/**
 * Created by mingjian.deng on 2017/8/30.
 */
@TagDefinition
public class ImageStoreSystemTags {
    public static SystemTag INITIALIZED_FILE = new SystemTag("imagestore::initializedFile", ImageStoreBackupStorageVO.class);

    public static String IMAGESTORE_SYNC_TASK_TOKEN = "taskid";
    public static String IMAGESTORE_SYNC_STATUS_TOKEN = "status";
    public static String IMAGESTORE_SYNC_IMAGE_TOKEN = "image";
    public static String IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN = "dst";
    public static SystemTag SYNC_TASK_STATUS = new SystemTag(String.format(
            "image::{%s}::taskid::{%s}::dst::{%s}::status::{%s}", IMAGESTORE_SYNC_IMAGE_TOKEN, IMAGESTORE_SYNC_TASK_TOKEN,
            IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN, IMAGESTORE_SYNC_STATUS_TOKEN),
            ImageStoreBackupStorageVO.class);
    public static SystemTag SYNC_TASK_STATUS_OLD = new SystemTag(String.format(
            "image::{%s}::taskid::{%s}::status::{%s}", IMAGESTORE_SYNC_IMAGE_TOKEN, IMAGESTORE_SYNC_TASK_TOKEN, IMAGESTORE_SYNC_STATUS_TOKEN),
            ImageStoreBackupStorageVO.class);

    public static String IS_FROM_ALIYUN_TOKEN = "aliyun";
    public static SystemTag IS_FROM_ALIYUN = new SystemTag(String.format("%s", IS_FROM_ALIYUN_TOKEN), ImageStoreBackupStorageVO.class);

    public static String IS_REMOTE_BACKUP_TOKEN = "remotebackup";
    public static SystemTag IS_REMOTE_BACKUP = new SystemTag(String.format("%s", IS_REMOTE_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String ALLOW_BACKUP_TOKEN = "allowbackup";
    public static SystemTag ALLOW_BACKUP = new SystemTag(String.format("%s", ALLOW_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String ONLY_FOR_BACKUP_TOKEN = "onlybackup";
    public static SystemTag ONLY_FOR_BACKUP = new SystemTag(String.format("%s", ONLY_FOR_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String SUPPORT_FUSE_TOKEN = "supportFuse";
    public static SystemTag SUPPORT_FUSE = new SystemTag(String.format("%s", SUPPORT_FUSE_TOKEN), ImageStoreBackupStorageVO.class);

    public static final String BACKUP_CIDR_TOKEN = "backupCidr";
    public static SystemTag BACKUP_CIDR = new SystemTag(
            String.format("backup::network::cidr::{%s}", BACKUP_CIDR_TOKEN), ImageStoreBackupStorageVO.class);

    public static final String SYNC_NETWORK_TOKEN = "syncNetwork";
    public static SystemTag SYNC_NETWORK = new SystemTag(String.format("sync::network::cidr::{%s}", SYNC_NETWORK_TOKEN), BackupStorageVO.class);

    public static String OPTION_TOKEN = "options";
    public static String URL_TOKEN = "url";
    public static String FS_TYPE_TOKEN = "type";
    public static SystemTag STORAGE_INFO = new SystemTag(
            String.format("fsInfo::type::{%s}::url::{%s}::options::{%s}", FS_TYPE_TOKEN, URL_TOKEN, OPTION_TOKEN),
            ImageStoreBackupStorageVO.class);

    public static final String IO_RATE_TOKEN = "ioRate";
    public static SystemTag IO_RATE = new SystemTag(String.format("ioRate::{%s}", IO_RATE_TOKEN), ImageStoreBackupStorageVO.class);
}
