package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.vip.VipVO;

/**
 * @api
 *
 * create port forwarding rule
 *
 * @category port forwarding
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.portforwarding.APICreatePortForwardingRuleMsg": {
"vipUuid": "ad6c5c53e9c6322e8425d1c7a7d41094",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"protocolType": "TCP",
"vmNicUuid": "bbfec28df38c426c974e37fc85c98b45",
"name": "pfRule1",
"session": {
"uuid": "4ead370e6a4a444a95155cf65d870b98"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.portforwarding.APICreatePortForwardingRuleMsg": {
"vipUuid": "ad6c5c53e9c6322e8425d1c7a7d41094",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"protocolType": "TCP",
"vmNicUuid": "bbfec28df38c426c974e37fc85c98b45",
"name": "pfRule1",
"session": {
"uuid": "4ead370e6a4a444a95155cf65d870b98"
},
"timeout": 1800000,
"id": "d6e1b111a0b541d29f10a84a97c05d3e",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APICreatePortForwardingRuleEvent`
 */
@RestRequest(
        path = "/port-forwarding",
        method = HttpMethod.POST,
        responseClass = APICreatePortForwardingRuleEvent.class,
        parameterName = "params"
)
public class APICreatePortForwardingRuleMsg extends APICreateMessage implements APIAuditor {
    /**
     * @desc uuid of vip the rule is being created on
     */
    @APIParam(resourceType = VipVO.class)
    private String vipUuid;
    /**
     * @desc start port to be mapped
     * @choices 1 - 65535
     */
    @APIParam(numberRange = {1, 65535})
    private Integer vipPortStart;
    /**
     * @desc end port to be mapped. Null means equaling to vipPortStart
     * @choices 1 - 65535
     * @optional
     */
    @APIParam(required = false, numberRange = {1, 65535})
    private Integer vipPortEnd;
    /**
     * @desc start port vipPortStart maps to
     * @choices 1 - 65535
     */
    @APIParam(required = false, numberRange = {1, 65535})
    private Integer privatePortStart;
    /**
     * @desc end port vipPortEnd maps to. Null means equaling to privatePortEnd
     * @choices 1 - 65535
     * @optional
     */
    @APIParam(required = false, numberRange = {1, 65535})
    private Integer privatePortEnd;
    /**
     * @desc network prototype the rule applies to
     * @choices - TCP
     * - UDP
     */
    @APIParam(validValues = {"TCP", "UDP"})
    private String protocolType;
    /**
     * @desc uuid of vm nic(see :ref:`VmNicInventory) the rule attaches to. If omitted, the rule is created without attaching
     * to any vm nic
     * @optional
     */
    @APIParam(required = false, resourceType = VmNicVO.class)
    private String vmNicUuid;
    /**
     * @desc if not null, the rule only applies to traffic from this CIDR, other traffic are denied
     * @optional
     */
    private String allowedCidr;
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     * @optional
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public Integer getVipPortStart() {
        return vipPortStart;
    }

    public void setVipPortStart(Integer vipPortStart) {
        this.vipPortStart = vipPortStart;
    }

    public Integer getVipPortEnd() {
        return vipPortEnd;
    }

    public void setVipPortEnd(Integer vipPortEnd) {
        this.vipPortEnd = vipPortEnd;
    }

    public Integer getPrivatePortStart() {
        return privatePortStart;
    }

    public void setPrivatePortStart(Integer privatePortStart) {
        this.privatePortStart = privatePortStart;
    }

    public Integer getPrivatePortEnd() {
        return privatePortEnd;
    }

    public void setPrivatePortEnd(Integer privatePortEnd) {
        this.privatePortEnd = privatePortEnd;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getAllowedCidr() {
        return allowedCidr;
    }

    public void setAllowedCidr(String allowedCidr) {
        this.allowedCidr = allowedCidr;
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

    public static APICreatePortForwardingRuleMsg __example__() {
        APICreatePortForwardingRuleMsg msg = new APICreatePortForwardingRuleMsg();
        msg.setName("pf1");
        msg.setVipPortStart(22);
        msg.setVipUuid(uuid());
        msg.setProtocolType("TCP");
        msg.setVmNicUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreatePortForwardingRuleEvent)rsp).getInventory().getUuid() : "", PortForwardingRuleVO.class);
    }
}


