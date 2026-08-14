package org.zstack.pciDevice.specification.mdev;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.pciDevice.PciDeviceInventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-04-18.
 */
@Inventory(mappingVOClass = PciDeviceMdevSpecRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "pciDevice", inventoryClass = PciDeviceInventory.class,
                foreignKey = "pciDeviceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "mdevSpec", inventoryClass = MdevDeviceSpecInventory.class,
                foreignKey = "mdevSpecUuid", expandedInventoryKey = "uuid"),
})
public class PciDeviceMdevSpecRefInventory {
    @APINoSee
    private Long id;
    private String pciDeviceUuid;
    private String mdevSpecUuid;
    private Boolean effective;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public PciDeviceMdevSpecRefInventory() {
    }

    public PciDeviceMdevSpecRefInventory(PciDeviceMdevSpecRefVO vo) {
        id = vo.getId();
        pciDeviceUuid = vo.getPciDeviceUuid();
        mdevSpecUuid = vo.getMdevSpecUuid();
        effective = vo.isEffective();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
    }

    public static PciDeviceMdevSpecRefInventory valueOf(PciDeviceMdevSpecRefVO vo) {
        return new PciDeviceMdevSpecRefInventory(vo);
    }

    public static List<PciDeviceMdevSpecRefInventory> valueOf(Collection<PciDeviceMdevSpecRefVO> vos) {
        return vos.stream().map(PciDeviceMdevSpecRefInventory::valueOf).collect(Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    public Boolean getEffective() {
        return effective;
    }

    public void setEffective(Boolean effective) {
        this.effective = effective;
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
