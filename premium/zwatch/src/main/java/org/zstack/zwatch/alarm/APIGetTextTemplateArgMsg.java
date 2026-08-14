package org.zstack.zwatch.alarm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/zwatch/textTemplateArg",
        method = HttpMethod.GET,
        responseClass = APIGetTextTemplateArgReply.class
)
public class APIGetTextTemplateArgMsg extends APISyncCallMessage {

    public static APIGetTextTemplateArgMsg __example__() {
        APIGetTextTemplateArgMsg msg = new APIGetTextTemplateArgMsg();
        return msg;
    }
}
