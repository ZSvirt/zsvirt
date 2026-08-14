package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:35 2023/11/10
 */
@RestResponse(allTo = "inventory")
public class APICalculateImageHashEvent extends APIEvent {
    private ImageInventory inventory;

    public APICalculateImageHashEvent(String apiId) {
        super(apiId);
    }

    public APICalculateImageHashEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICalculateImageHashEvent __example__() {
        APICalculateImageHashEvent event = new APICalculateImageHashEvent();

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
        inv.setMd5Sum("6fc2357e711877c14c09eec960e51aed");

        event.setInventory(inv);
        return event;
    }

}
