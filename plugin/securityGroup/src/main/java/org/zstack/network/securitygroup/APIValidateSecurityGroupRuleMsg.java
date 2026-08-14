package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
    path = "/security-groups/{securityGroupUuid}/rules/validation",
    method = HttpMethod.GET,
    responseClass = APIValidateSecurityGroupRuleReply.class
)
public class APIValidateSecurityGroupRuleMsg extends APISyncCallMessage {

    @APIParam(resourceType = SecurityGroupVO.class, required = true, nonempty = true)
    private String securityGroupUuid;

    @APIParam(required = true, nonempty = true, validValues = {"Ingress", "Egress"})
    private String type;

    @APIParam(required = true, nonempty = true, validValues = {"TCP", "UDP", "ICMP", "ALL"})
    private String protocol;

    @APIParam(resourceType = SecurityGroupVO.class, required = false, nonempty = true)
    private String remoteSecurityGroupUuid;

    @APIParam(required = false, validValues = {"4", "6"})
    private Integer ipVersion;

    @APIParam(required = false)
    private String srcIpRange;

    @APIParam(required = false)
    private String dstIpRange;

    @APIParam(required = false)
    private String dstPortRange;

    @APIParam(required = false, nonempty = true, validValues = {"ACCEPT", "DROP"})
    private String action;

    @APIParam(required = false)
    private Integer startPort;

    @APIParam(required = false)
    private Integer endPort;

    @APIParam(required = false)
    private String allowedCidr;

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

    public String getRemoteSecurityGroupUuid() {
        return remoteSecurityGroupUuid;
    }

    public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
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

    public void setDstPortRange(String dstPortRange) {
        this.dstPortRange = dstPortRange;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public static APIValidateSecurityGroupRuleMsg __example__() {
        APIValidateSecurityGroupRuleMsg msg = new APIValidateSecurityGroupRuleMsg();
        msg.setSecurityGroupUuid(uuid());
        msg.setType("Ingress");
        msg.setRemoteSecurityGroupUuid(uuid());
        msg.setIpVersion(4); 
        msg.setAction("ACCEPT");
        msg.setSrcIpRange("10.0.0.1,10.0.0.2-10.0.0.200,10.1.1.0/24");
        msg.setDstIpRange("10.0.0.1,10.0.0.2-10.0.0.200,10.1.1.0/24");
        msg.setProtocol("TCP");
        msg.setDstPortRange("1000,1001,1002-1005,1008");
        return msg;
    }
}
