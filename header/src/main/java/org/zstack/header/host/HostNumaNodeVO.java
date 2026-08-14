package org.zstack.header.host;

import com.google.common.collect.ImmutableMap;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid")
        }
)
public class HostNumaNodeVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    private Integer nodeID;

    @Column
    private String nodeCPUs;

    @Column
    private Long nodeMemSize;

    @Column
    private String nodeDistance;

    public HostNumaNodeVO() {}

    public HostNumaNodeVO(HostNUMANode node) {
        this.nodeCPUs = String.join(",", node.getCpus());
        this.nodeMemSize = node.getSize();
        this.nodeDistance = String.join(",", node.getDistance());
    }

    public Long getId() {
        return id;
    }

    public String getNodeCPUs() {
        return nodeCPUs;
    }

    public void setNodeCPUs(String nodeCPUs) {
        this.nodeCPUs = nodeCPUs;
    }

    public Long getNodeMemSize() {
        return nodeMemSize;
    }

    public void setNodeMemSize(Long nodeMemSize) {
        this.nodeMemSize = nodeMemSize;
    }

    public String getNodeDistance() {
        return nodeDistance;
    }

    public void setNodeDistance(String nodeDistance) {
        this.nodeDistance = nodeDistance;
    }

    public Integer getNodeID() {
        return nodeID;
    }

    public void setNodeID(Integer nodeID) {
        this.nodeID = nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = Integer.valueOf(nodeID);
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setId(Long id) {
        this.id = id;
    }
}