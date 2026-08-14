package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by GuoYi on 2019-04-19.
 */
@RestRequest(
        path = "/mdev-devices/{mdevDeviceUuid}/vm-instances/{vmInstanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachMdevDeviceFromVmEvent.class
)
public class APIDetachMdevDeviceFromVmMsg extends APIMessage implements MdevDeviceMessage {
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

    public static APIDetachMdevDeviceFromVmMsg __example__() {
        APIDetachMdevDeviceFromVmMsg msg = new APIDetachMdevDeviceFromVmMsg();
        msg.setMdevDeviceUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
