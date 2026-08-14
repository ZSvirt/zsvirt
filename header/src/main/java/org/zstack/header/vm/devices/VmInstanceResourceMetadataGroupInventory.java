package org.zstack.header.vm.devices;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/20 18:03
 */
@Inventory(mappingVOClass = VmInstanceResourceMetadataGroupVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volumeSnapshotRef", inventoryClass = VmInstanceResourceMetadataArchiveInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "addressGroupUuid", hidden = true),
})
public class VmInstanceResourceMetadataGroupInventory {
    private String uuid;
    private String resourceUuid;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    @Queryable(mappingClass = VmInstanceResourceMetadataArchiveInventory.class,
            joinColumn = @JoinColumn(name = "addressGroupUuid"))
    private List<VmInstanceResourceMetadataArchiveInventory> addressList;

    public static VmInstanceResourceMetadataGroupInventory valueOf(VmInstanceResourceMetadataGroupVO vo) {
        VmInstanceResourceMetadataGroupInventory inv = new VmInstanceResourceMetadataGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setAddressList(VmInstanceResourceMetadataArchiveInventory.valueOf(vo.getAddressList()));
        return inv;
    }

    public static List<VmInstanceResourceMetadataGroupInventory> valueOf(Collection<VmInstanceResourceMetadataGroupVO> vos) {
        return vos.stream().map(VmInstanceResourceMetadataGroupInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public List<VmInstanceResourceMetadataArchiveInventory> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<VmInstanceResourceMetadataArchiveInventory> addressList) {
        this.addressList = addressList;
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
