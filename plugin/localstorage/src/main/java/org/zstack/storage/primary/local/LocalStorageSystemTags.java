package org.zstack.storage.primary.local;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/8/22.
 */
@TagDefinition
public class LocalStorageSystemTags {
    public static final String DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN = "hostUuid";
    public static PatternedSystemTag DEST_HOST_FOR_CREATING_DATA_VOLUME = new PatternedSystemTag(
            String.format("localStorage::hostUuid::{%s}", DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN),
            VolumeVO.class
    );

    public static final String LOCALSTORAGE_HOST_INITIALIZED_TOKEN = "primaryStorageUuid";
    public static PatternedSystemTag LOCALSTORAGE_HOST_INITIALIZED = new PatternedSystemTag(String.format(
            "localStorage::{%s}::initialized", LOCALSTORAGE_HOST_INITIALIZED_TOKEN), HostVO.class);
}
