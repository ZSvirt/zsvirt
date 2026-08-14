package org.zstack.header.vm.devices;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by LiangHanYu on 2022/6/17 17:24
 */
@Inventory(mappingVOClass = VmInstanceResourceMetadataArchiveVO.class)
public class VmInstanceResourceMetadataArchiveInventory {
    private long id;
    private String resourceUuid;
    private String vmInstanceUuid;
    private String deviceAddress;
    private String addressGroupUuid;
    private String metadata;
    private String metadataClass;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static VmInstanceResourceMetadataArchiveInventory valueOf(VmInstanceResourceMetadataArchiveVO vo) {
        VmInstanceResourceMetadataArchiveInventory inv = new VmInstanceResourceMetadataArchiveInventory();
        inv.setId(vo.getId());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setDeviceAddress(vo.getDeviceAddress());
        inv.setAddressGroupUuid(vo.getAddressGroupUuid());
        inv.setVmInstanceUuid(vo.getVmInstanceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setMetadata(vo.getMetadata());
        inv.setMetadataClass(vo.getMetadataClass());
        return inv;
    }

    public static List<VmInstanceResourceMetadataArchiveInventory> valueOf(Collection<VmInstanceResourceMetadataArchiveVO> vos) {
        return vos.stream().map(VmInstanceResourceMetadataArchiveInventory::valueOf).collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getAddressGroupUuid() {
        return addressGroupUuid;
    }

    public void setAddressGroupUuid(String addressGroupUuid) {
        this.addressGroupUuid = addressGroupUuid;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getMetadataClass() {
        return metadataClass;
    }

    public void setMetadataClass(String metadataClass) {
        this.metadataClass = metadataClass;
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
