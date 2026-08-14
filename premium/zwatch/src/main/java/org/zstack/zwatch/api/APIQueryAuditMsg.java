package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.zwatch.migratedb.AuditsInventory;

import java.util.ArrayList;
import java.util.List;

@AutoQuery(replyClass = APIQueryAuditReply.class, inventoryClass = AuditsInventory.class)
@RestRequest(path = "/zwatch/audit-records",
        responseClass = APIQueryAuditReply.class,
        method = HttpMethod.GET)
public class APIQueryAuditMsg extends APIQueryMessage {
    public static List<String> __example__() {
        List<String> ret = new ArrayList<>();
        ret.add(String.format("resourceUuid=%s", uuid()));
        return ret;
    }
}
