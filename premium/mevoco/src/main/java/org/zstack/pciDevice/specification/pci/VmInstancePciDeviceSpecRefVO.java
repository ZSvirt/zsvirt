package org.zstack.pciDevice.specification.pci;

import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;
import org.zstack.pciDevice.PciDeviceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2019-05-23.
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmInstanceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceSpecVO.class, myField = "pciSpecUuid", targetField = "uuid"),
        }
)
public class VmInstancePciDeviceSpecRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = PciDeviceSpecVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String pciSpecUuid;

    @Column
    private int pciDeviceNumber = 1;

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

    public int getPciDeviceNumber() {
        return pciDeviceNumber;
    }

    public void setPciDeviceNumber(int pciDeviceNumber) {
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
