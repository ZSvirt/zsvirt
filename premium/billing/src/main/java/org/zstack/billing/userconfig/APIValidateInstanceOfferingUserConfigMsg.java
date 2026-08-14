package org.zstack.billing.userconfig;

import org.springframework.http.HttpMethod;
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
        responseClass = APIValidateInstanceOfferingUserConfigEvent.class
)
public class APIValidateInstanceOfferingUserConfigMsg extends APIMessage {
    @APIParam(emptyString = false)
    private String config;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public static APIValidateInstanceOfferingUserConfigMsg __example__() {
        APIValidateInstanceOfferingUserConfigMsg msg = new APIValidateInstanceOfferingUserConfigMsg();

        return msg;
    }
}
