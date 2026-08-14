package org.zstack.network.service.eip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

/**
 * @api
 * attach eip to a vm nic
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.eip.APIAttachEipMsg": {
"eipUuid": "69198105fd7a40778fba1759b923545c",
"vmNicUuid": "0bdb4fb64baa4c4d85d39c9f43773342",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.eip.APIAttachEipMsg": {
"eipUuid": "69198105fd7a40778fba1759b923545c",
"vmNicUuid": "0bdb4fb64baa4c4d85d39c9f43773342",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
},
"timeout": 1800000,
"id": "3df75c42d240472d8fc1e7edecd83149",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAttachEipEvent`
 */
@RestRequest(
        path = "/eips/{eipUuid}/vm-instances/nics/{vmNicUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachEipEvent.class
)
public class APIAttachEipMsg extends APIMessage implements EipMessage {
    /**
     * @desc eip uuid
     */
    @APIParam(resourceType = EipVO.class)
    private String eipUuid;
    /**
     * @desc vm nic uuid. See :ref:`VmNicInventory`
     */
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    /**
     * @desc vm nic ip. See :ref:`UsedIpInventory`
     */
    @APIParam(required = false, resourceType = UsedIpVO.class)
    private String usedIpUuid;

    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public static APIAttachEipMsg __example__() {
        APIAttachEipMsg msg = new APIAttachEipMsg();
        msg.setEipUuid(uuid());
        msg.setVmNicUuid(uuid());

        return msg;
    }
}
