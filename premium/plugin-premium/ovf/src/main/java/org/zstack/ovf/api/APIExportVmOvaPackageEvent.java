package org.zstack.ovf.api;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.ovf.OvfConstant;
import org.zstack.ovf.datatype.ImagePackageInventory;
import org.zstack.ovf.datatype.ImagePackageState;
import org.zstack.ovf.datatype.ImagePackageVO;
import org.zstack.utils.data.SizeUnit;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestResponse(allTo = "inventory")
public class APIExportVmOvaPackageEvent extends APIEvent {
    private ImagePackageInventory inventory;

    public APIExportVmOvaPackageEvent() {
    }

    public APIExportVmOvaPackageEvent(String apiId) {
        super(apiId);
    }

    public ImagePackageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImagePackageInventory inventory) {
        this.inventory = inventory;
    }

    public static APIExportVmOvaPackageEvent __example__() {
        APIExportVmOvaPackageEvent event = new APIExportVmOvaPackageEvent();
        ImagePackageInventory inventory = new ImagePackageInventory();
        event.setInventory(inventory);
        inventory.setUuid(uuid(ImagePackageVO.class));
        inventory.setName("ova");
        inventory.setDescription("description");
        inventory.setVmUuid(uuid(VmInstanceVO.class));
        inventory.setBackupStorageUuid(uuid(BackupStorageVO.class));
        inventory.setFormat(OvfConstant.OVA_FORMAT);
        inventory.setSize(SizeUnit.GIGABYTE.toByte(10));
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        inventory.setExportUrl("http://bs-host-name/path/to/ova.ova");
        inventory.setMd5Sum("sampleMd5Sum");
        inventory.setState(ImagePackageState.Exported);
        return event;
    }
}
