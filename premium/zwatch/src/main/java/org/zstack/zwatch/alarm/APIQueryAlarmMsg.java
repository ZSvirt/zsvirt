package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryAlarmReply.class, inventoryClass = AlarmInventory.class)
@RestRequest(path = "/zwatch/alarms", optionalPaths = {"/zwatch/alarms/{uuid}"},
        responseClass = APIQueryAlarmReply.class, method = HttpMethod.GET)
public class APIQueryAlarmMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("name=VM"));
        return ret;
    }
}
