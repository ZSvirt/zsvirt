package org.zstack.storage.backup.imagestore;

import org.zstack.header.image.*;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;

/**
 * Created by mingjian.deng on 2017/9/12.
 */
@RestResponse(allTo = "inventory")
public class APISyncImageFromImageStoreBackupStorageEvent extends APIEvent {
    private ImageInventory inventory;

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }

    public APISyncImageFromImageStoreBackupStorageEvent(String apiId) {
        super(apiId);
    }

    public APISyncImageFromImageStoreBackupStorageEvent() {
        super(null);
    }

    public static APISyncImageFromImageStoreBackupStorageEvent __example__() {
        APISyncImageFromImageStoreBackupStorageEvent event = new APISyncImageFromImageStoreBackupStorageEvent();
        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("zstore://ab4a0065f50425bf8f5d2aed253fbd85/84951a97167d0c291c53256810855b97b20cf00d");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("disaster");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setUrl("http://192.168.1.20/share/images/tinylinux.qcow2");
        inv.setFormat(ImageConstant.QCOW2_FORMAT_STRING);
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());
        inv.setDescription("disaster");

        event.setInventory(inv);

        return event;
    }
}
