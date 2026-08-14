package org.zstack.header.storage.volume.backup;

import org.zstack.header.image.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(fieldsTo = {"inventory"})
public class APICreateDataVolumeTemplateFromVolumeBackupEvent extends APIEvent {
    public APICreateDataVolumeTemplateFromVolumeBackupEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeTemplateFromVolumeBackupEvent() {
        super(null);
    }

    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateDataVolumeTemplateFromVolumeBackupEvent __example__() {
        APICreateDataVolumeTemplateFromVolumeBackupEvent event = new APICreateDataVolumeTemplateFromVolumeBackupEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("zstore://mydata/0cd599ec159249489475112a058bb93a");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("My Volume Template");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        inv.setPlatform(ImagePlatform.Linux.toString());
        inv.setMediaType(ImageConstant.ImageMediaType.DataVolumeTemplate.toString());

        event.setInventory(inv);

        return event;
    }
}
