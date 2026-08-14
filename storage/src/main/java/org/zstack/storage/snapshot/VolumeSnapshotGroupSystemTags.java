package org.zstack.storage.snapshot;

import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

@TagDefinition
public class VolumeSnapshotGroupSystemTags {
    public static SystemTag VOLUME_SNAPSHOT_GROUP_CREATED_BY_SYSTEM =
            new SystemTag("CreatedBySystem", VolumeSnapshotGroupVO.class);
}
