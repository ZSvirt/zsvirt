package org.zstack.storage.snapshot;

import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by Qi Le on 2019-05-17
 */
@TagDefinition
public class VolumeSnapshotSystemTags {
    public static String VOLUMESNAPSHOT_CREATED_BY_SYSTEM_TOKEN = "CreatedBySystem";
    public static SystemTag VOLUMESNAPSHOT_CREATED_BY_SYSTEM =
            new SystemTag(String.format("%s", VOLUMESNAPSHOT_CREATED_BY_SYSTEM_TOKEN), VolumeSnapshotVO.class);

    public static String BACKING_VOLUME_TOKEN = "VolumeUuid";
    public static SystemTag BACKING_TO_VOLUME = new SystemTag(String.format("backingTo::{%s}::volume",
            BACKING_VOLUME_TOKEN), VolumeSnapshotVO.class);

    public static String BACKING_IMAGE_TOKEN = "imageUuid";
    public static SystemTag BACKING_TO_IMAGE = new SystemTag(String.format("backingTo::{%s}::image",
            BACKING_IMAGE_TOKEN), VolumeSnapshotVO.class);
}
