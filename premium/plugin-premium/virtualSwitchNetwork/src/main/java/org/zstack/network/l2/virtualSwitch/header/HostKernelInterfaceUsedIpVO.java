package org.zstack.network.l2.virtualSwitch.header;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vo.ForeignKey;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class HostKernelInterfaceUsedIpVO extends UsedIpVO {

    @Column
    @ForeignKey(parentEntityClass = HostKernelInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostKernelInterfaceUuid;

    public String getHostKernelInterfaceUuid() {
        return hostKernelInterfaceUuid;
    }

    public void setHostKernelInterfaceUuid(String hostKernelInterfaceUuid) {
        this.hostKernelInterfaceUuid = hostKernelInterfaceUuid;
    }

    public HostKernelInterfaceUsedIpVO() {

    }

    public HostKernelInterfaceUsedIpVO(UsedIpVO vo) {
        super(vo);
    }
}
