package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.migratedb.AlarmRecordsInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryAlarmRecordReply.class, inventoryClass = AlarmRecordsInventory.class)
@RestRequest(path = "/zwatch/alarm-records",
        responseClass = APIQueryAlarmRecordReply.class,
        method = HttpMethod.GET)
public class APIQueryAlarmRecordMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("dataUuid=%s", uuid()));
        return ret;
    }
}
