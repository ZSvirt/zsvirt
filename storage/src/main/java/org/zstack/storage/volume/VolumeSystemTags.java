package org.zstack.storage.volume;

import org.zstack.header.core.NonCloneable;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.SystemTag;

/**
 * Created by miao on 12/23/16.
 */
@TagDefinition
public class VolumeSystemTags {
    public static SystemTag SHAREABLE = SystemTag.makeEphemeralTag("shareable");

    public static String VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN = "volumeSnapshotMaxNum";
    public static SystemTag VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM = new SystemTag(String.format("volumeMaxIncrementalSnapshotNum::{%s}", VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN), VolumeVO.class);

    public static String VOLUME_PROVISIONING_STRATEGY_TOKEN = "volumeProvisioningStrategy";
    public static SystemTag VOLUME_PROVISIONING_STRATEGY = new SystemTag(String.format("volumeProvisioningStrategy::{%s}", VOLUME_PROVISIONING_STRATEGY_TOKEN), VolumeVO.class);

    public static String PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN = "primaryStorageVolumeProvisioningStrategy";
    public static SystemTag PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY = new SystemTag(String.format("primaryStorageVolumeProvisioningStrategy::{%s}", PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN), PrimaryStorageVO.class);

    public static String AUTO_SCALING_GROUP_UUID_TOKEN = "autoScalingGroupUuid";
    public static SystemTag AUTO_SCALING_GROUP_UUID = new SystemTag(String.format("autoScalingGroupUuid::{%s}", AUTO_SCALING_GROUP_UUID_TOKEN), VolumeVO.class);

    @NonCloneable
    public static String OVERWRITED_VOLUME_TOKEN = "overwritedVolumeUuid";
    public static SystemTag OVERWRITED_VOLUME = new SystemTag(String.format("overwriteBy::volume::{%s}", OVERWRITED_VOLUME_TOKEN), VolumeVO.class);

    public static String NOT_SUPPORT_ACTUAL_SIZE_FLAG_TOKEN = "notSupportActualSize";
    public static SystemTag NOT_SUPPORT_ACTUAL_SIZE_FLAG = new SystemTag(String.format("notSupportActualSize::{%s}", NOT_SUPPORT_ACTUAL_SIZE_FLAG_TOKEN), VolumeVO.class);

    public static SystemTag PACKER_BUILD = new SystemTag("packer", VolumeVO.class);

    @NonCloneable
    public static SystemTag FAST_CREATE = SystemTag.makeEphemeralTag("volume::fastCreate");

    @NonCloneable
    public static SystemTag FLATTEN = SystemTag.makeEphemeralTag("volume::flatten");

    @NonCloneable
    public static SystemTag FORMAT_QCOW2 = SystemTag.makeEphemeralTag("volume::format::qcow2");

    @NonCloneable
    public static SystemTag NO_ZERO_FILLED_VOLUME = SystemTag.makeEphemeralTag("volume::noZeroFilled");

    public static String VOLUME_QOS_TOKEN = "qos";
    public static SystemTag VOLUME_QOS = new SystemTag(String.format("%s::{%s}", VOLUME_QOS_TOKEN, VOLUME_QOS_TOKEN), VolumeVO.class);

    @NonCloneable
    public static String VOLUME_LIBVIRT_SECRET_HOST_TOKEN = "hostUuid";
    public static SystemTag VOLUME_LIBVIRT_SECRET_HOST = new SystemTag(
            String.format("volumeLibvirtSecretHost::{%s}", VOLUME_LIBVIRT_SECRET_HOST_TOKEN), VolumeVO.class);
}
