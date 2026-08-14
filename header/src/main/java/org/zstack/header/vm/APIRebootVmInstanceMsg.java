package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * @api reboot a vm instance
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIRebootVmInstanceMsg": {
 * "uuid": "bfc19807cdde47d5ac18ef76ed0b450f",
 * "session": {
 * "uuid": "07682ae6cc4648d7975d0d2e9fe9127c"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIRebootVmInstanceMsg": {
 * "uuid": "bfc19807cdde47d5ac18ef76ed0b450f",
 * "session": {
 * "uuid": "07682ae6cc4648d7975d0d2e9fe9127c"
 * },
 * "timeout": 1800000,
 * "id": "1e784af42f334997a9382b1e990b53c8",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIRebootVmInstanceEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIRebootVmInstanceEvent.class
)
@SkipVmTracer(replyClass = APIRebootVmInstanceEvent.class)
public class APIRebootVmInstanceMsg extends APIMessage implements VmInstanceMessage, CheckAttachedVolumesMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
 
    public static APIRebootVmInstanceMsg __example__() {
        APIRebootVmInstanceMsg msg = new APIRebootVmInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
