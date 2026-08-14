package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.volume.VolumeVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2020/7/22.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVmDeviceAddressReply extends APIReply {
    private Map<String, List<VmDeviceAddress>> addresses;

    public void setAddresses(Map<String, List<VmDeviceAddress>> addresses) {
        this.addresses = addresses;
    }

    public Map<String, List<VmDeviceAddress>> getAddresses() {
        return addresses;
    }

    public static APIGetVmDeviceAddressReply __example__() {
        APIGetVmDeviceAddressReply reply = new APIGetVmDeviceAddressReply();

        VmDeviceAddress address = new VmDeviceAddress();
        address.setDeviceType("disk");
        address.setResourceType(VolumeVO.class.getSimpleName());
        address.setAddressType("pci");
        address.setAddress("0000:01:00:0");
        reply.setAddresses(Collections.singletonMap(VolumeVO.class.getSimpleName(), Collections.singletonList(address)));
        return reply;
    }
}
