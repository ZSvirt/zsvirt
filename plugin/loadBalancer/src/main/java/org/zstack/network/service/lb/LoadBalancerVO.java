package org.zstack.network.service.lb;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.network.service.vip.VipVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 8/7/2015.
 */
@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VipVO.class, myField = "vipUuid", targetField = "uuid"),
        },

        friends = {
                @EntityGraph.Neighbour(type = LoadBalancerListenerVO.class, myField = "loadBalancerUuid", targetField = "uuid"),
        }
)
public class LoadBalancerVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String providerType;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerState state;

    @Column
    @Enumerated(EnumType.STRING)
    private LoadBalancerType type;

    @Column
    @ForeignKey(parentEntityClass = VipVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String vipUuid;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerServerGroupVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String serverGroupUuid;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="loadBalancerUuid", insertable=false, updatable=false)
    @NoView
    private Set<LoadBalancerListenerVO> listeners = new HashSet<LoadBalancerListenerVO>();

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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


    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Set<LoadBalancerListenerVO> getListeners() {
        return listeners;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public void setListeners(Set<LoadBalancerListenerVO> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LoadBalancerState getState() {
        return state;
    }

    public void setState(LoadBalancerState state) {
        this.state = state;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
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

    public LoadBalancerType getType() {
        return type;
    }

    public void setType(LoadBalancerType type) {
        this.type = type;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
}
