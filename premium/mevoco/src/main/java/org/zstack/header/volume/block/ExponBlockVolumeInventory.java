package org.zstack.header.volume.block;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ExponBlockVolumeVO.class, collectionValueOfMethod="valueOf2",
        parent = {@Parent(inventoryClass = BlockVolumeInventory.class, type = BlockVolumeConstants.EXPON_BLOCK_VOLUME_TYPE)})
public class ExponBlockVolumeInventory extends BlockVolumeInventory {
    private String exponStatus;

    public ExponBlockVolumeInventory() {
    }

    public ExponBlockVolumeInventory(ExponBlockVolumeVO vo) {
        super(vo);
        this.exponStatus = vo.getExponStatus();
    }

    public static ExponBlockVolumeInventory valueOf(ExponBlockVolumeVO vo) {
        return new ExponBlockVolumeInventory(vo);
    }

    public static List<ExponBlockVolumeInventory> valueOf2(Collection<ExponBlockVolumeVO> vos) {
        List<ExponBlockVolumeInventory> invs = new ArrayList<ExponBlockVolumeInventory>(vos.size());
        for (ExponBlockVolumeVO vo : vos) {
            invs.add(new ExponBlockVolumeInventory(vo));
        }
        return invs;
    }


    public String getExponStatus() {
        return exponStatus;
    }

    public void setExponStatus(String exponStatus) {
        this.exponStatus = exponStatus;
    }
}
