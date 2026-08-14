package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryVolumeReply.class, inventoryClass = VolumeInventory.class)
@RestRequest(
        path = "/volumes",
        optionalPaths = {"/volumes/{uuid}"},
        responseClass = APIQueryVolumeReply.class,
        method = HttpMethod.GET
)
public class APIQueryVolumeMsg extends APIQueryMessage {


    public static List<String> __example__() {
        return asList();
    }

}
