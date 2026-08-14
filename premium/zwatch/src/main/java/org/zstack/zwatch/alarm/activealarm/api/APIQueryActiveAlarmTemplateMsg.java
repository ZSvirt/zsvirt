package org.zstack.zwatch.alarm.activealarm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.alarm.activealarm.entity.ActiveAlarmTemplateInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryActiveAlarmTemplateReply.class, inventoryClass = ActiveAlarmTemplateInventory.class)
@RestRequest(
        path = "/zwatch/activealarms/templates",
        optionalPaths = {"/zwatch/activealarms/templates/{uuid}"},
        responseClass = APIQueryActiveAlarmTemplateReply.class,
        method = HttpMethod.GET)
public class APIQueryActiveAlarmTemplateMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("uuid=d4904ace98f834e7bf3485376742133f"));
        return ret;
    }
}
