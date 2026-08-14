package org.zstack.zwatch.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/zwatch/metrics/meta-data", method = HttpMethod.GET, responseClass = APIGetAllMetricMetadataReply.class)
public class APIGetAllMetricMetadataMsg extends APISyncCallMessage {
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

    public static APIGetAllMetricMetadataMsg __example__() {
        APIGetAllMetricMetadataMsg ret = new APIGetAllMetricMetadataMsg();
        ret.name = "VMHAProcess";
        return ret;
    }
}
