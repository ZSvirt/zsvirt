package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import static java.util.Arrays.asList;
import java.util.List;

@AutoQuery(replyClass = APIQueryHostReply.class, inventoryClass = HostInventory.class)
@RestRequest(
        path = "/hosts",
        optionalPaths = {"/hosts/{uuid}"},
        responseClass = APIQueryHostReply.class,
        method = HttpMethod.GET
)
public class APIQueryHostMsg extends APIQueryMessage {

 
    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }

}
