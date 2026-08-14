package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 5:32 PM
 * To change this template use File | Settings | File Templates.
 */
@RestResponse(allTo = "inventory")
public class APIChangeImageStateEvent extends APIEvent {
    private ImageInventory inventory;

    public APIChangeImageStateEvent(String apiId) {
        super(apiId);
    }

    public APIChangeImageStateEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeImageStateEvent __example__() {
        APIChangeImageStateEvent event = new APIChangeImageStateEvent();

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

        event.setInventory(inv);
        return event;
    }

}
