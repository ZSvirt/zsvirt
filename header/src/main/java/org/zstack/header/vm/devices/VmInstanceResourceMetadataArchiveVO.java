package org.zstack.header.vm.devices;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class VmInstanceResourceMetadataArchiveVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * uuid of vm device
     */
    @Column
    private String resourceUuid;

    /**
     * vm instance uuid that the resourceUuid related resource attached
     */
    @Column
    private String vmInstanceUuid;

    /**
     * pciAddress used to store a string format by PciAddressConfig
     */
    @Column
    private String deviceAddress;

    /**
     * normally a json string of resource inventory
     */
    @Column
    private String metadata;

    /**
     * canonical name of metadata
     */
    @Column
    private String metadataClass;

    /**
     * use to mark a group of vm device archives
     */
    @Column
    private String addressGroupUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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
}
