package org.zstack.header.vpc;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.header.vpc.ha.VpcHaGroupApplianceVmRefVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = VmInstanceEO.class, needView = false)
@AutoDeleteTag
public class VpcRouterVmVO extends VirtualRouterVmVO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "vpcRouterUuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcRouterDnsVO> dns = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid", insertable = false, updatable = false)
    @NoView
    private Set<VpcHaGroupApplianceVmRefVO> haRef = new HashSet<>();

    public Set<VpcHaGroupApplianceVmRefVO> getHaRef() {
        return haRef;
    }

    public void setHaRef(Set<VpcHaGroupApplianceVmRefVO> haRef) {
        this.haRef = haRef;
    }

    public VpcRouterVmVO(VpcRouterVmVO other) {
        super(other);
        this.dns = other.dns;
    }

    public VpcRouterVmVO(VirtualRouterVmVO other) {
        super(other);
    }

    public VpcRouterVmVO() {
    }

    public Set<VpcRouterDnsVO> getDns() {
        return dns;
    }

    public void setDns(Set<VpcRouterDnsVO> dns) {
        this.dns = dns;
    }
}

