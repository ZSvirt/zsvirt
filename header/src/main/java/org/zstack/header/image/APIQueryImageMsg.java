package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@AutoQuery(replyClass = APIQueryImageReply.class, inventoryClass = ImageInventory.class)
@RestRequest(
        path = "/images",
        optionalPaths = {"/images/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryImageReply.class
)
public class APIQueryImageMsg extends APIQueryMessage {

 
    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}
