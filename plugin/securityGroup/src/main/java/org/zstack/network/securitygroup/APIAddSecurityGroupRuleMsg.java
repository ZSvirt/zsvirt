package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;

/**
 * @api
 * add rule to a security group
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"priority": -1,
"type": "Ingress",
"rules": [
{
"protocol": "TCP",
"srcIpRange": "10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24",
"dstIpRange": "20.0.0.1,20.0.0.2-20.0.0.20,20.1.1.0/24",
"srcPortRange": "1000,1001,1002-1005,1008",
"dstPortRange": "2000,2001,2002-2005,2008",
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"defaultTarget": "RETURN"
},
{
"protocol": "UDP",
"srcIpRange": "10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24",
"dstIpRange": "20.0.0.1,20.0.0.2-20.0.0.20,20.1.1.0/24",
"srcPortRange": "1000,1001,1002-1005,1008",
"dstPortRange": "2000,2001,2002-2005,2008",
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"defaultTarget": "RETURN"
}
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"priority": -1,
"type": "Ingress",
"rules": [
{
"ipVersion": "4",
"protocol": "TCP",
"srcIpRange": "10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24",
"dstIpRange": "20.0.0.1,20.0.0.2-20.0.0.20,20.1.1.0/24",
"srcPortRange": "1000,1001,1002-1005,1008",
"dstPortRange": "2000,2001,2002-2005,2008",
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"action": "ACCEPT"
},
{
"ipVersion": "4",
"protocol": "UDP",
"srcIpRange": "10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24",
"dstIpRange": "20.0.0.1,20.0.0.2-20.0.0.20,20.1.1.0/24",
"srcPortRange": "1000,1001,1002-1005,1008",
"dstPortRange": "2000,2001,2002-2005,2008",
"remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
"action": "ACCEPT"
}
],
}
],
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
},
"timeout": 1800000,
"id": "c644a6d41e614ffeaa9e2112bf339b6b",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAddSecurityGroupRuleEvent`
 */
@RestRequest(
        path = "/security-groups/{securityGroupUuid}/rules",
        method = HttpMethod.POST,
        responseClass = APIAddSecurityGroupRuleEvent.class,
        parameterName = "params"
)
public class APIAddSecurityGroupRuleMsg extends APIMessage implements AddSecurityGroupRuleMessage {
    /**
     * @inventory
     * inventory for SecurityGroupRuleAO
     *
     * @category security group
     *
     * @example
     *
     *{
    "ipVersion": "4",
    "protocol": "UDP",
    "srcIpRange": "1.1.1.1,1.1.1.2",
    "dstIpRange": "2.2.2.1,2.2.2.2",
    "srcPortRange": "1000,1001",
    "dstPortRange": "2000,2001",
    "remoteSecurityGroupUuid": "7c224d3f5ad74520ac4dd6c81def0d8e",
    "defaultTarget": "DROP"
    }
     * @since 0.1.0
     */
    @PythonClassInventory
    public static class SecurityGroupRuleAO {

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
        @APIParam(required = true, validValues = {"Ingress", "Egress"})
        private String type;

        @APIParam(required = false, validValues = {"Enabled", "Disabled"})
        private String state = SecurityGroupRuleState.Enabled.toString();

        @APIParam(required = false, maxLength = 255)
        private String description;

        /**
         * @desc remote security group uuid for rules between groups
         */
        @APIParam(resourceType = SecurityGroupVO.class, required = false, nonempty = true)
        private String remoteSecurityGroupUuid;

        @APIParam(required = false, validValues = {"4", "6"})
        private Integer ipVersion;

        /**
         * @desc network protocol type
         * @choices
         * - TCP
         * - UDP
         * - ICMP
         * - ALL
         */
        private String protocol;

        /**
         * @desc source ip address range
         * @choices 10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24
         */
        @APIParam(required = false)
        private String srcIpRange;

        /**
         * @desc destination ip address range
         * @choices 10.0.0.1,10.0.0.2-10.0.0.20,10.1.1.0/24
         */
        @APIParam(required = false)
        private String dstIpRange;

        /**
         * @desc destination ip port range
         * @choices 1000,1001,1002-1005,1008
         */
        @APIParam(required = false)
        private String dstPortRange;

