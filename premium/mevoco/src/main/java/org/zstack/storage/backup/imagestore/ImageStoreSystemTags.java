package org.zstack.storage.backup.imagestore;

import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 * Created by mingjian.deng on 2017/8/30.
 */
@TagDefinition
public class ImageStoreSystemTags {
    public static String IMAGESTORE_SYNC_TASK_TOKEN = "taskid";
    public static String IMAGESTORE_SYNC_STATUS_TOKEN = "status";
    public static String IMAGESTORE_SYNC_IMAGE_TOKEN = "image";
    public static String IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN = "dst";
    public static PatternedSystemTag SYNC_TASK_STATUS = new PatternedSystemTag(String.format(
            "image::{%s}::taskid::{%s}::dst::{%s}::status::{%s}", IMAGESTORE_SYNC_IMAGE_TOKEN, IMAGESTORE_SYNC_TASK_TOKEN,
            IMAGESTORE_SYNC_DESTINATE_IMAGE_TOKEN, IMAGESTORE_SYNC_STATUS_TOKEN),
            ImageStoreBackupStorageVO.class);
    public static PatternedSystemTag SYNC_TASK_STATUS_OLD = new PatternedSystemTag(String.format(
            "image::{%s}::taskid::{%s}::status::{%s}", IMAGESTORE_SYNC_IMAGE_TOKEN, IMAGESTORE_SYNC_TASK_TOKEN, IMAGESTORE_SYNC_STATUS_TOKEN),
            ImageStoreBackupStorageVO.class);

    public static String IS_FROM_ALIYUN_TOKEN = "aliyun";
    public static PatternedSystemTag IS_FROM_ALIYUN = new PatternedSystemTag(String.format("%s", IS_FROM_ALIYUN_TOKEN), ImageStoreBackupStorageVO.class);

    public static String IS_REMOTE_BACKUP_TOKEN = "remotebackup";
    public static PatternedSystemTag IS_REMOTE_BACKUP = new PatternedSystemTag(String.format("%s", IS_REMOTE_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String ALLOW_BACKUP_TOKEN = "allowbackup";
    public static PatternedSystemTag ALLOW_BACKUP = new PatternedSystemTag(String.format("%s", ALLOW_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String ONLY_FOR_BACKUP_TOKEN = "onlybackup";
    public static PatternedSystemTag ONLY_FOR_BACKUP = new PatternedSystemTag(String.format("%s", ONLY_FOR_BACKUP_TOKEN), ImageStoreBackupStorageVO.class);

    public static String SUPPORT_FUSE_TOKEN = "supportFuse";
    public static PatternedSystemTag SUPPORT_FUSE = new PatternedSystemTag(String.format("%s", SUPPORT_FUSE_TOKEN), ImageStoreBackupStorageVO.class);

    public static final String BACKUP_CIDR_TOKEN = "backupCidr";
    public static PatternedSystemTag BACKUP_CIDR = new PatternedSystemTag(
            String.format("backup::network::cidr::{%s}", BACKUP_CIDR_TOKEN), ImageStoreBackupStorageVO.class);

    public static final String SYNC_NETWORK_TOKEN = "syncNetwork";
    public static PatternedSystemTag SYNC_NETWORK = new PatternedSystemTag(String.format("sync::network::cidr::{%s}", SYNC_NETWORK_TOKEN), BackupStorageVO.class);

    public static String OPTION_TOKEN = "options";
    public static String URL_TOKEN = "url";
    public static String FS_TYPE_TOKEN = "type";
    public static PatternedSystemTag STORAGE_INFO = new PatternedSystemTag(
            String.format("fsInfo::type::{%s}::url::{%s}::options::{%s}", FS_TYPE_TOKEN, URL_TOKEN, OPTION_TOKEN),
            ImageStoreBackupStorageVO.class);

    public static final String IO_RATE_TOKEN = "ioRate";
    public static PatternedSystemTag IO_RATE = new PatternedSystemTag(String.format("ioRate::{%s}", IO_RATE_TOKEN), ImageStoreBackupStorageVO.class);
}
