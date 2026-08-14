package org.zstack.header.vpc;

import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.vpc.ha.VpcHaGroupApplianceVmRefInventory;
import org.zstack.header.vpc.ha.VpcHaGroupApplianceVmRefVO;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = VpcRouterVmVO.class, collectionValueOfMethod="valueOf3",
        parent = {@Parent(inventoryClass = VirtualRouterVmInventory.class, type = VpcConstants.VPC_VROUTER_VM_TYPE)})
@PythonClassInventory
public class VpcRouterVmInventory extends VirtualRouterVmInventory {
    @Queryable(mappingClass = VpcRouterDnsInventory.class,
            joinColumn = @JoinColumn(name = "vpcRouterUuid"))
    private List<VpcRouterDnsInventory> dns;

    @Queryable(mappingClass = VpcHaGroupApplianceVmRefInventory.class,
            joinColumn = @JoinColumn(name = "uuid"))
    private List<VpcHaGroupApplianceVmRefInventory> haRef;

    protected VpcRouterVmInventory(VpcRouterVmVO vo) {
        super(vo);
        List<VpcRouterDnsVO> dnsVos = Q.New(VpcRouterDnsVO.class).eq(VpcRouterDnsVO_.vpcRouterUuid, vo.getUuid())
                .orderBy(VpcRouterDnsVO_.id, SimpleQuery.Od.ASC).list();
        this.setDns(VpcRouterDnsInventory.valueOf(dnsVos));
        this.haRef = VpcHaGroupApplianceVmRefInventory.valueOf(vo.getHaRef());
    }

    public VpcRouterVmInventory() {
    }

    public static VpcRouterVmInventory valueOf(VpcRouterVmVO vo) {
        return new VpcRouterVmInventory(vo);
    }

    public static List<VpcRouterVmInventory> valueOf3(Collection<VpcRouterVmVO> vos) {
        List<VpcRouterVmInventory> invs = new ArrayList<>();
        for (VpcRouterVmVO vo : vos) {
            invs.add(VpcRouterVmInventory.valueOf(vo));
        }
        return invs;
    }

    public List<VpcRouterDnsInventory> getDns() {
        return dns;
    }

    public void setDns(List<VpcRouterDnsInventory> dns) {
        this.dns = dns;
    }

    public List<VpcHaGroupApplianceVmRefInventory> getHaRef() {
        return haRef;
    }

    public void setHaRef(List<VpcHaGroupApplianceVmRefInventory> haRef) {
        this.haRef = haRef;
    }
}
