package org.zstack.ovf.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.ovf.OvfConstant;
import org.zstack.ovf.datatype.ImagePackageInventory;
import org.zstack.ovf.datatype.ImagePackageState;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestResponse(allTo = "inventory")
public class APIUpdateImagePackageEvent extends APIEvent {
    private ImagePackageInventory inventory;

    public APIUpdateImagePackageEvent() {
    }

    public APIUpdateImagePackageEvent(String apiId) {
        super(apiId);
    }

    public ImagePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImagePackageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateImagePackageEvent __example__() {
        APIUpdateImagePackageEvent event = new APIUpdateImagePackageEvent();
        ImagePackageInventory inventory = new ImagePackageInventory();
        event.setInventory(inventory);
        inventory.setUuid(uuid());
        inventory.setName("ova");
        inventory.setDescription("description");
        inventory.setVmUuid(uuid());
        inventory.setBackupStorageUuid(uuid());
        inventory.setFormat(OvfConstant.OVA_FORMAT);
        inventory.setSize(SizeUnit.GIGABYTE.toByte(10));
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));
        inventory.setExportUrl("http://bs-host-name/path/to/ova.ova");
        inventory.setMd5Sum("sampleMd5Sum");
        inventory.setState(ImagePackageState.Exported);
        return event;
    }
}
