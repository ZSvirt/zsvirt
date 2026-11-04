package org.zstack.storage.device;

import org.zstack.header.storageDevice.ScsiLunVmInstanceRefVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Create by weiwang at 2018/10/25
 */
@TagDefinition
public class StorageDeviceSsytemTags {
    public static SystemTag DISABLE_ATTACH_MULTIPATH = new SystemTag("disableAttachMultipath", ScsiLunVmInstanceRefVO.class);
}
