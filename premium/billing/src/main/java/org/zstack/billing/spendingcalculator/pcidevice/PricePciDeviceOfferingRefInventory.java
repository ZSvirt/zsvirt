package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.PriceInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.pciDevice.*;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by shixin on 2018/05/04.
 */
@PythonClassInventory
@Inventory(mappingVOClass = PricePciDeviceOfferingRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "price", inventoryClass = PriceInventory.class,
                foreignKey = "priceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pciDeviceOffering", inventoryClass = PciDeviceOfferingInventory.class,
                foreignKey = "pciDeviceOfferingUuid", expandedInventoryKey = "uuid"),
})
public class PricePciDeviceOfferingRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String priceUuid;
    private String pciDeviceOfferingUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public PricePciDeviceOfferingRefInventory() {
    }

    public PricePciDeviceOfferingRefInventory(PricePciDeviceOfferingRefVO vo) {
        this.setId(vo.getId());
        this.setPriceUuid(vo.getPriceUuid());
        this.setPciDeviceOfferingUuid(vo.getPciDeviceOfferingUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static PricePciDeviceOfferingRefInventory valueOf(PricePciDeviceOfferingRefVO vo) {
        return new PricePciDeviceOfferingRefInventory(vo);
    }

    public static List<PricePciDeviceOfferingRefInventory> valueOf(Collection<PricePciDeviceOfferingRefVO> vos) {
        List<PricePciDeviceOfferingRefInventory> invs = new ArrayList<>();
        for (PricePciDeviceOfferingRefVO vo : vos) {
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

    public String getPriceUuid() {
        return priceUuid;
    }

    public void setPriceUuid(String priceUuid) {
        this.priceUuid = priceUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
