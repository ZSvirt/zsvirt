package org.zstack.network.l2.virtualSwitch.header;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventories")
public class APIQueryHostKernelInterfaceReply extends APIQueryReply {

    private List<HostKernelInterfaceInventory> inventories;

    public List<HostKernelInterfaceInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostKernelInterfaceInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostKernelInterfaceReply __example__() {
        APIQueryHostKernelInterfaceReply reply = new APIQueryHostKernelInterfaceReply();
        reply.setInventories(asList(HostKernelInterfaceInventory.__example__()));
        reply.setSuccess(true);
        return reply;
    }

}
