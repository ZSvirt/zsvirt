package org.zstack.billing.userconfig;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/4/23.
 */

@RestRequest(
        path = "/billings/accounts/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIValidateDiskOfferingUserConfigEvent.class
)
public class APIValidateDiskOfferingUserConfigMsg extends APIMessage {
    @APIParam(emptyString = false)
    private String config;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public static APIValidateDiskOfferingUserConfigMsg __example__() {
        APIValidateDiskOfferingUserConfigMsg msg = new APIValidateDiskOfferingUserConfigMsg();
        msg.setConfig("{ \"allocate\": {\n" +
                "        \"primaryStorage\": {\n" +
                "          \"type\": \"localStorage\",\n" +
                "          \"uuid\": \"dasdsadadas\"\n" +
                "        }}");
        return msg;
    }
}
