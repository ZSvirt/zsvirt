package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 2019-04-20.
 */
@RestRequest(
        path = "/mdev-devices/{mdevDeviceUuid}/vm-instances/{vmInstanceUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachMdevDeviceToVmEvent.class,
        parameterName = "params"
)
public class APIAttachMdevDeviceToVmMsg extends APIMessage implements MdevDeviceMessage {
    @APIParam(resourceType = MdevDeviceVO.class)
    private String mdevDeviceUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getMdevDeviceUuid() {
        return mdevDeviceUuid;
    }

    public void setMdevDeviceUuid(String mdevDeviceUuid) {
        this.mdevDeviceUuid = mdevDeviceUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIAttachMdevDeviceToVmMsg __example__() {
        APIAttachMdevDeviceToVmMsg msg = new APIAttachMdevDeviceToVmMsg();
        msg.setMdevDeviceUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
