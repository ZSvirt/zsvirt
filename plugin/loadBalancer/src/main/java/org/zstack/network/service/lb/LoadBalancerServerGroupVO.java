package org.zstack.network.service.lb;

import org.zstack.core.db.Q;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = LoadBalancerVO.class, myField = "loadBalancerUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = LoadBalancerListenerServerGroupRefVO.class, myField = "uuid", targetField = "serverGroupUuid"),
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupServerIpVO.class, myField = "uuid", targetField = "serverGroupUuid"),
                @EntityGraph.Neighbour(type = LoadBalancerServerGroupVmNicRefVO.class, myField = "uuid", targetField = "serverGroupUuid"),
        }
)
public class LoadBalancerServerGroupVO extends ResourceVO  implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = LoadBalancerVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String loadBalancerUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "serverGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<LoadBalancerListenerServerGroupRefVO> loadBalancerListenerServerGroupRefs = new HashSet<LoadBalancerListenerServerGroupRefVO>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "serverGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<LoadBalancerServerGroupServerIpVO> loadBalancerServerGroupServerIps = new HashSet<LoadBalancerServerGroupServerIpVO>();


    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "serverGroupUuid", insertable = false, updatable = false)
    @NoView
    private Set<LoadBalancerServerGroupVmNicRefVO> loadBalancerServerGroupVmNicRefs = new HashSet<LoadBalancerServerGroupVmNicRefVO>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }


    public Set<LoadBalancerListenerServerGroupRefVO> getLoadBalancerListenerServerGroupRefs() {
        return loadBalancerListenerServerGroupRefs;
    }

    public void setLoadBalancerListenerServerGroupRefs(Set<LoadBalancerListenerServerGroupRefVO> loadBalancerListenerServerGroupRefs) {
        this.loadBalancerListenerServerGroupRefs = loadBalancerListenerServerGroupRefs;
    }

    public Set<LoadBalancerServerGroupServerIpVO> getLoadBalancerServerGroupServerIps() {
        return loadBalancerServerGroupServerIps;
    }

    public void setLoadBalancerServerGroupServerIps(Set<LoadBalancerServerGroupServerIpVO> loadBalancerServerGroupServerIps) {
        this.loadBalancerServerGroupServerIps = loadBalancerServerGroupServerIps;
    }

    public Set<LoadBalancerServerGroupVmNicRefVO> getLoadBalancerServerGroupVmNicRefs() {
        return loadBalancerServerGroupVmNicRefs;
    }

    public void setLoadBalancerServerGroupVmNicRefs(Set<LoadBalancerServerGroupVmNicRefVO> loadBalancerServerGroupVmNicRefs) {
        this.loadBalancerServerGroupVmNicRefs = loadBalancerServerGroupVmNicRefs;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
}
    