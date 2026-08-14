package org.zstack.zwatch.monitorgroup.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.monitorgroup.entity.MonitorGroupAlarmInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryMonitorGroupAlarmReply.class, inventoryClass = MonitorGroupAlarmInventory.class)
@RestRequest(
        path = "/zwatch/monitorgroups/alarms",
        optionalPaths = {"/zwatch/monitorgroups/alarms/{uuid}"},
        responseClass = APIQueryMonitorGroupAlarmReply.class,
        method = HttpMethod.GET)
public class APIQueryMonitorGroupAlarmMsg extends APIQueryMessage {

    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("groupUuid=d4904ace98f834e7bf3485376742133f"));
        return ret;
    }
}
