package org.zstack.storage.primary;

import org.zstack.core.db.Q;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;

import java.util.function.Function;

public class ImageCacheUtil {
    public static String getImageCachePath(ImageInventory image, Function<String, String> maker) {
        if (!image.getUrl().startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            return maker.apply(image.getUuid());
        }

        String snapshotUuid = image.getUrl().substring(image.getUrl().length() - 32);
        return Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.primaryStorageInstallPath)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .findValue();
    }

    public static String getImageCachePath(ImageCacheInventory cache) {
        if (!cache.getInstallUrl().contains(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            return cache.getInstallUrl();
        }
        String snapshotUuid = cache.getInstallUrl().substring(cache.getInstallUrl().length() - 32);
        return Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.primaryStorageInstallPath)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .findValue();
    }
}
