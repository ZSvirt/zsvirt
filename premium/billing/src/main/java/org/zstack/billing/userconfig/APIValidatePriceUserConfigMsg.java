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
        responseClass = APIValidatePriceUserConfigEvent.class
)
public class APIValidatePriceUserConfigMsg extends APIMessage {
    @APIParam
    private String config;

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public static APIValidatePriceUserConfigMsg __example__() {
        APIValidatePriceUserConfigMsg msg = new APIValidatePriceUserConfigMsg();

        return msg;
    }
}
