package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(allTo = "inventory")
public class APICreateRootVolumeTemplateFromRootVolumeEvent extends APIEvent {
    private ImageInventory inventory;

    public APICreateRootVolumeTemplateFromRootVolumeEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromRootVolumeEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateRootVolumeTemplateFromRootVolumeEvent __example__() {
        APICreateRootVolumeTemplateFromRootVolumeEvent event = new APICreateRootVolumeTemplateFromRootVolumeEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("ceph://zs-images/0cd599ec519249489475112a058bb93a");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("My Root Volume Template");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setFormat(ImageConstant.RAW_FORMAT_STRING);
        inv.setArchitecture(ImageArchitecture.x86_64.toString());
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());

        event.setInventory(inv);

        return event;
    }

}
