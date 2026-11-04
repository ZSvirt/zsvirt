package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.pciDevice.entity.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.message.virtual.vfio_mdev.MdevDeviceMessage;

@RestRequest(
        path = "/mdev-devices/{mdevDeviceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteMdevDeviceEvent.class
)
public class APIDeleteMdevDeviceMsg extends APIDeleteMessage implements MdevDeviceMessage {
    @APIParam(resourceType = MdevDeviceVO.class)
    private String mdevDeviceUuid;

    @Override
    public String getMdevDeviceUuid() {
        return mdevDeviceUuid;
    }

    public void setMdevDeviceUuid(String mdevDeviceUuid) {
        this.mdevDeviceUuid = mdevDeviceUuid;
    }

    public static APIDeleteMdevDeviceMsg __example__() {
        APIDeleteMdevDeviceMsg msg = new APIDeleteMdevDeviceMsg();
        msg.setMdevDeviceUuid(uuid(MdevDeviceVO.class));
        return msg;
    }
}
