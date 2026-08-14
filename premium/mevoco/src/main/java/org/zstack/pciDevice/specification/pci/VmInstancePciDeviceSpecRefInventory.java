package org.zstack.pciDevice.specification.pci;

import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-05-23.
 */
@Inventory(mappingVOClass = VmInstancePciDeviceSpecRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pciSpec", inventoryClass = PciDeviceSpecInventory.class,
                foreignKey = "pciSpecUuid", expandedInventoryKey = "uuid"),
})
public class VmInstancePciDeviceSpecRefInventory {
    @APINoSee
    private Long id;
    private String vmInstanceUuid;
    private String pciSpecUuid;
    private Integer pciDeviceNumber;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmInstancePciDeviceSpecRefInventory() {
    }

    public VmInstancePciDeviceSpecRefInventory(VmInstancePciDeviceSpecRefVO vo) {
        id = vo.getId();
        vmInstanceUuid = vo.getVmInstanceUuid();
        pciSpecUuid = vo.getPciSpecUuid();
        pciDeviceNumber = vo.getPciDeviceNumber();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
    }

    public static VmInstancePciDeviceSpecRefInventory valueOf(VmInstancePciDeviceSpecRefVO vo) {
        return new VmInstancePciDeviceSpecRefInventory(vo);
    }

    public static List<VmInstancePciDeviceSpecRefInventory> valueOf(Collection<VmInstancePciDeviceSpecRefVO> vos) {
        return vos.stream().map(VmInstancePciDeviceSpecRefInventory::valueOf).collect(Collectors.toList());
    }

    public static VmInstancePciDeviceSpecRefInventory __example__() {
        VmInstancePciDeviceSpecRefInventory inv = new VmInstancePciDeviceSpecRefInventory();
        inv.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        inv.setPciSpecUuid(DocUtils.createFixedUuid(PciDeviceSpecVO.class));
        inv.setPciDeviceNumber(1);
        inv.setCreateDate(new Timestamp(DocUtils.date));
        inv.setLastOpDate(new Timestamp(DocUtils.date));
        return inv;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPciSpecUuid() {
        return pciSpecUuid;
    }

    public void setPciSpecUuid(String pciSpecUuid) {
        this.pciSpecUuid = pciSpecUuid;
    }

    public Integer getPciDeviceNumber() {
        return pciDeviceNumber;
    }

    public void setPciDeviceNumber(Integer pciDeviceNumber) {
        this.pciDeviceNumber = pciDeviceNumber;
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
