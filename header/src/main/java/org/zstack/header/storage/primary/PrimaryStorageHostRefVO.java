package org.zstack.header.storage.primary;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;


import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017-05-08.
 */

@Entity
@Table
@BaseResource
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PrimaryStorageVO.class, joinColumn = "primaryStorageUuid"),
        @SoftDeletionCascade(parent = HostVO.class, joinColumn = "hostUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = PrimaryStorageVO.class, myField = "primaryStorageUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class PrimaryStorageHostRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private PrimaryStorageHostStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public PrimaryStorageHostRefVO() {
    }

    public PrimaryStorageHostRefVO(PrimaryStorageHostRefVO vo) {
        this.setId(vo.getId());
        this.setHostUuid(vo.getHostUuid());
        this.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        this.setStatus(vo.getStatus());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public PrimaryStorageHostStatus getStatus() {
        return status;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }
}
