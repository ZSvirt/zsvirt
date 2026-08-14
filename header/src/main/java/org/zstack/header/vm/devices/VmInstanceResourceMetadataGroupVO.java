package org.zstack.header.vm.devices;

import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table
public class VmInstanceResourceMetadataGroupVO implements ToInventory {
    @Id
    @Column
    private String uuid;

    /**
     * vm instance uuid
     */
    @Column
    private String resourceUuid;

    /**
     * vm device address archive that referred by this group
     */
    @Column
    private String vmInstanceUuid;

    @Column
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "addressGroupUuid", insertable = false, updatable = false)
    private List<VmInstanceResourceMetadataArchiveVO> addressList;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public List<VmInstanceResourceMetadataArchiveVO> getAddressList() {
        return addressList;
    }

    public void setAddressList(List<VmInstanceResourceMetadataArchiveVO> addressList) {
        this.addressList = addressList;
    }
}
