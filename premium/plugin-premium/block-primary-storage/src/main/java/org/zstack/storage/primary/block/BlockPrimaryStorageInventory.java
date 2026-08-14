package org.zstack.storage.primary.block;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/11 00:26
 */

@Inventory(mappingVOClass = BlockPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = BlockPrimaryStorageConstants.BLOCK_PRIMARY_STORAGE_TYPE)})
@PythonClassInventory
public class BlockPrimaryStorageInventory extends PrimaryStorageInventory {
    private String vendorName;

    private String metadata;

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadata() {
        return metadata;
    }

    public BlockPrimaryStorageInventory() {}

    public BlockPrimaryStorageInventory(BlockPrimaryStorageVO bpsvo) {
        super(bpsvo);
        setMetadata(bpsvo.getMetadata());
        setVendorName(bpsvo.getVendorName());
    }

    public static BlockPrimaryStorageInventory valueOf(BlockPrimaryStorageVO bpsvo) {
        return new BlockPrimaryStorageInventory(bpsvo);
    }

    public static List<BlockPrimaryStorageInventory> valueOf1(Collection<BlockPrimaryStorageVO> bpsvos) {
        List<BlockPrimaryStorageInventory> invs = new ArrayList<>();
        for (BlockPrimaryStorageVO bpsvo : bpsvos) {
            invs.add(valueOf(bpsvo));
        }
        return invs;
    }
}