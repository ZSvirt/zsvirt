package org.zstack.header.storage.backup;

import org.zstack.header.tag.TagDefinition;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 * Created by MaJin on 2019/3/13.
 */
@TagDefinition
public class VolumeBackupSystemTag {
    public static final String API_ID_TOKEN = "apiId";
    public static PatternedSystemTag MANUAL_CREATE_RESOURCE = new PatternedSystemTag(
            String.format("manualCreate::api::{%s}", API_ID_TOKEN),
            VolumeBackupVO.class
    );

    public static final String NO_METADATA_BSUUID_TOKEN = "bsUuid";
    public static PatternedSystemTag NO_METADATA = new PatternedSystemTag(
            String.format("noMetadata::imageStore::{%s}",NO_METADATA_BSUUID_TOKEN),
            VolumeBackupVO.class);

    public static final String DEGREE = "degree";
    public static PatternedSystemTag LIVE_BACKUP_PARALLELISM = new PatternedSystemTag(
            String.format("volumeLiveBackup::parallelismDegree::{%s}", DEGREE),
            ImageStoreBackupStorageVO.class);

}
