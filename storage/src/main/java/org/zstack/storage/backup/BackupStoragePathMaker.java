package org.zstack.storage.backup;

import org.apache.commons.io.FilenameUtils;
import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.identity.AccountManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackupStoragePathMaker {
    private static final List<String> knownImageExtensions = new ArrayList<String>();
    static {
        knownImageExtensions.add("qcow2");
        knownImageExtensions.add("vmdk");
        knownImageExtensions.add("ova");
        knownImageExtensions.add("img");
        knownImageExtensions.add("vhd");
        knownImageExtensions.add("qcow");
        knownImageExtensions.add("raw");
    }

    private static final String ROOT_VOLUME_TEMPLATE_FOLDER = "rootVolumeTemplates";
    private static final String DATA_VOLUME_TEMPLATE_FOLDER = "dataVolumeTemplates";
    private static final String ISO_FOLDER = "dataVolumeTemplates";
    private static final String VOLUME_SNAPSHOT_FOLDER = "volumeSnapshots";

    private static AccountManager accountManager;

    public static AccountManager getAccountManager() {
        if (accountManager == null) {
            accountManager = Platform.getComponentLoader().getComponent(AccountManager.class);
        }
        return accountManager;
    }

    private static String makeFilename(String installPath, String format) {
        File f = new File(installPath);
        String baseName = FilenameUtils.getBaseName(f.getAbsolutePath());
        String suffix = FilenameUtils.getExtension(f.getAbsolutePath());
        if (!knownImageExtensions.contains(baseName)) {
            if (format.equals(ImageMediaType.RootVolumeTemplate.toString())) {
                suffix = "template";
            } else if (format.equals(ImageMediaType.ISO.toString())) {
                suffix = "iso";
            } else if (format.equals(ImageMediaType.DataVolumeTemplate.toString())) {
                suffix = "volume";
            }
        }

        return String.format("%s.%s", baseName, suffix);
    }

    private static String accountFolder(String accountUuid) {
        return "acct-" + accountUuid;
    }

    public static String makeVolumeInstallPath(String url, VolumeInventory vol) {
        String accountUuid = getAccountManager().getOwnerAccountUuidOfResource(vol.getUuid());
        String fileName = makeFilename(url, "volume");
        return PathUtil.join("volumes", accountFolder(accountUuid), vol.getUuid(), fileName);
    }
    
    public static String makeImageInstallPath(ImageInventory iminv) {
        String root;
        if (iminv.getMediaType().equals(ImageMediaType.RootVolumeTemplate.toString())) {
            root = ROOT_VOLUME_TEMPLATE_FOLDER;
        } else if (iminv.getMediaType().equals(ImageMediaType.ISO.toString())) {
            root = ISO_FOLDER;
        } else if (iminv.getMediaType().equals(ImageMediaType.DataVolumeTemplate.toString())) {
            root = DATA_VOLUME_TEMPLATE_FOLDER;
        } else {
            throw new CloudRuntimeException(String.format("unknown image mediaType[%s]", iminv.getMediaType()));
        }
        
        String accountUuid = getAccountManager().getOwnerAccountUuidOfResource(iminv.getUuid());

        String filename = makeFilename(iminv.getUrl(), iminv.getMediaType());
        return PathUtil.join(root, accountFolder(accountUuid), iminv.getUuid(), filename);
    }

    public static String makeImageInstallPath(String imageUuid, String mediaType) {
        String root;
        String suffix;
        if (mediaType.equals(ImageMediaType.RootVolumeTemplate.toString())) {
            root = ROOT_VOLUME_TEMPLATE_FOLDER;
            suffix = "template";
        } else if (mediaType.equals(ImageMediaType.ISO.toString())) {
            root = ISO_FOLDER;
            suffix = "iso";
        } else if (mediaType.equals(ImageMediaType.DataVolumeTemplate.toString())) {
            root = DATA_VOLUME_TEMPLATE_FOLDER;
            suffix = "volume";
        } else if (mediaType.equals(VolumeSnapshotVO.class.getSimpleName())) {
            root = VOLUME_SNAPSHOT_FOLDER;
            suffix = "snapshot";
        } else {
            throw new CloudRuntimeException(String.format("unknown image mediaType[%s]", mediaType));
        }

        String accountUuid = getAccountManager().getOwnerAccountUuidOfResource(imageUuid);
        String filename = String.format("%s.%s", imageUuid, suffix);
        return PathUtil.join(root, accountFolder(accountUuid), imageUuid, filename);
    }

    private static String getAccountUuid(String resourceUuid) {
        AccountManager acntMgr = Platform.getComponentLoader().getComponent(AccountManager.class);
        String accountUuid = acntMgr.getOwnerAccountUuidOfResource(resourceUuid);
        DebugUtils.Assert(accountUuid!=null, String.format("cannot find account uuid for resource[uuid:%s]", resourceUuid));
        return accountUuid;
    }

    public static String makeDataVolumeTemplateInstallFolderPath(String volumeUUid) {
        String accountUuid = getAccountUuid(volumeUUid);
        return PathUtil.join(DATA_VOLUME_TEMPLATE_FOLDER, accountFolder(accountUuid), volumeUUid);
    }

    public static String makeRootVolumeTemplateInstallFolderPath(String imageUuid) {
        String accountUuid = getAccountUuid(imageUuid);
        return PathUtil.join(ROOT_VOLUME_TEMPLATE_FOLDER, accountFolder(accountUuid), imageUuid);
    }

    public static String makeVolumeSnapshotInstallFolderPath(String snapshotUuid) {
        String accountUuid = getAccountUuid(snapshotUuid);
        return PathUtil.join(VOLUME_SNAPSHOT_FOLDER, accountFolder(accountUuid), snapshotUuid);
    }
}
