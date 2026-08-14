package org.zstack.storage.device.iscsi;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/8/1
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = IscsiServerVO.class, joinColumn = "iscsiServerUuid"),
        @SoftDeletionCascade(parent = ClusterVO.class, joinColumn = "clusterUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = IscsiServerVO.class, myField = "iscsiServerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ClusterVO.class, myField = "clusterUuid", targetField = "uuid"),
        }
)
public class IscsiServerClusterRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = IscsiServerVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String iscsiServerUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getIscsiServerUuid() {
        return iscsiServerUuid;
    }

    public void setIscsiServerUuid(String iscsiServerUuid) {
        this.iscsiServerUuid = iscsiServerUuid;
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
