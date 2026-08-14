package org.zstack.resourceconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by MaJin on 2019/2/23.
 */
@RestRequest(path = "/resource-configurations/bindable",
        optionalPaths = {"/resource-configurations/bindable/{category}"},
        method = HttpMethod.GET, responseClass = APIGetResourceBindableConfigReply.class)
public class APIGetResourceBindableConfigMsg extends APISyncCallMessage {
    @APIParam(required = false)
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public static APIGetResourceBindableConfigMsg __example__() {
        APIGetResourceBindableConfigMsg msg = new APIGetResourceBindableConfigMsg();
        msg.category = "host";
        return msg;
    }
}
