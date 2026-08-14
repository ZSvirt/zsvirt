package org.zstack.storage.primary.block.message;

import org.zstack.header.message.APIReply;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.storage.primary.block.BlockPrimaryStorageInventory;
import org.zstack.storage.primary.block.vendor.xstor.XStorDeviceImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/11 00:20
 */
@RestResponse(allTo = "inventories")
public class APIQueryBlockPrimaryStorageReply extends APIQueryReply {
    private List<BlockPrimaryStorageInventory> inventories = new ArrayList<>();

    public static APIQueryBlockPrimaryStorageReply __example__() {
        APIQueryBlockPrimaryStorageReply reply = new APIQueryBlockPrimaryStorageReply();
        BlockPrimaryStorageInventory blockPrimaryStorageInventory = new BlockPrimaryStorageInventory();
        String metadata = "{'ip':'127.0.0.1','port':8443, 'user':'optAdmin'}";
        XStorDeviceImpl xStorDevice = new XStorDeviceImpl(metadata);
        blockPrimaryStorageInventory.setUuid(uuid());
        blockPrimaryStorageInventory.setVendorName(xStorDevice.getVendorName());
        reply.inventories.add(blockPrimaryStorageInventory);
        return reply;
    }

    public void setInventories(List<BlockPrimaryStorageInventory> inventories) {
        this.inventories = inventories;
    }

    public List<BlockPrimaryStorageInventory> getInventories() {
        return inventories;
    }
}
