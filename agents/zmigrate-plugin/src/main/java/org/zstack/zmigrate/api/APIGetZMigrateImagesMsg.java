package org.zstack.zmigrate.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zmigrate/images",
        method = HttpMethod.GET,
        responseClass = APIGetZMigrateImagesReply.class
)
public class APIGetZMigrateImagesMsg extends APISyncCallMessage {

    public static APIGetZMigrateImagesMsg __example__() {
        APIGetZMigrateImagesMsg msg = new APIGetZMigrateImagesMsg();
        return msg;
    }
}
