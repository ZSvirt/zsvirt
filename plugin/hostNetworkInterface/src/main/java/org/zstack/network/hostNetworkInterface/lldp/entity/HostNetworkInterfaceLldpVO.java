package org.zstack.network.hostNetworkInterface.lldp.entity;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;
import org.zstack.header.vo.Index;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostNetworkInterfaceVO.class, joinColumn = "interfaceUuid")
})
public class HostNetworkInterfaceLldpVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    @Index
    @ForeignKey(parentEntityClass = HostNetworkInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String interfaceUuid;

    @Column
    private String mode;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="uuid",  referencedColumnName = "lldpUuid", insertable=false, updatable=false)
    @NoView
    private HostNetworkInterfaceLldpRefVO neighborDevice;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public HostNetworkInterfaceLldpRefVO getNeighborDevice() {
        return neighborDevice;
    }

    public void setNeighborDevice(HostNetworkInterfaceLldpRefVO neighborDevice) {
        this.neighborDevice = neighborDevice;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