         /**
         * @desc rule default target
         * @choices
         * - ACCEPT / DROP
         */
        @APIParam(required = false, validValues = {"ACCEPT", "DROP"})
        private String action = SecurityGroupRuleAction.ACCEPT.toString();

         /**
         * @desc
         * start port
         * @choices 0 - 65535
         */
        private Integer startPort;
        /**
         * @desc
         * end port. If omitted, endPort is set to startPort
         * @choices 0 - 65535
         * @nullable
         */
        private Integer endPort;

        /**
         * @desc source CIDR the rule applies to. If set, the rule only applies to traffic from this CIDR. If omitted, the rule
         * applies to all traffic
         * @nullable
         */
        private String allowedCidr;

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        public void setDstIpRange(String dstIpRange) {
            this.dstIpRange = dstIpRange;
        }

        public String getDstPortRange() {
            return dstPortRange;
        }

        public void setDstPortRange(String dstPortRange) {
            this.dstPortRange = dstPortRange;
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

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

       public String getRemoteSecurityGroupUuid() {
            return remoteSecurityGroupUuid;
        }

        public void setRemoteSecurityGroupUuid(String remoteSecurityGroupUuid) {
            this.remoteSecurityGroupUuid = remoteSecurityGroupUuid;
        }

        public String getAllowedCidr() {
            return allowedCidr;
        }

        public void setAllowedCidr(String allowedCidr) {
            this.allowedCidr = allowedCidr;
        }

        public Integer getIpVersion() {
            return ipVersion;
        }

        public void setIpVersion(Integer ipVersion) {
            this.ipVersion = ipVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, remoteSecurityGroupUuid,
                    ipVersion, protocol, srcIpRange, dstIpRange, dstPortRange, action);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof SecurityGroupRuleAO)) {
                return false;
            }
            SecurityGroupRuleAO other = (SecurityGroupRuleAO) o;
            return Objects.equals(type, other.type) && Objects.equals(remoteSecurityGroupUuid, other.remoteSecurityGroupUuid)
                    && Objects.equals(ipVersion, other.ipVersion) && Objects.equals(protocol, other.protocol)
                    && Objects.equals(srcIpRange, other.srcIpRange) && Objects.equals(dstIpRange, other.dstIpRange)
                    && Objects.equals(dstPortRange, other.dstPortRange) && Objects.equals(action, other.action);
        }
    }

    @APIParam(resourceType = SecurityGroupVO.class, nonempty = true, required = true)
    private String securityGroupUuid;

    /**
     * @desc a list of :ref:`SecurityGroupRuleAO` that describe rules
     */
    @APIParam(nonempty = true, required = true)
    private List<SecurityGroupRuleAO> rules;

    @APIParam(resourceType = SecurityGroupVO.class, required = false, nonempty = true)
    private List<String> remoteSecurityGroupUuids;

    /**
     * @desc rules priority
     * @choices
     * - >1: defined by user
     * - 1: lowest priority
     */
    @APIParam(required = false)
    private Integer priority = -1;

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public List<SecurityGroupRuleAO> getRules() {
        return rules;
    }

    public void setRules(List<SecurityGroupRuleAO> rules) {
        this.rules = rules;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }

    public void setRemoteSecurityGroupUuids(List<String> remoteSecurityGroupUuids) {
        this.remoteSecurityGroupUuids = remoteSecurityGroupUuids;
    }

    public List<String> getRemoteSecurityGroupUuids() {
        return remoteSecurityGroupUuids;
    }

    public static APIAddSecurityGroupRuleMsg __example__() {
        APIAddSecurityGroupRuleMsg msg = new APIAddSecurityGroupRuleMsg();
        msg.setSecurityGroupUuid(uuid());
        SecurityGroupRuleAO rule = new SecurityGroupRuleAO();
        rule.setType("Ingress");
        rule.setState("Enabled");
        rule.setDescription("test");
        rule.setRemoteSecurityGroupUuid(uuid());
        rule.setIpVersion(4); 
        rule.setAction("ACCEPT");
        rule.setSrcIpRange("10.0.0.1,10.0.0.2-10.0.0.200,10.1.1.0/24");
        rule.setDstIpRange("10.0.0.1,10.0.0.2-10.0.0.200,10.1.1.0/24");
        rule.setProtocol("TCP");
        rule.setDstPortRange("1000,1001,1002-1005,1008");
        msg.setRules(asList(rule));
        msg.setPriority(-1);
        return msg;
    }
}
