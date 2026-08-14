package org.zstack.storage.device;

import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Create by weiwang at 2018/10/25
 */
@TagDefinition
public class StorageDeviceSsytemTags {
    public static PatternedSystemTag DISABLE_ATTACH_MULTIPATH = new PatternedSystemTag("disableAttachMultipath", ScsiLunVmInstanceRefVO.class);
}
