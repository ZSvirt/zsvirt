package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.vip.VipVO;

/**
 * @api
 *
 * create a eip
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.eip.APICreateEipMsg": {
"name": "eip",
"description": "eip",
"vipUuid": "715b7942abc93c959e331d4582ede1e2",
"vmNicUuid": "105ff0edcab646128c56ab414588d9fa",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.eip.APICreateEipMsg": {
"name": "eip",
"description": "eip",
"vipUuid": "715b7942abc93c959e331d4582ede1e2",
"vmNicUuid": "105ff0edcab646128c56ab414588d9fa",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
},
"timeout": 1800000,
"id": "c35449410e3342d9895a23630c5f9f50",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APICreateEipEvent`
 */
@RestRequest(
        path = "/eips",
        method = HttpMethod.POST,
        responseClass = APICreateEipEvent.class,
        parameterName = "params"
)
public class APICreateEipMsg extends APICreateMessage implements APIAuditor {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc vip uuid. See :ref:`VipInventory`
     */
    @APIParam(resourceType = VipVO.class)
    private String vipUuid;
    /**
     * @desc vm nic uuid, see :ref:`VmNicInventory`. If omitted, the eip is created without attaching to any vm nic
     * @optional
     */
    @APIParam(required = false, resourceType = VmNicVO.class)
    private String vmNicUuid;

    /**
     * @desc vm nic ip. See :ref:`UsedIpInventory`
     */
    @APIParam(required = false, resourceType = UsedIpVO.class)
    private String usedIpUuid;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public static APICreateEipMsg __example__() {
        APICreateEipMsg msg = new APICreateEipMsg();

        msg.setName("Test-EIP");
        msg.setVipUuid(uuid());
        msg.setVmNicUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateEipEvent evt = (APICreateEipEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, EipVO.class);
    }
}
