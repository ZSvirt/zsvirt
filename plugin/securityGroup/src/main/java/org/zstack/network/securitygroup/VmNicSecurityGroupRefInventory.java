package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VmNicSecurityGroupRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "securityGroup", inventoryClass = SecurityGroupInventory.class,
                foreignKey = "securityGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VmNicInventory.class, expandedField = "securityGroupRef", inventoryClass = VmNicSecurityGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "vmNicUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = VmNicInventory.class, alias = "securityGroup", expandedField = "securityGroupRef.securityGroup")
})
public class VmNicSecurityGroupRefInventory {
    @APINoSee
    private String uuid;
    private Integer priority;
    private String vmNicUuid;
    private String securityGroupUuid;
    private String vmInstanceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    protected VmNicSecurityGroupRefInventory(VmNicSecurityGroupRefVO vo) {
        this.setUuid(vo.getUuid());
        this.setPriority(vo.getPriority());
        this.setVmNicUuid(vo.getVmNicUuid());
        this.setSecurityGroupUuid(vo.getSecurityGroupUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setVmInstanceUuid(vo.getVmInstanceUuid());
    }
    
    public VmNicSecurityGroupRefInventory() {
    }
    
    public static VmNicSecurityGroupRefInventory valueOf(VmNicSecurityGroupRefVO vo) {
        return new VmNicSecurityGroupRefInventory(vo);
    }
    
    public static List<VmNicSecurityGroupRefInventory> valueOf(Collection<VmNicSecurityGroupRefVO> vos) {
        List<VmNicSecurityGroupRefInventory> invs = new ArrayList<VmNicSecurityGroupRefInventory>(vos.size());
        for (VmNicSecurityGroupRefVO vo : vos) {
            invs.add(VmNicSecurityGroupRefInventory.valueOf(vo));
        }
        return invs;
    }
    
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }
    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }
    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
