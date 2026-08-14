package org.zstack.network.securitygroup;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = SecurityGroupVO.class, myField = "securityGroupUuid", targetField = "uuid")
        }
)
public class SecurityGroupRuleVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = SecurityGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String securityGroupUuid;
    
    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleType type;

    @Column
    private Integer ipVersion;

    @Column
    private int priority;

    @Column
    private String description;

    @Column
    private String action;

    @Column
    private String srcIpRange;

    @Column
    private String dstIpRange;

    @Column
    private String srcPortRange;

    @Column
    private String dstPortRange;
    
    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleProtocolType protocol;

    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupRuleState state = SecurityGroupRuleState.Enabled;

    @Column
    @ForeignKey(parentEntityClass = SecurityGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String remoteSecurityGroupUuid;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @Column
    private int startPort;
    
    @Column
    private int endPort;

    @Column
    private String allowedCidr;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public int getPriority() {
        return priority;
    }
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

   public void setDescription(String description) {
        this.description = description;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public void setDstIpRange(String dstIprange) {
        this.dstIpRange = dstIprange;
    }

    public String getSrcPortRange() {
        return srcPortRange;
    }

    public void setSrcPortRange(String srcPortRange) {
        this.srcPortRange = srcPortRange;
    }

    public String getDstPortRange() {
        return dstPortRange;
    }

    public void setDstPortRange(String dstPortRange) {
        this.dstPortRange = dstPortRange;
    }

    public SecurityGroupRuleState getState() {
        return state;
    }

    public void setState(SecurityGroupRuleState state) {
        this.state = state;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public SecurityGroupRuleType getType() {
        return type;
    }

    public void setType(SecurityGroupRuleType type) {
        this.type = type;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }
    
    public SecurityGroupRuleProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(SecurityGroupRuleProtocolType protocol) {
        this.protocol = protocol;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
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
}
