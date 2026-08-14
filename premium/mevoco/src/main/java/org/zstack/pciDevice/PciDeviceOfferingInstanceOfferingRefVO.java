package org.zstack.pciDevice;

import org.zstack.header.configuration.InstanceOfferingEO;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;

/**
 * Created by weiwang on 07/07/2017.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = InstanceOfferingEO.class, joinColumn = "instanceOfferingUuid"),
        @SoftDeletionCascade(parent = PciDeviceOfferingVO.class, joinColumn = "pciDeviceOfferingUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = InstanceOfferingVO.class, myField = "instanceOfferingUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PciDeviceOfferingVO.class, myField = "pciDeviceOfferingUuid", targetField = "uuid"),
        }
)
public class PciDeviceOfferingInstanceOfferingRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = InstanceOfferingEO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String instanceOfferingUuid;

    @Column
    @ForeignKey(parentEntityClass = PciDeviceOfferingVO.class, onDeleteAction = org.zstack.header.vo.ForeignKey.ReferenceOption.CASCADE)
    private String pciDeviceOfferingUuid;

    @Column
    private String metadata;

    @Column
    private Integer pciDeviceCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }

    public String getMetadata() {
        return metadata;
    }

    public PciDeviceMetaData getPciDeviceMetaData() {
        return new PciDeviceMetaData(this.getMetadata());
    }

    public void setPciDeviceMetaData(PciDeviceMetaData data) {
        this.setMetadata(data.toString());
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Integer getPciDeviceCount() {
        return pciDeviceCount;
    }

    public void setPciDeviceCount(Integer pciDeviceCount) {
        this.pciDeviceCount = pciDeviceCount;
    }
}
