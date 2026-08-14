package org.zstack.header.host;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostNetworkBondingVO.class, joinColumn = "bondingUuid")
})
public class HostNetworkBondingServiceRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = HostNetworkBondingVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String bondingUuid;

    @Column
    private Integer vlanId;

    @Column
    @Enumerated(EnumType.STRING)
    private HostNetworkInterfaceServiceType serviceType;

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

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public Integer getVlanId() {
        return vlanId;
    }

    public void setVlanId(Integer vlanId) {
        this.vlanId = vlanId;
    }

    public HostNetworkInterfaceServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(HostNetworkInterfaceServiceType serviceType) {
        this.serviceType = serviceType;
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
