package org.zstack.storage.primary;

import org.zstack.core.Platform;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.identity.AccountManager;
import org.zstack.utils.path.PathUtil;

public class PrimaryStoragePathMaker {
    public static String cachedImageInstallDir = "imagecache";

    private static AccountManager acntMgr;

    protected static String getAccountUuidOfResource(String resourceUuid) {
        if (acntMgr == null) {
            acntMgr = Platform.getComponentLoader().getComponent(AccountManager.class);
        }
        return acntMgr.getOwnerAccountUuidOfResource(resourceUuid);
    }

    public static String makeTemporaryRootVolumeInstallPath(VolumeInventory vol, String originVolumeUuid) {
        return PathUtil.join("rootVolumes", "acct-" + getAccountUuidOfResource(vol.getUuid()), "vol-" + originVolumeUuid, vol.getUuid() + ".qcow2");
    }

    public static String makeRootVolumeInstallPath(VolumeInventory vol) {
        return PathUtil.join("rootVolumes", "acct-" + getAccountUuidOfResource(vol.getUuid()), "vol-" + vol.getUuid(), vol.getUuid() + ".qcow2");
    }

    public static String makeMemoryVolumeParentInstallPath(VolumeInventory vol) {
        return PathUtil.join("memory", "acct-" + getAccountUuidOfResource(vol.getUuid()), "vol-" + vol.getUuid(), "");
    }

    public static String makeMemoryVolumeInstallPath(VolumeInventory vol) {
        return PathUtil.join("memory", "acct-" + getAccountUuidOfResource(vol.getUuid()), "vol-" + vol.getUuid(), vol.getUuid() + ".qcow2");
    }

    public static String makeTemporaryDataVolumeInstallPath(String volUuid, String originVolumeUuid) {
        return PathUtil.join("dataVolumes", "acct-" + getAccountUuidOfResource(volUuid), "vol-" + originVolumeUuid, volUuid + ".qcow2");
    }

    public static String makeDataVolumeInstallPath(String volUuid) {
        return PathUtil.join("dataVolumes", "acct-" + getAccountUuidOfResource(volUuid), "vol-" + volUuid, volUuid + ".qcow2");
    }

    public static String makeNvRamVolumeInstallPath(String volUuid) {
        return PathUtil.join("nvRam", "acct-" + getAccountUuidOfResource(volUuid), "vol-" + volUuid, volUuid + ".raw");
    }

    public static String makeImageFromSnapshotWorkspacePath(String imageUuid) {
        return PathUtil.join("snapshotWorkspace", String.format("image-%s", imageUuid));
    }

    public static String makeCachedImageInstallPath(ImageInventory iminv) {
        if (iminv.getMediaType().equals(ImageMediaType.ISO.toString())) {
            return PathUtil.join(getCachedImageInstallDir(), "iso", iminv.getUuid(), iminv.getUuid() + ".iso");
        } else {
            return PathUtil.join(getCachedImageInstallDir(), "template", iminv.getUuid(), iminv.getUuid() + ".qcow2");
        }
    }

    public static String makeCachedImageInstallPathFromImageUuidForTemplate(String imageUuid) {
        return PathUtil.join(getCachedImageInstallDir(), "template", imageUuid, imageUuid + ".qcow2");
    }

    public static String getCachedImageInstallDir(){
        return cachedImageInstallDir;
    }
}
