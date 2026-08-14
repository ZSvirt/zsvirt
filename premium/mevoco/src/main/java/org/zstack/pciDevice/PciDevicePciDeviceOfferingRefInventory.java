package org.zstack.pciDevice;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 10/07/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PciDevicePciDeviceOfferingRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "pciDevice", inventoryClass = PciDeviceInventory.class,
                foreignKey = "pciDeviceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pciDeviceOffering", inventoryClass = PciDeviceOfferingInventory.class,
                foreignKey = "pciDeviceOfferingUuid", expandedInventoryKey = "uuid"),
})
public class PciDevicePciDeviceOfferingRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String pciDeviceUuid;
    private String pciDeviceOfferingUuid;

    public PciDevicePciDeviceOfferingRefInventory() {
    }

    public PciDevicePciDeviceOfferingRefInventory(PciDevicePciDeviceOfferingRefVO vo) {
        this.setId(vo.getId());
        this.setPciDeviceUuid(vo.getPciDeviceUuid());
        this.setPciDeviceOfferingUuid(vo.getPciDeviceOfferingUuid());
    }

    public static PciDevicePciDeviceOfferingRefInventory valueOf(PciDevicePciDeviceOfferingRefVO vo) {
        return new PciDevicePciDeviceOfferingRefInventory(vo);
    }

    public static List<PciDevicePciDeviceOfferingRefInventory> valueOf(Collection<PciDevicePciDeviceOfferingRefVO> vos) {
        List<PciDevicePciDeviceOfferingRefInventory> invs = new ArrayList<>();
        for (PciDevicePciDeviceOfferingRefVO vo : vos) {
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

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }
}
