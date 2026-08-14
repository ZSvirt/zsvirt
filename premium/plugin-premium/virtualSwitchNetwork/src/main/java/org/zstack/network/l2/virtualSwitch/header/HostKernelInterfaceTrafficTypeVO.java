package org.zstack.network.l2.virtualSwitch.header;

import javax.persistence.*;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = HostKernelInterfaceVO.class, myField = "hostKernelInterfaceUuid", targetField = "uuid"),
        }
)
public class HostKernelInterfaceTrafficTypeVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = HostKernelInterfaceVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostKernelInterfaceUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private HostKernelInterfaceTrafficType trafficType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostKernelInterfaceUuid() {
        return hostKernelInterfaceUuid;
    }

    public void setHostKernelInterfaceUuid(String hostKernelInterfaceUuid) {
        this.hostKernelInterfaceUuid = hostKernelInterfaceUuid;
    }

    public HostKernelInterfaceTrafficType getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(HostKernelInterfaceTrafficType trafficType) {
        this.trafficType = trafficType;
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
