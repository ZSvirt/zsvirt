package org.zstack.network.securitygroup;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VmNicSecurityPolicyVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
})

public class VmNicSecurityPolicyInventory {
    @APINoSee
    private String uuid;
    private String vmNicUuid;
    private String ingressPolicy;
    private String egressPolicy;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected VmNicSecurityPolicyInventory(VmNicSecurityPolicyVO vo) {
        this.setUuid(vo.getUuid());
        this.setVmNicUuid(vo.getVmNicUuid());
        this.setIngressPolicy(vo.getIngressPolicy());
        this.setEgressPolicy(vo.getEgressPolicy());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public VmNicSecurityPolicyInventory() {

    }

    public static VmNicSecurityPolicyInventory valueOf(VmNicSecurityPolicyVO vo) {
        return new VmNicSecurityPolicyInventory(vo);
    }

    public static List<VmNicSecurityPolicyInventory> valueOf(Collection<VmNicSecurityPolicyVO> vos) {
        List<VmNicSecurityPolicyInventory> invs = new ArrayList<VmNicSecurityPolicyInventory>(vos.size());
        for (VmNicSecurityPolicyVO vo : vos) {
            invs.add(VmNicSecurityPolicyInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid= uuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getIngressPolicy() {
        return ingressPolicy;
    }

    public void setIngressPolicy(String ingressPolicy) {
        this.ingressPolicy = ingressPolicy;
    }

    public String getEgressPolicy() {
        return egressPolicy;
    }

    public void setEgressPolicy(String egressPolicy) {
        this.egressPolicy = egressPolicy;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate= createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate= lastOpDate;
    }
}
