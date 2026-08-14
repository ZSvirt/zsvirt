package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by GuoYi on 2019-09-28.
 */
@RestResponse(fieldsTo = {"firstBootDevice"})
public class APIGetVmInstanceFirstBootDeviceReply extends APIReply {
    private String firstBootDevice;

    public String getFirstBootDevice() {
        return firstBootDevice;
    }

    public void setFirstBootDevice(String firstBootDevice) {
        this.firstBootDevice = firstBootDevice;
    }

    public static APIGetVmInstanceFirstBootDeviceReply __example__() {
        APIGetVmInstanceFirstBootDeviceReply reply = new APIGetVmInstanceFirstBootDeviceReply();
        reply.setFirstBootDevice(VmBootDevice.CdRom.toString());
        return reply;
    }
}
