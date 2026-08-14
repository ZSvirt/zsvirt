package org.zstack.header.storage.addon.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/primary-storage/addon/discover",
        method = HttpMethod.POST,
        responseClass = APIDiscoverExternalPrimaryStorageEvent.class,
        parameterName = "params"
)
public class APIDiscoverExternalPrimaryStorageMsg extends APIMessage {
    @APIParam
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    @APIParam(required = false)
    private String identity;
    @APIParam(required = false)
    private String config;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public static APIDiscoverExternalPrimaryStorageMsg __example__() {
        APIDiscoverExternalPrimaryStorageMsg msg = new APIDiscoverExternalPrimaryStorageMsg();
        msg.setUrl("http://operator:password@172.25.1.5:80");
        return msg;
    }
}
