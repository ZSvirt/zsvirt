package org.zstack.network.service.lb;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shixin on 03/22/2018.
 */
@Entity
@Table
@BaseResource
/*
@EntityGraph(
        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerCertificateRefVO.class, myField = "uuid", targetField = "certificateUuid")
        }
)
*/
public class CertificateVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String certificate;

    @Column
    private String description;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="certificateUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerCertificateRefVO> listeners = new HashSet<>();

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<LoadBalancerListenerCertificateRefVO> getListeners() {
        return listeners;
    }

    public void setListeners(Set<LoadBalancerListenerCertificateRefVO> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
