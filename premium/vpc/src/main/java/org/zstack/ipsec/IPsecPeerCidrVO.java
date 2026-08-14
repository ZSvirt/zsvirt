package org.zstack.ipsec;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2016/11/3.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = IPsecConnectionVO.class, myField = "connectionUuid", targetField = "uuid")
        }
)
public class IPsecPeerCidrVO {
    @Id
    @Column
    private String uuid;
    @Column
    private String cidr;
    @Column
    @ForeignKey(parentEntityClass = IPsecConnectionVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String connectionUuid;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getConnectionUuid() {
        return connectionUuid;
    }

    public void setConnectionUuid(String connectionUuid) {
        this.connectionUuid = connectionUuid;
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
