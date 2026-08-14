package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by camile on 12/18/2017.
 */
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateVmNicMacEvent.class
)
public class APIUpdateVmNicMacMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;
    @APIParam
    private String mac;
    @APINoSee
    private String vmInstanceUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public static APIUpdateVmNicMacMsg __example__() {
        APIUpdateVmNicMacMsg msg = new APIUpdateVmNicMacMsg();
        msg.setVmNicUuid(uuid());
        msg.setMac("fa:4c:ee:9a:76:00");
        return msg;
    }
}
