package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MaJin on 2020/7/22.
 */
public class GetVmDeviceAddressReply extends MessageReply {
    private Map<String, List<VmDeviceAddress>> addresses = new HashMap<>();

    public void putAddresses(String resourceType, List<VmDeviceAddress> addresses) {
        this.addresses.put(resourceType, addresses);
    }

    public void setAddresses(Map<String, List<VmDeviceAddress>> addresses) {
        this.addresses = addresses;
    }

    public Map<String, List<VmDeviceAddress>> getAddresses() {
        return addresses;
    }
}
