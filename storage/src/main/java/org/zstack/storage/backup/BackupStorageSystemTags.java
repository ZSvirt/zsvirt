package org.zstack.storage.backup;

import org.zstack.header.host.HostVO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by mingjian.deng on 2017/9/22.
 */
@TagDefinition
public class BackupStorageSystemTags {
    public static final String BACKUP_STORAGE_DATA_NETWORK_TOKEN = "backupStorageDataNetwork";
    public static SystemTag BACKUP_STORAGE_DATA_NETWORK = new SystemTag(String.format("backupStorage::data::network::cidr::{%s}", BACKUP_STORAGE_DATA_NETWORK_TOKEN), BackupStorageVO.class);

    public static String EXTRA_IPS_TOKEN = "extraips";
    public static SystemTag EXTRA_IPS = new SystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), BackupStorageVO.class);

    public static final String ISCSI_INITIATOR_NAME_TOKEN = "iscsiInitiatorName";
    public static SystemTag ISCSI_INITIATOR_NAME = new SystemTag(String.format("iscsiInitiatorName::{%s}", ISCSI_INITIATOR_NAME_TOKEN), BackupStorageVO.class);
}
