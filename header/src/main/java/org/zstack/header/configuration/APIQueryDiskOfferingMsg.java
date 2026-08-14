package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryDiskOfferingReply.class, inventoryClass = DiskOfferingInventory.class)
@RestRequest(
        path = "/disk-offerings",
        optionalPaths = "/disk-offerings/{uuid}",
        method = HttpMethod.GET,
        responseClass = APIQueryDiskOfferingReply.class
)
public class APIQueryDiskOfferingMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
