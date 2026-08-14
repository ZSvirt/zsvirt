package org.zstack.pciDevice.specification.mdev;

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
@Inventory(mappingVOClass = VmInstanceMdevDeviceSpecRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmInstanceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "mdevSpec", inventoryClass = MdevDeviceSpecInventory.class,
                foreignKey = "mdevSpecUuid", expandedInventoryKey = "uuid"),
})
public class VmInstanceMdevDeviceSpecRefInventory {
    @APINoSee
    private Long id;
    private String vmInstanceUuid;
    private String mdevSpecUuid;
    private Integer mdevDeviceNumber;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VmInstanceMdevDeviceSpecRefInventory() {
    }

    public VmInstanceMdevDeviceSpecRefInventory(VmInstanceMdevDeviceSpecRefVO vo) {
        id = vo.getId();
        vmInstanceUuid = vo.getVmInstanceUuid();
        mdevSpecUuid = vo.getMdevSpecUuid();
        mdevDeviceNumber = vo.getMdevDeviceNumber();
        createDate = vo.getCreateDate();
        lastOpDate = vo.getLastOpDate();
    }

    public static VmInstanceMdevDeviceSpecRefInventory valueOf(VmInstanceMdevDeviceSpecRefVO vo) {
        return new VmInstanceMdevDeviceSpecRefInventory(vo);
    }

    public static List<VmInstanceMdevDeviceSpecRefInventory> valueOf(Collection<VmInstanceMdevDeviceSpecRefVO> vos) {
        return vos.stream().map(VmInstanceMdevDeviceSpecRefInventory::valueOf).collect(Collectors.toList());
    }

    public static VmInstanceMdevDeviceSpecRefInventory __example__() {
        VmInstanceMdevDeviceSpecRefInventory inv = new VmInstanceMdevDeviceSpecRefInventory();
        inv.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        inv.setMdevSpecUuid(DocUtils.createFixedUuid(MdevDeviceSpecVO.class));
        inv.setMdevDeviceNumber(1);
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

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    public Integer getMdevDeviceNumber() {
        return mdevDeviceNumber;
    }

    public void setMdevDeviceNumber(Integer mdevDeviceNumber) {
        this.mdevDeviceNumber = mdevDeviceNumber;
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
