package org.zstack.header.vo;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/5/1.
 */
@RestRequest(
        path = "/resources/names",
        method = HttpMethod.GET,
        responseClass = APIGetResourceNamesReply.class
)
public class APIGetResourceNamesMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> uuids;

    public List<String> getUuids() {
        return uuids;
    }

    public void setUuids(List<String> uuids) {
        this.uuids = uuids;
    }

    public static APIGetResourceNamesMsg __example__() {
        APIGetResourceNamesMsg msg = new APIGetResourceNamesMsg();
        msg.setUuids(asList(uuid(), uuid()));
        return msg;
    }
}
