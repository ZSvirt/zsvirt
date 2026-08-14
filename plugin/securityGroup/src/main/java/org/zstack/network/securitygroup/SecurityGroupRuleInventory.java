package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory
 * inventory for security group rule
 *
 * @category security group
 *
 * @example
 * {
"uuid": "02bc62abee88444ca3e2c434a1b8fdea",
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"type": "Ingress",
"priority": 1,
"ipVersion": 4,
"protocol": "TCP",
"srcIpRange": "10.10.10.1,10.10.10.2",
"dstIpRange": "20.20.20.1,20.20.20.1",
"srcPortRange": "10,20",
"dstPortRange": "30,40",
"action": "RETURN",
"state": "Enabled,
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"createDate": "May 14, 2014 9:38:24 PM",
"lastOpDate": "May 14, 2014 9:38:24 PM"
}
 *
 * @since 0.1.0
 */
@Inventory(mappingVOClass = SecurityGroupRuleVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "securityGroup", inventoryClass = SecurityGroupInventory.class,
                foreignKey = "securityGroupUuid", expandedInventoryKey = "uuid")
})
public class SecurityGroupRuleInventory {
    /**
     * @desc rule uuid
     */
    private String uuid;
    /**
     * @desc security group uuid
     */
    private String securityGroupUuid;
    /**
     * @desc
     * rule type
     *
     * - Ingress: for inbound traffic
     * - Egress: for outbound traffic
     * @choices
     * - Ingress
     * - Egress
     */
    private String type;

    private Integer ipVersion;

    /**
     * @desc
     * network protocol type
     * @choices
     * - TCP
     * - UDP
     * - ICMP
     */
    private String protocol;

    private String state;

    private Integer priority;

    private String description;

    /**
     * @desc source ip address range
     * @choices 10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24
     */
    private String srcIpRange;

    /**
     * @desc destination ip address range
     * @choices 10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24
     */
    private String dstIpRange;

    /**
     * @desc source ip port range
     * @choices 1000,1001,1002-1005,1008
     */
    private String srcPortRange;

    /**
     * @desc destination ip port range
     * @choices 1000,1001,1002-1005,1008
     */
    private String dstPortRange;

        /**
     * @desc rule default target
     * @choices
     * - RETURN / DROP
     */
    private String action;

    /**
     * @desc remote security group uuids for rules between groups
     */
    private String remoteSecurityGroupUuid;
    /**
     * @desc the time this resource gets created
     */

    private String allowedCidr;

    private Integer startPort;
 
    private Integer endPort;

    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public SecurityGroupRuleInventory() {
    }
    
    protected SecurityGroupRuleInventory(SecurityGroupRuleVO vo) {
        this.setUuid(vo.getUuid());
        this.setSecurityGroupUuid(vo.getSecurityGroupUuid());
        this.setState(vo.getState().toString());
        this.setRemoteSecurityGroupUuid(vo.getRemoteSecurityGroupUuid());
        this.setDescription(vo.getDescription());
        this.setType(vo.getType().toString());
        this.setIpVersion(vo.getIpVersion());
        this.setProtocol(vo.getProtocol().toString());
        this.setPriority(vo.getPriority());
        this.setSrcIpRange(vo.getSrcIpRange());
        this.setDstIpRange(vo.getDstIpRange());
        this.setSrcPortRange(vo.getSrcPortRange());
        this.setDstPortRange(vo.getDstPortRange());
        this.setAction(vo.getAction().toString());
        this.setAllowedCidr(vo.getAllowedCidr());
        this.setStartPort(vo.getStartPort());
        this.setEndPort(vo.getEndPort());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }
    
    public static SecurityGroupRuleInventory valueOf(SecurityGroupRuleVO vo) {
        SecurityGroupRuleInventory inv = new SecurityGroupRuleInventory(vo);
        return inv;
    }
    
    public static List<SecurityGroupRuleInventory> valueOf(Collection<SecurityGroupRuleVO> vos) {
        List<SecurityGroupRuleInventory> invs = new ArrayList<SecurityGroupRuleInventory>(vos.size());
        for (SecurityGroupRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getSrcIpRange() {
        return srcIpRange;
    }

    public void setSrcIpRange(String srcIpRange) {
        this.srcIpRange = srcIpRange;
    }

    public String getDstIpRange() {
        return dstIpRange;
    }

    public void setDstIpRange(String dstIpRange) {
        this.dstIpRange = dstIpRange;
    }

    public String getDstPortRange() {
        return dstPortRange;
    }

    public String getSrcPortRange() {
        return srcPortRange;
    }

    public void setSrcPortRange(String srcPortRange) {
        this.srcPortRange = srcPortRange;
    }

    public void setDstPortRange(String dstPortRange) {
        this.dstPortRange = dstPortRange;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }

    public String getRemoteSecurityGroupUuid() {
        return remoteSecurityGroupUuid;
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

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("uuid: %s,", this.uuid));
        sb.append(String.format("securityGroupUuid: %s,", this.securityGroupUuid));
        sb.append(String.format("remoteSecurityGroupUuid: %s,", this.remoteSecurityGroupUuid));
        sb.append(String.format("description: %s,", this.description));
        sb.append(String.format("type: %s,", this.type));
        sb.append(String.format("priority: %d,", this.priority));
        sb.append(String.format("ipVersion: %d,", this.ipVersion));
        sb.append(String.format("protocol: %s,", this.protocol));
        sb.append(String.format("srcIpRange: %s,", this.srcIpRange));
        sb.append(String.format("dstIpRange: %s,", this.dstIpRange));
        sb.append(String.format("srcPortRange: %s,", this.srcPortRange));
        sb.append(String.format("dstPortRange: %s,", this.dstPortRange));
        sb.append(String.format("action: %s,", this.action));
        sb.append(String.format("allowedCidr: %s", this.allowedCidr));
        sb.append(String.format("startPort: %s", this.startPort));
        sb.append(String.format("endPort: %s", this.endPort));
        return sb.toString();
    }
}
