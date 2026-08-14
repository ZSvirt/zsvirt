package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/global-configurations/{category}/{name}",
        method = HttpMethod.GET,
        responseClass = APIGetGlobalConfigOptionsReply.class
)
public class APIGetGlobalConfigOptionsMsg extends APISyncCallMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;

    public String getIdentity() {
        return GlobalConfig.produceIdentity(category, name);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIGetGlobalConfigOptionsMsg __example__() {
        APIGetGlobalConfigOptionsMsg msg = new APIGetGlobalConfigOptionsMsg();
        msg.setCategory("kvm");
        msg.setName("vm.cpuMode");
        return msg;
    }
}
