package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 */
@AutoQuery(replyClass = APIQueryUserTagReply.class, inventoryClass = UserTagInventory.class)
@RestRequest(
        path = "/user-tags",
        optionalPaths = {"/user-tags/{uuid}"},
        responseClass = APIQueryUserTagReply.class,
        method = HttpMethod.GET
)
public class APIQueryUserTagMsg extends APIQueryMessage {
 
    public static List<String> __example__() {
        return asList("resourceType=DiskOfferingVO","tag=for-large-DB");
    }

}
