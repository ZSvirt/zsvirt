package org.zstack.zwatch.alarm.activealarm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryActiveAlarmReply.class, inventoryClass = ActiveAlarmInventory.class)
@RestRequest(
        path = "/zwatch/activealarms/alarms",
        optionalPaths = {"/zwatch/activealarms/alarms/{uuid}"},
        responseClass = APIQueryActiveAlarmReply.class,
        method = HttpMethod.GET)
public class APIQueryActiveAlarmMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("alarmUuid=d4904ace98f834e7bf3485376742133f"));
        return ret;
    }
}
