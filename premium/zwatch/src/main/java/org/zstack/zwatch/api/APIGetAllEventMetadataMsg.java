package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/events/meta-data", method = HttpMethod.GET, responseClass = APIGetAllEventMetadataReply.class)
public class APIGetAllEventMetadataMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String name;
    @APIParam(required = false)
    private String namespace;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public static APIGetAllEventMetadataMsg __example__() {
        APIGetAllEventMetadataMsg ret = new APIGetAllEventMetadataMsg();
        ret.name = "VMHAProcess";
        return ret;
    }
}
