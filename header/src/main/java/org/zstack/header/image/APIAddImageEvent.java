package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

@RestResponse(allTo = "inventory")
public class APIAddImageEvent extends APIEvent {
    public APIAddImageEvent(String apiId) {
        super(apiId);
    }

    public APIAddImageEvent() {
        super(null);
    }

    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIAddImageEvent __example__() {
        APIAddImageEvent event = new APIAddImageEvent();

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
        inv.setArchitecture(ImageArchitecture.x86_64.toString());

        event.setInventory(inv);
        return event;
    }

}
