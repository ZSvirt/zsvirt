package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQuerySystemTagReply.class, inventoryClass = SystemTagInventory.class)
@RestRequest(
        path = "/system-tags",
        optionalPaths = {"/system-tags/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySystemTagReply.class
)
public class APIQuerySystemTagMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return asList("inherent=true", "resourceType=HostVO");
    }

}
