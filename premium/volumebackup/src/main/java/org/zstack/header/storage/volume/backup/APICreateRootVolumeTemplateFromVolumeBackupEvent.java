package org.zstack.header.storage.volume.backup;

import org.zstack.header.image.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(fieldsTo = {"inventory"})
public class APICreateRootVolumeTemplateFromVolumeBackupEvent extends APIEvent {
    public APICreateRootVolumeTemplateFromVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromVolumeBackupEvent() {
        super(null);
    }

    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateRootVolumeTemplateFromVolumeBackupEvent __example__() {
        APICreateRootVolumeTemplateFromVolumeBackupEvent event = new APICreateRootVolumeTemplateFromVolumeBackupEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("zstore://centos/0cd599ec519249489475112a058bb93a");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("My Volume Template");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        inv.setPlatform(ImagePlatform.Linux.toString());
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());

        event.setInventory(inv);

        return event;
    }
}
