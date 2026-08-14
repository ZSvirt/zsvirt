package org.zstack.header.cloudformation;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/7/11.
 */
@RestRequest(
        path = "/cloudformation/resources",
        method = HttpMethod.GET,
        responseClass = APIGetSupportedCloudFormationResourcesReply.class
)
public class APIGetSupportedCloudFormationResourcesMsg extends APISyncCallMessage {
    @APIParam(validValues = {"v1"}, required = false)
    private String version;

    @APIParam(validValues = {"zstack"}, required = false)
    private String type = "zstack";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APIGetSupportedCloudFormationResourcesMsg __example__() {
        APIGetSupportedCloudFormationResourcesMsg msg = new APIGetSupportedCloudFormationResourcesMsg();

        return msg;
    }
}
