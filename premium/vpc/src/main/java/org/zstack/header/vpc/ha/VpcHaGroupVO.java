package org.zstack.header.vpc.ha;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.*;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
public class VpcHaGroupVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Transient
    private String accountUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vpcHaRouterUuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcHaGroupMonitorIpVO> monitors = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vpcHaRouterUuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcHaGroupApplianceVmRefVO> vrs = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vpcHaRouterUuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcHaGroupNetworkServiceRefVO> services = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vpcHaRouterUuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcHaGroupVipRefVO> usedIps = new HashSet<>();

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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public Set<VpcHaGroupMonitorIpVO> getMonitors() {
        return monitors;
    }

    public void setMonitors(Set<VpcHaGroupMonitorIpVO> monitors) {
        this.monitors = monitors;
    }

    public Set<VpcHaGroupApplianceVmRefVO> getVrs() {
        return vrs;
    }

    public void setVrs(Set<VpcHaGroupApplianceVmRefVO> vrs) {
        this.vrs = vrs;
    }

    public Set<VpcHaGroupNetworkServiceRefVO> getServices() {
        return services;
    }

    public void setServices(Set<VpcHaGroupNetworkServiceRefVO> services) {
        this.services = services;
    }

    public Set<VpcHaGroupVipRefVO> getUsedIps() {
        return usedIps;
    }

    public void setUsedIps(Set<VpcHaGroupVipRefVO> usedIps) {
        this.usedIps = usedIps;
    }
}
