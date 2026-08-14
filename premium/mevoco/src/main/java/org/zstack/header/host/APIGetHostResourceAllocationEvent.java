package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestResponse(fieldsTo = {"name", "uuid", "vCPUPin"})
@Deprecated
public class APIGetHostResourceAllocationEvent extends APIEvent {
    private String name;
    private String uuid;
    private List<Map<String, String>> vCPUPin;

    public APIGetHostResourceAllocationEvent() {
        super(null);
    }

    public APIGetHostResourceAllocationEvent(String apiId) {
        super(apiId);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public List<Map<String, String>> getvCPUPin() {
        return vCPUPin;
    }

    public void setvCPUPin(List<Map<String, String>> vCPUPin) {
        this.vCPUPin = vCPUPin;
    }

    public static APIGetHostResourceAllocationEvent __example__() {
        APIGetHostResourceAllocationEvent evt = new APIGetHostResourceAllocationEvent();
        List<Map<String, String>> vCPUPin = new ArrayList<Map<String, String>> ();
        Map<String, String> pin = new HashMap<String, String>();
        pin.put("vCPU", "0");
        pin.put("pCPU", "13");
        vCPUPin.add(pin);

        evt.setUuid("f7bae73b9874344b8766dfcdda48ad6e");
        evt.setName("example");
        evt.setvCPUPin(vCPUPin);
        return evt;
    }
}
