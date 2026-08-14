package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class GetHostSensorsReply extends MessageReply {
    List<Sensor> Sensors;

    public List<Sensor> getSensors() {
        return Sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        Sensors = sensors;
    }
}
