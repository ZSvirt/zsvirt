package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 2018-10-10.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = BaremetalPxeServerVO.class, joinColumn = "pxeServerUuid"),
        @SoftDeletionCascade(parent = ClusterVO.class, joinColumn = "clusterUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = BaremetalPxeServerVO.class, myField = "pxeServerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid")
        }
)
public class BaremetalPxeServerClusterRefVO implements ToInventory {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = BaremetalPxeServerVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String pxeServerUuid;

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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
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
