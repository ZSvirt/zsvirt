package org.zstack.network.service.vip;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by weiwang on 29/10/2017
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = VipVO.class, joinColumn = "vipUuid"),
        @SoftDeletionCascade(parent = L3NetworkVO.class, joinColumn = "l3NetworkUuid")
})
@IdClass(CompositePrimaryKeyForVipPeerL3NetworkRefVO.class)
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
        }
)
public class VipPeerL3NetworkRefVO {
    @Column
    @Id
    @ForeignKey(parentEntityClass = VipVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    @Id
    @ForeignKey(parentEntityClass = L3NetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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
