package org.zstack.storage.primary.nfs;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/4/10.
 */
@TagDefinition
public class NfsSystemTags {
    public static String MOUNT_OPTIONS_TOKEN = "options";
    public static PatternedSystemTag MOUNT_OPTIONS = new PatternedSystemTag(String.format("nfs::mount::options::{%s}", MOUNT_OPTIONS_TOKEN), PrimaryStorageVO.class);
}
