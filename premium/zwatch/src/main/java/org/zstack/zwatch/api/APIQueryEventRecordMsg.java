package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.migratedb.EventRecordsInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryEventRecordReply.class, inventoryClass = EventRecordsInventory.class)
@RestRequest(path = "/zwatch/event-records",
        responseClass = APIQueryEventRecordReply.class,
        method = HttpMethod.GET)
public class APIQueryEventRecordMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("dataUuid=%s", uuid()));
        return ret;
    }
}
