package org.zstack.pciDevice.specification.mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 2019-05-22.
 */
@RestRequest(
        path = "/mdev-device-specs/{mdevSpecUuid}/vm-instances/{vmInstanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveMdevDeviceSpecFromVmInstanceEvent.class
)
public class APIRemoveMdevDeviceSpecFromVmInstanceMsg extends APIMessage {
    @APIParam(resourceType = MdevDeviceSpecVO.class)
    private String mdevSpecUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getMdevSpecUuid() {
        return mdevSpecUuid;
    }

    public void setMdevSpecUuid(String mdevSpecUuid) {
        this.mdevSpecUuid = mdevSpecUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIRemoveMdevDeviceSpecFromVmInstanceMsg __example__() {
        APIRemoveMdevDeviceSpecFromVmInstanceMsg msg = new APIRemoveMdevDeviceSpecFromVmInstanceMsg();
        msg.setMdevSpecUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
