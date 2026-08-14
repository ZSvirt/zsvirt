package org.zstack.header.vm;


import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VmInstanceVO.class, myField = "vmUuid", targetField = "uuid")
        }
)
public class VmInstanceNumaNodeVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vmUuid;

    @Column
    private Integer vNodeID;

    @Column
    private String vNodeCPUs;

    @Column
    private Long vNodeMemSize;

    @Column
    private String vNodeDistance;

    @Column
    private Integer pNodeID;

    public VmInstanceNumaNodeVO() {}

    public Long getId() {
        return id;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public Integer getvNodeID() {
        return vNodeID;
    }

    public void setvNodeID(Integer vNodeID) {
        this.vNodeID = vNodeID;
    }

    public String getvNodeCPUs() {
        return vNodeCPUs;
    }

    public void setvNodeCPUs(String vNodeCPUs) {
        this.vNodeCPUs = vNodeCPUs;
    }

    public Long getvNodeMemSize() {
        return vNodeMemSize;
    }

    public void setvNodeMemSize(Long vNodeMemSize) {
        this.vNodeMemSize = vNodeMemSize;
    }

    public String getvNodeDistance() {
        return vNodeDistance;
    }

    public void setvNodeDistance(String vNodeDistance) {
        this.vNodeDistance = vNodeDistance;
    }

    public Integer getpNodeID() {
        return pNodeID;
    }

    public void setpNodeID(Integer pNodeID) {
        this.pNodeID = pNodeID;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
