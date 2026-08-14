package org.zstack.core.errorcode;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/1.
 */
@RestRequest(
        path = "/errorcode/elaborations/categories",
        method = HttpMethod.GET,
        responseClass = APIGetElaborationCategoriesReply.class
)
public class APIGetElaborationCategoriesMsg extends APISyncCallMessage {
    public static APIGetElaborationCategoriesMsg __example__() {
        return new APIGetElaborationCategoriesMsg();
    }
}
