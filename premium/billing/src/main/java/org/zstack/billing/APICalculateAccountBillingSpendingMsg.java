package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Date;

/**
 * Created by lining on 2019/4/25.
 */
@RestRequest(
        path = "/billings/accounts/{accountUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICalculateAccountBillingSpendingReply.class
)
public class APICalculateAccountBillingSpendingMsg extends APISyncCallMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;
    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long dateStart;
    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long dateEnd;
    @APIParam(required = false)
    private String resourceUuid;
    @APIParam(required = false)
    private boolean simple;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Long getDateStart() {
        return dateStart;
    }

    public void setDateStart(Long dateStart) {
        this.dateStart = dateStart;
    }

    public Long getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Long dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public boolean isSimple() {
        return simple;
    }

    public void setSimple(boolean simple) {
        this.simple = simple;
    }

    public static APICalculateAccountBillingSpendingMsg __example__() {
        APICalculateAccountBillingSpendingMsg msg = new APICalculateAccountBillingSpendingMsg();
        msg.setAccountUuid(uuid());
        msg.setDateStart(new Date(0).getTime());
        msg.setDateEnd(org.zstack.header.message.DocUtils.date);

        return msg;
    }

}
