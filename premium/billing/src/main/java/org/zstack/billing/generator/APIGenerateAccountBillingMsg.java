package org.zstack.billing.generator;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created by lining on 2019/4/25.
 */
@RestRequest(
        path = "/billings/accounts/{accountUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIGenerateAccountBillingEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APIGenerateAccountBillingMsg extends APIMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
 
    public static APIGenerateAccountBillingMsg __example__() {
        APIGenerateAccountBillingMsg msg = new APIGenerateAccountBillingMsg();
        msg.setAccountUuid(uuid());

        return msg;
    }
}
