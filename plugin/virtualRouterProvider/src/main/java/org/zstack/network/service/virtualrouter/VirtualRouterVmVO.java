package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmInstanceEO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = VmInstanceEO.class, needView = false)
@AutoDeleteTag
public class VirtualRouterVmVO extends ApplianceVmVO {
    private String publicNetworkUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "virtualRouterVmUuid", insertable = false, updatable = false)
    @NoView
    private Set<VirtualRouterVipVO> virtualRouterVips = new HashSet<>();

    public VirtualRouterVmVO(VirtualRouterVmVO other) {
        super(other);
        this.publicNetworkUuid = other.publicNetworkUuid;
        this.virtualRouterVips = other.virtualRouterVips;
    }

    public VirtualRouterVmVO(ApplianceVmVO other) {
        super(other);
    }

    public VirtualRouterVmVO() {
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public void setPublicNetworkUuid(String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }

    public Set<VirtualRouterVipVO> getVirtualRouterVips() {
        return virtualRouterVips;
    }

    public void setVirtualRouterVips(Set<VirtualRouterVipVO> virtualRouterVips) {
        this.virtualRouterVips = virtualRouterVips;
    }

    public List<UsedIpVO> getUsedIps() {
        List<UsedIpVO> ret = new ArrayList<>();
        for (VmNicVO nic : getVmNics()) {
            ret.addAll(nic.getUsedIps());
        }
        return ret;
    }
}
