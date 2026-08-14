package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateDataVolumeTemplateFromVolumeEvent extends APIEvent {
    private ImageInventory inventory;

    public APICreateDataVolumeTemplateFromVolumeEvent(String apiId) {
        super(apiId);
    }

    public APICreateDataVolumeTemplateFromVolumeEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateDataVolumeTemplateFromVolumeEvent __example__() {
        APICreateDataVolumeTemplateFromVolumeEvent event = new APICreateDataVolumeTemplateFromVolumeEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("ceph://zs-data-volume/0cd599ec519249489475112a058bb93a");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("My Data Volume Template");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setFormat(ImageConstant.RAW_FORMAT_STRING);
        inv.setMediaType(ImageConstant.ImageMediaType.DataVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());

        event.setInventory(inv);
        return event;
    }

}
