package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


@RestRequest(
        path = "/security-groups/rules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeSecurityGroupRuleEvent.class,
        isAction = true
)

public class APIChangeSecurityGroupRuleMsg extends APIMessage{
    @APIParam(resourceType = SecurityGroupRuleVO.class, nonempty = true)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String description;

    @APIParam(required = false)
    private String remoteSecurityGroupUuid;

    @APIParam(validValues = {"DROP", "ACCEPT"}, required = false, nonempty = true)
    private String action;

    @APIParam(validValues = {"Enabled", "Disabled"}, required = false, nonempty = true)
    private String state;

    @APIParam(maxLength = 4, required = false, nonempty = true)
    private Integer priority;

    @APIParam(validValues = {"ALL", "TCP", "UDP", "ICMP"}, required = false, nonempty = true)
    private String protocol;

    @APIParam(required = false, maxLength = 1024)
    private String srcIpRange;

    @APIParam(required = false, maxLength = 1024)
    private String dstIpRange;

    @APIParam(required = false, maxLength = 255)
    private String dstPortRange;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription(){
        return description;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public String getRemoteSecurityGroupUuid() {
        return remoteSecurityGroupUuid;
    }

    public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
        this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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

    public static APIChangeSecurityGroupRuleMsg __example__() {
        APIChangeSecurityGroupRuleMsg msg = new APIChangeSecurityGroupRuleMsg();
        msg.setUuid(uuid());
        msg.setDescription("test");
        msg.setRemoteSecurityGroupUuid(uuid());
        msg.setAction(SecurityGroupRuleAction.DROP.toString());
        msg.setState(SecurityGroupRuleState.Enabled.toString());
        msg.setPriority(1);
        msg.setProtocol(SecurityGroupRuleProtocolType.TCP.toString());
        msg.setSrcIpRange("1.1.1.1,2.2.2.0/24,3.3.3.1-3.3.3.10");
        msg.setDstPortRange("1001,2000-2023,6001");
        return msg;
    }

}
