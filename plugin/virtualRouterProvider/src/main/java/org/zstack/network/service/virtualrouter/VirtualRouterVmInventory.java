package org.zstack.network.service.virtualrouter;

import org.zstack.appliancevm.ApplianceVmInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefInventory;
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefInventory;
import org.zstack.network.service.virtualrouter.portforwarding.VirtualRouterPortForwardingRuleRefInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipInventory;
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

@Inventory(mappingVOClass = VirtualRouterVmVO.class, collectionValueOfMethod="valueOf2",
        parent = {@Parent(inventoryClass = ApplianceVmInventory.class, type = VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE)})
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "virtualRouterEipRef", expandedInventoryKey = "virtualRouterVmUuid",
                inventoryClass = VirtualRouterEipRefInventory.class, foreignKey = "uuid", hidden = true),
        @ExpandedQuery(expandedField = "virtualRouterVipRef", expandedInventoryKey = "virtualRouterVmUuid",
                inventoryClass = VirtualRouterVipInventory.class, foreignKey = "uuid", hidden = true),
        @ExpandedQuery(expandedField = "virtualRouterPortforwardingRef", expandedInventoryKey = "virtualRouterVmUuid",
                inventoryClass = VirtualRouterPortForwardingRuleRefInventory.class, foreignKey = "uuid", hidden = true),
        @ExpandedQuery(expandedField = "virtualRouterOffering", expandedInventoryKey = "uuid",
                inventoryClass = VirtualRouterOfferingInventory.class, foreignKey = "instanceOfferingUuid"),
        @ExpandedQuery(expandedField = "virtualRouterLoadBalancerListenerRef", expandedInventoryKey = "virtualRouterVmUuid",
                inventoryClass = VirtualRouterLoadBalancerRefInventory.class, foreignKey = "uuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "eip", expandedField = "virtualRouterEipRef.eip"),
        @ExpandedQueryAlias(alias = "vip", expandedField = "virtualRouterVipRef.vip"),
        @ExpandedQueryAlias(alias = "portForwarding", expandedField = "virtualRouterPortforwardingRef.portForwarding"),
        @ExpandedQueryAlias(alias = "loadBalancer", expandedField = "virtualRouterLoadBalancerListenerRef.loadBalancer"),
})
public class VirtualRouterVmInventory extends ApplianceVmInventory {
    private String publicNetworkUuid;

    @Queryable(mappingClass = VirtualRouterVipInventory.class,
            joinColumn = @JoinColumn(name = "virtualRouterVmUuid", referencedColumnName = "uuid"))
    private List<String> virtualRouterVips;

    protected VirtualRouterVmInventory(VirtualRouterVmVO vo) {
        super(vo);
        publicNetworkUuid = vo.getPublicNetworkUuid();
        virtualRouterVips = new ArrayList<>();
        for (VirtualRouterVipVO ref : vo.getVirtualRouterVips()) {
            virtualRouterVips.add(ref.getUuid());
        }
    }

    public VirtualRouterVmInventory() {
    }

    public static VirtualRouterVmInventory valueOf(VirtualRouterVmVO vo) {
        return new VirtualRouterVmInventory(vo);
    }

    public static List<VirtualRouterVmInventory> valueOf2(Collection<VirtualRouterVmVO> vos) {
        List<VirtualRouterVmInventory> invs = new ArrayList<VirtualRouterVmInventory>();
        for (VirtualRouterVmVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public void setPublicNetworkUuid(String publicNetworkUuid) {
        this.publicNetworkUuid = publicNetworkUuid;
    }

    public VmNicInventory getPublicNic() {
        if (getVmNics() == null) {
            return null;
        }

        for (VmNicInventory n : getVmNics()) {
            if (VirtualRouterNicMetaData.isPublicNic(n)) {
                return n;
            }
        }

        return null;
    }

    @Deprecated
    public VmNicInventory getGuestNic() {
        if (getVmNics() == null) {
            return null;
        }

        for (VmNicInventory n : getVmNics()) {
            if (VirtualRouterNicMetaData.isGuestNic(n)) {
                return n;
            }
        }

        return null;
    }

    public List<VmNicInventory> getGuestNics() {
        List<VmNicInventory> guestNics = new ArrayList<>();
        if (getVmNics() == null) {
            return guestNics;
        }

        for (VmNicInventory n : getVmNics()) {
            if (VirtualRouterNicMetaData.isGuestNic(n)) {
                guestNics.add(n);
            }
        }

        return guestNics;
    }

    public List<VmNicInventory> getAdditionalPublicNics() {
        List<VmNicInventory> nics = new ArrayList<>();
        if (getVmNics() == null) {
            return nics;
        }

        for (VmNicInventory n : getVmNics()) {
            if (VirtualRouterNicMetaData.isAddinitionalPublicNic(n)) {
                nics.add(n);
            }
        }

        return nics;
    }

    public VmNicInventory getGuestNicByL3NetworkUuid(String l3uuid) {
        for (VmNicInventory nic : getVmNics()) {
            if (l3uuid.equals(nic.getL3NetworkUuid())) {
                return nic;
            }
        }

        return null;
    }

    public List<String> getAllL3Networks() {
        List<String> l3s = new ArrayList<String>();
        for (VmNicInventory nic : getVmNics()) {
            l3s.add(nic.getL3NetworkUuid());
        }
        return l3s;
    }

    public List<String> getGuestL3Networks() {
        return transformAndRemoveNull(getVmNics(), arg -> VirtualRouterNicMetaData.isGuestNic(arg) ? arg.getL3NetworkUuid() : null);
    }

    public List<String> getVirtualRouterVips() {
        return virtualRouterVips;
    }

    public void setVirtualRouterVips(List<String> virtualRouterVips) {
        this.virtualRouterVips = virtualRouterVips;
    }

    public List<UsedIpInventory> getUsedIps() {
        List<UsedIpInventory> ret = new ArrayList<>();
        for (VmNicInventory nic : getVmNics()) {
            ret.addAll(nic.getUsedIps());
        }
        return ret;
    }
}
