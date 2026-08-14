package org.zstack.pciDevice;

import org.zstack.header.configuration.InstanceOfferingEO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;

import javax.persistence.*;
import javax.persistence.ForeignKey;

/**
 * Created by weiwang on 07/07/2017.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PciDeviceVO.class, joinColumn = "pciDeviceUuid"),
        @SoftDeletionCascade(parent = PciDeviceOfferingVO.class, joinColumn = "pciDeviceOfferingUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = PciDeviceVO.class, myField = "pciDeviceUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceOfferingVO.class, myField = "pciDeviceOfferingUuid", targetField = "uuid"),
        }
)
public class PciDevicePciDeviceOfferingRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = PciDeviceVO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String pciDeviceUuid;

    @Column
    @org.zstack.header.vo.ForeignKey(parentEntityClass = PciDeviceOfferingVO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String pciDeviceOfferingUuid;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPciDeviceUuid() {
        return pciDeviceUuid;
    }

    public void setPciDeviceUuid(String pciDeviceUuid) {
        this.pciDeviceUuid = pciDeviceUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }
}
