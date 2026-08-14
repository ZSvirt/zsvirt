package org.zstack.usbDevice;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 10/21/17.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/candidate-usb-devices",
        method = HttpMethod.GET,
        responseClass = APIGetUsbDeviceCandidatesForAttachingVmReply.class
)
public class APIGetUsbDeviceCandidatesForAttachingVmMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false, validValues = {"PassThrough","Redirect"})
    private String attachType = "PassThrough";

    public static APIGetUsbDeviceCandidatesForAttachingVmMsg __example__() {
        APIGetUsbDeviceCandidatesForAttachingVmMsg msg = new APIGetUsbDeviceCandidatesForAttachingVmMsg();
        msg.setVmInstanceUuid(uuid());
        msg.attachType = "PassThrough";
        return msg;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getAttachType() {
        return attachType;
    }

    public void setAttachType(String attachType) {
        this.attachType = attachType;
    }
}