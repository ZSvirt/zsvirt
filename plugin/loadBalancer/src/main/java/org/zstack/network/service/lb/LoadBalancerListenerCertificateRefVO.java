package org.zstack.network.service.lb;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by shixin on 03/22/2018.
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "listenerUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = CertificateVO.class, myField = "certificateUuid", targetField = "uuid"),
        }
)
public class LoadBalancerListenerCertificateRefVO {
    @Id
    @Column
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerListenerVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String listenerUuid;

    @Column
    @ForeignKey(parentEntityClass = CertificateVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String certificateUuid;

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

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
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
