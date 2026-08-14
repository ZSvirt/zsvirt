package org.zstack.header.network.l2;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@TriggerIndex
@SqlTrigger(foreignVOClass = L2NetworkVO.class, foreignVOJoinColumn = "l2NetworkUuid")
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = L2NetworkVO.class, joinColumn = "l2NetworkUuid"),
        @SoftDeletionCascade(parent = HostVO.class, joinColumn = "hostUuid")
})
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = L2NetworkVO.class, myField = "l2NetworkUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = HostVO.class, myField = "hostUuid", targetField = "uuid"),
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
public class L2NetworkHostRefVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @ForeignKey(parentEntityClass = L2NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l2NetworkUuid;

    @Column
    private String l2ProviderType;

    @Column
    private String bridgeName;

    @Column
    private boolean skipDeletion;

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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public boolean getSkipDeletion() {
        return skipDeletion;
    }

    public void setSkipDeletion(boolean skipDeletion) {
        this.skipDeletion = skipDeletion;
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

    @Override
    public String toString() {
        return String.format("L2NetworkHostRefVO[hostUuid:%s, l2NetworkUuid:%s, l2ProviderType:%s, bridgeName:%s]",
                hostUuid, l2NetworkUuid, l2ProviderType, bridgeName);
    }
}
