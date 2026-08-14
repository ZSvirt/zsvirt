package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by GuoYi on 2019-05-07.
 */
@TagDefinition
public class MdevDeviceSystemTags {
    public static final String MDEV_DEVICE_TOKEN = "mdevDevice";
    // only use before vm created
    public static PatternedSystemTag MDEV_DEVICE = new PatternedSystemTag(
            String.format("mdevDevice::{%s}", MDEV_DEVICE_TOKEN), VmInstanceVO.class);

    public static final String MDEV_DEVICE_SPEC_UUID_TOKEN = "mdevSpecUuid";
    public static final String MDEV_DEVICE_NUMBER_TOKEN = "mdevDeviceNumber";
    public static PatternedSystemTag MDEV_DEVICE_SPEC = new PatternedSystemTag(
            String.format("mdevDeviceSpec::{%s}::{%s}", MDEV_DEVICE_SPEC_UUID_TOKEN, MDEV_DEVICE_NUMBER_TOKEN), VmInstanceVO.class
    );
}
