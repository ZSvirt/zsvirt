package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "sensors")
public class APIGetHostSensorsReply extends APIReply {
    List<Sensor> sensors;

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    public static APIGetHostSensorsReply __example__() {
        APIGetHostSensorsReply reply = new APIGetHostSensorsReply();
        return reply;
    }
}