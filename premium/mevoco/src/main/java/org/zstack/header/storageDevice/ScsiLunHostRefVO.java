package org.zstack.header.storageDevice;

import org.zstack.header.host.HostVO;
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
        @SoftDeletionCascade(parent = ScsiLunVO.class, joinColumn = "scsiLunUuid"),
        @SoftDeletionCascade(parent = HostVO.class, joinColumn = "hostUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = ScsiLunVO.class, myField = "scsiLunUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
public class ScsiLunHostRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = HostVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = ScsiLunVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String scsiLunUuid;

    @Column
    private String hctl;

    @Column
    private String path;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public ScsiLunHostRefVO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getScsiLunUuid() {
        return scsiLunUuid;
    }

    public void setScsiLunUuid(String scsiLunUuid) {
        this.scsiLunUuid = scsiLunUuid;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getHctl() {
        return hctl;
    }

    public void setHctl(String hctl) {
        this.hctl = hctl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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
