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
        method = HttpMethod.POST,
        responseClass = APIAddMdevDeviceSpecToVmInstanceEvent.class,
        parameterName = "params"
)
public class APIAddMdevDeviceSpecToVmInstanceMsg extends APIMessage {
    @APIParam(resourceType = MdevDeviceSpecVO.class)
    private String mdevSpecUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    // assume there are less than 100 devices releated to one spec in one host
    @APIParam(required = false, numberRange = {1, 100})
    private Integer mdevDeviceNumber = 1;

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

    public Integer getMdevDeviceNumber() {
        return mdevDeviceNumber;
    }

    public void setMdevDeviceNumber(Integer mdevDeviceNumber) {
        this.mdevDeviceNumber = mdevDeviceNumber;
    }

    public static APIAddMdevDeviceSpecToVmInstanceMsg __example__() {
        APIAddMdevDeviceSpecToVmInstanceMsg msg = new APIAddMdevDeviceSpecToVmInstanceMsg();
        msg.setMdevSpecUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
