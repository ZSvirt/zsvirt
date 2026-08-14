package org.zstack.header.image;

import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class ImageDeletionStruct {
    private Boolean deleteAll;
    private ImageInventory image;
    private List<String> backupStorageUuids;

    public Boolean getDeleteAll() {
        if (backupStorageUuids == null) {
            return true;
        }

        if (deleteAll == null) {
            List<String> bsUuids = transformAndRemoveNull(image.getBackupStorageRefs(),
                    ImageBackupStorageRefInventory::getBackupStorageUuid);

            deleteAll = backupStorageUuids.containsAll(bsUuids);
        }

        return deleteAll;
    }

    public ImageInventory getImage() {
        return image;
    }

    public void setImage(ImageInventory image) {
        this.image = image;
    }

    public List<String> getBackupStorageUuids() {
        return backupStorageUuids;
    }

    public void setBackupStorageUuids(List<String> backupStorageUuids) {
        this.backupStorageUuids = backupStorageUuids;
    }
}
