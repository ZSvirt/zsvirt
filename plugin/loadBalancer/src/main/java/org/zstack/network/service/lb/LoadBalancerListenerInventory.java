package org.zstack.network.service.lb;

import org.zstack.core.db.Q;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerListenerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "loadBalancer", inventoryClass = LoadBalancerInventory.class,
                foreignKey = "loadBalancerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "vmNicRef", inventoryClass = LoadBalancerListenerVmNicRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "listenerUuid", hidden = true),
        @ExpandedQuery(expandedField = "serverGroupRef", inventoryClass = LoadBalancerListenerServerGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "listenerUuid", hidden = true),
        @ExpandedQuery(expandedField = "certificate", inventoryClass = LoadBalancerListenerCertificateRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "listenerUuid")
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "vmNic", expandedField = "vmNicRef.vmNic"),
        @ExpandedQueryAlias(alias = "serverGroup", expandedField = "serverGroupRef.group"),
})
public class LoadBalancerListenerInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String loadBalancerUuid;
    private Integer instancePort;
    private Integer loadBalancerPort;
    private String securityPolicyType;
    private String protocol;
    private String serverGroupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<LoadBalancerListenerVmNicRefInventory> vmNicRefs;
    private List<LoadBalancerListenerACLRefInventory> aclRefs;
    private List<LoadBalancerListenerCertificateRefInventory> certificateRefs;
    private List<LoadBalancerListenerServerGroupRefInventory> serverGroupRefs;

    public static LoadBalancerListenerInventory valueOf(LoadBalancerListenerVO vo) {
        LoadBalancerListenerInventory inv = new LoadBalancerListenerInventory();
        inv.setUuid(vo.getUuid());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setInstancePort(vo.getInstancePort());
        inv.setLoadBalancerPort(vo.getLoadBalancerPort());
        inv.setProtocol(vo.getProtocol());
        inv.setSecurityPolicyType(vo.getSecurityPolicyType());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setServerGroupUuid(vo.getServerGroupUuid());

        List<LoadBalancerServerGroupVmNicRefVO> serverGroupVmNicRefVOs
                = Q.New(LoadBalancerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerServerGroupVmNicRefVO_.serverGroupUuid,vo.getServerGroupUuid())
                .list();
        Set<LoadBalancerListenerVmNicRefVO> vmNicRefs = new HashSet<>();
        for(LoadBalancerServerGroupVmNicRefVO serverGroupVmNicRefVO: serverGroupVmNicRefVOs){
            LoadBalancerListenerVmNicRefVO vmNicRefVO = new LoadBalancerListenerVmNicRefVO();
            vmNicRefVO.setId(serverGroupVmNicRefVO.getId());
            vmNicRefVO.setListenerUuid(vo.getUuid());
            vmNicRefVO.setCreateDate(serverGroupVmNicRefVO.getCreateDate());
            vmNicRefVO.setLastOpDate(serverGroupVmNicRefVO.getLastOpDate());
            vmNicRefVO.setStatus(serverGroupVmNicRefVO.getStatus());
            vmNicRefVO.setVmNicUuid(serverGroupVmNicRefVO.getVmNicUuid());
            vmNicRefs.add(vmNicRefVO);
        }
        vo.setVmNicRefs(vmNicRefs);

        inv.setVmNicRefs(LoadBalancerListenerVmNicRefInventory.valueOf(vo.getVmNicRefs()));
        inv.setAclRefs(LoadBalancerListenerACLRefInventory.valueOf(vo.getAclRefs()));
        inv.setCertificateRefs(LoadBalancerListenerCertificateRefInventory.valueOf(vo.getCertificateRefs()));
        inv.setServerGroupRefs(LoadBalancerListenerServerGroupRefInventory.valueOf(vo.getServerGroupRefs()));
        return inv;
    }

    public static List<LoadBalancerListenerInventory> valueOf(Collection<LoadBalancerListenerVO> vos) {
        List<LoadBalancerListenerInventory> invs = new ArrayList<LoadBalancerListenerInventory>();
        for (LoadBalancerListenerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public List<LoadBalancerListenerVmNicRefInventory> getVmNicRefs() {
        return vmNicRefs;
    }

    public void setVmNicRefs(List<LoadBalancerListenerVmNicRefInventory> vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }

    public List<LoadBalancerListenerACLRefInventory> getAclRefs() {
        return aclRefs;
    }

    public void setAclRefs(List<LoadBalancerListenerACLRefInventory> aclRefs) {
        this.aclRefs = aclRefs;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSecurityPolicyType() {
        return securityPolicyType;
    }

    public void setSecurityPolicyType(String securityPolicyType) {
        this.securityPolicyType = securityPolicyType;
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

    public List<LoadBalancerListenerCertificateRefInventory> getCertificateRefs() {
        return certificateRefs;
    }

    public void setCertificateRefs(List<LoadBalancerListenerCertificateRefInventory> certificateRefs) {
        this.certificateRefs = certificateRefs;
    }

    public void setInstancePort(Integer instancePort) {
        this.instancePort = instancePort;
    }

    public void setLoadBalancerPort(Integer loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public List<LoadBalancerListenerServerGroupRefInventory> getServerGroupRefs() {
        return serverGroupRefs;
    }

    public void setServerGroupRefs(List<LoadBalancerListenerServerGroupRefInventory> serverGroupRefs) {
        this.serverGroupRefs = serverGroupRefs;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
}
