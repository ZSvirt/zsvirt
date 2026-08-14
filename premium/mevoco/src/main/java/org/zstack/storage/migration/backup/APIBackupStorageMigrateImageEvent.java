package org.zstack.storage.migration.backup;

import org.zstack.header.image.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created by GuoYi on 8/31/17.
 */
@RestResponse(allTo = "inventory")
public class APIBackupStorageMigrateImageEvent extends APIEvent {
    private ImageInventory inventory;

    public APIBackupStorageMigrateImageEvent() {
    }

    public APIBackupStorageMigrateImageEvent(String apiId) {
        super(apiId);
    }

    public static APIBackupStorageMigrateImageEvent __example__() {
        APIBackupStorageMigrateImageEvent evt = new APIBackupStorageMigrateImageEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("ceph://zs-images/f0b149e053b34c7eb7fe694b182ebffd");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("TinyLinux");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2");
        inv.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());

        evt.setInventory(inv);
        evt.setSuccess(true);
        return evt;
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
}
