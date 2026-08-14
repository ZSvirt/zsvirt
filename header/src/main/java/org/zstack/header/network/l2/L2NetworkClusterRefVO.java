package org.zstack.header.network.l2;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass = L2NetworkVO.class, foreignVOJoinColumn = "l2NetworkUuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = L2NetworkVO.class, joinColumn = "l2NetworkUuid"),
        @SoftDeletionCascade(parent = ClusterVO.class, joinColumn = "clusterUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = L2NetworkVO.class, myField = "l2NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid"),
        }
)
public class L2NetworkClusterRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    @Column
    private String l2ProviderType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getL2ProviderType() {
        return l2ProviderType;
    }

    public void setL2ProviderType(String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }

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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
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
