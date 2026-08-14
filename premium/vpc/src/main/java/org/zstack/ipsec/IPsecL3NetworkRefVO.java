package org.zstack.ipsec;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by shixin on 11/21/2017
 */
@Entity
@Table
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = IPsecConnectionVO.class, myField = "connectionUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid"),
        }
)
public class IPsecL3NetworkRefVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = IPsecConnectionVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String connectionUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkVO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public void setConnectionUuid(String connectionUuid) {
        this.connectionUuid = connectionUuid;
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
