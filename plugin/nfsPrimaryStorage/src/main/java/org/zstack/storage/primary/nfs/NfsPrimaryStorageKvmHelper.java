package org.zstack.storage.primary.nfs;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.Volume;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.primary.ImageCacheUtil;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.path.PathUtil;

import java.io.File;

public class NfsPrimaryStorageKvmHelper {
    public static String makeTemporaryRootVolumeInstallUrl(PrimaryStorageInventory pinv, VolumeInventory vol, String originVolumeUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeTemporaryRootVolumeInstallPath(vol, originVolumeUuid));
    }

    public static String makeTemporaryDataVolumeInstallUrl(PrimaryStorageInventory pinv, String volUuid, String originVolumeUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeTemporaryDataVolumeInstallPath(volUuid, originVolumeUuid));
    }

    public static String makeRootVolumeInstallUrl(PrimaryStorageInventory pinv, VolumeInventory vol) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }
    
    public static String makeDataVolumeInstallUrl(PrimaryStorageInventory pinv, String volUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }

    public static String makeMemoryVolumeParentInstallUrl(PrimaryStorageInventory pinv, VolumeInventory vol) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeMemoryVolumeParentInstallPath(vol));
    }

    public static String makeMemoryVolumeInstallUrl(PrimaryStorageInventory pinv, VolumeInventory vol) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeMemoryVolumeInstallPath(vol));
    }
    
    public static String makeCachedImageInstallUrl(PrimaryStorageInventory pinv, ImageInventory iminv) {
        return ImageCacheUtil.getImageCachePath(iminv, it -> PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv)));
    }

    public static String makeNvRamVolumeInstallUrl(PrimaryStorageInventory pinv, String volUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeNvRamVolumeInstallPath(volUuid));
    }

    public static String getCachedImageDir(PrimaryStorageInventory pinv){
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.getCachedImageInstallDir());
    }

    public static String makeCachedImageInstallUrlFromImageUuidForTemplate(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(imageUuid));
    }

    public static String makeTemplateFromVolumeInWorkspacePath(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(pinv.getMountPath(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    public static String makeVolumeInstallDir(PrimaryStorageInventory pinv, VolumeInventory vol) {
        String volPath = null;
        if (VolumeType.Data.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(pinv, vol.getUuid());
        } else if (VolumeType.Root.toString().equals(vol.getType())) {
            volPath = makeRootVolumeInstallUrl(pinv, vol);
        } else if (VolumeType.Memory.toString().equals(vol.getType())) {
            volPath = makeMemoryVolumeInstallUrl(pinv, vol);
        } else if (VolumeType.Cache.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(pinv, vol.getUuid());
        }

        DebugUtils.Assert(!StringUtils.isEmpty(volPath), "volPath should not be null");

        return new File(volPath).getParentFile().getAbsolutePath();
    }

    public static String makeKvmSnapshotInstallPath(PrimaryStorageInventory pinv, VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        String volDir = makeVolumeInstallDir(pinv, vol);
        return PathUtil.join(volDir, "snapshots", String.format("%s.qcow2", snapshot.getUuid()));
    }

    public static String makeKvmSnapshotInstallPath(PrimaryStorageInventory pinv, VolumeInventory vol, String snapshotUuid) {
        String volDir = makeVolumeInstallDir(pinv, vol);
        return PathUtil.join(volDir, "snapshots", String.format("%s.qcow2", snapshotUuid));
    }

    public static String makeSnapshotWorkspacePath(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(
                pinv.getMountPath(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }
    
    public static String makeDownloadImageJobName(ImageInventory iminv, PrimaryStorageInventory pinv) {
        return String.format("download-image-%s-to-pri-%s", iminv.getUuid(), pinv.getUuid());
    }

    public static String makeCopyImageCacheJobName(ImageCacheInventory cacheInv, PrimaryStorageInventory srcPs, PrimaryStorageInventory dstPs) {
        return String.format("copy-cache-of-image-%s-from-pri-%s-to-pri-%s", cacheInv.getImageUuid(), srcPs.getUuid(), dstPs.getUuid());
    }

    public static String makeJobOwnerName(PrimaryStorageInventory pinv) {
        return "NfsPrimaryStorage-" + pinv.getUuid();
    }
}
