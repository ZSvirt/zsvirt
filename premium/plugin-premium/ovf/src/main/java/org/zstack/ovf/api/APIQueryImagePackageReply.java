package org.zstack.ovf.api;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.ovf.OvfConstant;
import org.zstack.ovf.datatype.ImagePackageInventory;
import org.zstack.ovf.datatype.ImagePackageState;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Qi Le on 2022/4/26
 */
@RestResponse(allTo = "inventories")
public class APIQueryImagePackageReply extends APIQueryReply {
    private List<ImagePackageInventory> inventories;

    public List<ImagePackageInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<ImagePackageInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryImagePackageReply __example__() {
        APIQueryImagePackageReply reply = new APIQueryImagePackageReply();
        reply.setInventories(new ArrayList<>());
        ImagePackageInventory inventory = new ImagePackageInventory();
        reply.getInventories().add(inventory);
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
        return reply;
    }
}
