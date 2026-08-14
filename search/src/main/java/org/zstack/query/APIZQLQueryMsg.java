package org.zstack.query;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zql", method = HttpMethod.GET, responseClass = APIZQLQueryReply.class)
public class APIZQLQueryMsg extends APISyncCallMessage {
    private String zql;

    public static APIZQLQueryMsg __example__() {
        APIZQLQueryMsg ret = new APIZQLQueryMsg();
        return ret;
    }

    public String getZql() {
        return zql;
    }

    public void setZql(String zql) {
        this.zql = zql;
    }
}
