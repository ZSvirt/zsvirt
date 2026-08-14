package org.zstack.storage.addon.primary;

import org.zstack.header.storage.addon.primary.BaseVolumeInfo;

public class ExternalPrimaryStorageNameHelper {
    public static String buildVolumeName(String volumeUuid) {
        return String.format("volume_%s", volumeUuid);
    }

    public static BaseVolumeInfo getVolumeInfo(String volumeName) {
        if (volumeName.startsWith("volume_")) {
            BaseVolumeInfo info = new BaseVolumeInfo();
            info.setUuid(volumeName.split("_")[1]);
            info.setType("volume");
            return info;
        } else if (volumeName.startsWith("image_")) {
            BaseVolumeInfo info = new BaseVolumeInfo();
            info.setUuid(volumeName.split("_")[1]);
            info.setType("image");
            return info;
        } else if (volumeName.startsWith("reimage_volume")) {
            BaseVolumeInfo info = new BaseVolumeInfo();
            info.setUuid(volumeName.split("_")[2]);
            info.setType("volume");
            return info;
        } else if (volumeName.startsWith("change_image_volume")) {
            BaseVolumeInfo info = new BaseVolumeInfo();
            info.setUuid(volumeName.split("_")[3]);
            info.setType("volume");
            return info;
        } else {
            throw new IllegalArgumentException(String.format("unknown volume name[%s]", volumeName));
        }
    }

    public static String buildChangeImageVolumeName(String volumeUuid) {
        return String.format("change_image_volume_%s_%d", volumeUuid, System.currentTimeMillis());
    }

    public static String buildReimageVolumeName(String volumeUuid) {
        return String.format("reimage_volume_%s_%d", volumeUuid, System.currentTimeMillis());
    }

    public static String buildImageName(String imageUuid) {
        return String.format("image_%s", imageUuid);
    }

    public static String buildSnapshotName(String snapshotUuid) {
        return String.format("snapshot_%s", snapshotUuid);
    }
}
