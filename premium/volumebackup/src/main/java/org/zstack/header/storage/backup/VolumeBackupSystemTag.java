package org.zstack.header.storage.backup;

import org.zstack.header.tag.TagDefinition;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.tag.SystemTag;

/**
 * Created by MaJin on 2019/3/13.
 */
@TagDefinition
public class VolumeBackupSystemTag {
    public static final String API_ID_TOKEN = "apiId";
    public static SystemTag MANUAL_CREATE_RESOURCE = new SystemTag(
            String.format("manualCreate::api::{%s}", API_ID_TOKEN),
            VolumeBackupVO.class
    );

    public static final String NO_METADATA_BSUUID_TOKEN = "bsUuid";
    public static SystemTag NO_METADATA = new SystemTag(
            String.format("noMetadata::imageStore::{%s}",NO_METADATA_BSUUID_TOKEN),
            VolumeBackupVO.class);

    public static final String DEGREE = "degree";
    public static SystemTag LIVE_BACKUP_PARALLELISM = new SystemTag(
            String.format("volumeLiveBackup::parallelismDegree::{%s}", DEGREE),
            ImageStoreBackupStorageVO.class);

}
