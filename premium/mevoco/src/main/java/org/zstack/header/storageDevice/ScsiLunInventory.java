package org.zstack.header.storageDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = ScsiLunVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "scsiLunHostRef", inventoryClass = ScsiLunHostRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "scsiLunUuid"),
        @ExpandedQuery(expandedField = "scsiLunVmInstanceRef", inventoryClass = ScsiLunVmInstanceRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "scsiLunUuid"),
})
public class ScsiLunInventory extends LunInventory implements Serializable {
    private List<ScsiLunHostRefInventory> scsiLunHostRefs = new ArrayList<>();

    private List<ScsiLunVmInstanceRefInventory> scsiLunVmInstanceRefs = new ArrayList<>();

    public ScsiLunInventory() {
    }

    public ScsiLunInventory(ScsiLunVO vo) {
        super(vo);
        this.setScsiLunVmInstanceRefs(ScsiLunVmInstanceRefInventory.valueOf1(vo.getScsiLunVmInstanceRefs()));
        this.setScsiLunHostRefs(ScsiLunHostRefInventory.valueOf1(vo.getScsiLunHostRefs()));
    }

    public static ScsiLunInventory valueOf(ScsiLunVO vo) {
        return new ScsiLunInventory(vo);
    }

    public static List<ScsiLunInventory> valueOf1(Collection<ScsiLunVO> vos) {
        return vos.stream().map(ScsiLunInventory::new).collect(Collectors.toList());
    }

    public List<ScsiLunHostRefInventory> getScsiLunHostRefs() {
        return scsiLunHostRefs;
    }

    public void setScsiLunHostRefs(List<ScsiLunHostRefInventory> scsiLunHostRefs) {
        this.scsiLunHostRefs = scsiLunHostRefs;
    }

    public List<ScsiLunVmInstanceRefInventory> getScsiLunVmInstanceRefs() {
        return scsiLunVmInstanceRefs;
    }

    public void setScsiLunVmInstanceRefs(List<ScsiLunVmInstanceRefInventory> scsiLunVmInstanceRefs) {
        this.scsiLunVmInstanceRefs = scsiLunVmInstanceRefs;
    }
}
