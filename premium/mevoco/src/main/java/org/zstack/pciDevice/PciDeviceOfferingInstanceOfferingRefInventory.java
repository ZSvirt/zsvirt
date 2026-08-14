package org.zstack.pciDevice;

import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 10/07/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PciDeviceOfferingInstanceOfferingRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "instanceOffering", inventoryClass = InstanceOfferingInventory.class,
                foreignKey = "instanceOfferingUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pciDeviceOffering", inventoryClass = PciDeviceOfferingInventory.class,
                foreignKey = "pciDeviceOfferingUuid", expandedInventoryKey = "uuid"),
})
public class PciDeviceOfferingInstanceOfferingRefInventory implements Serializable {
    private long id;

    private String instanceOfferingUuid;

    private String pciDeviceOfferingUuid;

    private PciDeviceMetaData metadata;

    private Integer pciDeviceCount;

    public PciDeviceOfferingInstanceOfferingRefInventory() {
    }

    public PciDeviceOfferingInstanceOfferingRefInventory(PciDeviceOfferingInstanceOfferingRefVO vo) {
        this.setId(vo.getId());
        this.setInstanceOfferingUuid(vo.getInstanceOfferingUuid());
        this.setPciDeviceOfferingUuid(vo.getPciDeviceOfferingUuid());
        this.setMetadata(vo.getPciDeviceMetaData());
        this.setPciDeviceCount(vo.getPciDeviceCount());
    }

    public static PciDeviceOfferingInstanceOfferingRefInventory valueOf(PciDeviceOfferingInstanceOfferingRefVO vo) {
        return new PciDeviceOfferingInstanceOfferingRefInventory(vo);
    }

    public static List<PciDeviceOfferingInstanceOfferingRefInventory> valueOf(Collection<PciDeviceOfferingInstanceOfferingRefVO> vos) {
        List<PciDeviceOfferingInstanceOfferingRefInventory> invs = new ArrayList<>();
        for (PciDeviceOfferingInstanceOfferingRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }

    public PciDeviceMetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(PciDeviceMetaData metadata) {
        this.metadata = metadata;
    }

    public Integer getPciDeviceCount() {
        return pciDeviceCount;
    }

    public void setPciDeviceCount(Integer pciDeviceCount) {
        this.pciDeviceCount = pciDeviceCount;
    }
}
