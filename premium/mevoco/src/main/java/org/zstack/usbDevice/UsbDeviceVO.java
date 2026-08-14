package org.zstack.usbDevice;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Created by GuoYi on 10/21/17.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid"),
        @SoftDeletionCascade(parent = VmInstanceEO.class, joinColumn = "vmInstanceUuid")
})
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
        }
)
public class UsbDeviceVO extends UsbDeviceAO implements OwnedByAccount {
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    private UsbDeviceState state;

    @Column
    protected String attachType;

    @Transient
    private String accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public UsbDeviceState getState() {
        return state;
    }

    public void setState(UsbDeviceState state) {
        this.state = state;
    }

    public String getAttachType() {
        return attachType;
    }

    public void setAttachType(String attachType) {
        this.attachType = attachType;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @Override
    public String toString() {
        return "UsbDeviceVO{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", vmInstanceUuid='" + vmInstanceUuid + '\'' +
                ", state=" + state +
                ", busNum=" + busNum +
                ", devNum=" + devNum +
                ", idVendor='" + idVendor + '\'' +
                ", idProduct='" + idProduct + '\'' +
                ", iManufacturer='" + iManufacturer + '\'' +
                ", iProduct='" + iProduct + '\'' +
                ", iSerial='" + iSerial + '\'' +
                ", usbVersion='" + usbVersion + '\'' +
                ", attachType='" + attachType + '\'' +
                ", createDate=" + createDate +
                ", lastOpDate=" + lastOpDate +
                '}';
    }
}
