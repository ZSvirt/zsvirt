package org.zstack.header.cloudformation.monitor;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/11/22.
 */
@RestResponse(fieldsTo = {"portStatus"})
public class APIGetResourceStackVmStatusReply extends APIReply {
    private Map<String, Map<String, String>> portStatus = new HashMap<>();

    public Map<String, Map<String, String>> getPortStatus() {
        return portStatus;
    }

    public void setPortStatus(Map<String, Map<String, String>> portStatus) {
        this.portStatus = portStatus;
    }

    public static APIGetResourceStackVmStatusReply __example__() {
        APIGetResourceStackVmStatusReply reply = new APIGetResourceStackVmStatusReply();
        Map<String, String> vm1Status = new HashMap<>();
        Map<String, String> vm2Status = new HashMap<>();
        Map<String, Map<String, String>> statusMap = new HashMap<>();
        vm1Status.put("22", "open");
        vm1Status.put("80", "open");
        vm2Status.put("22", "close");
        vm2Status.put("80", "close");

        statusMap.put(uuid(), vm1Status);
        statusMap.put(uuid(), vm2Status);
        return reply;
    }
}
