package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.APIGetCandidateIsoForAttachingVmReply;

/**
 * Created by lining on 2019/10/10.
 */
@RestRequest(
        path = "/billings/price-tables/refs",
        method = HttpMethod.GET,
        responseClass = APIGetAccountPriceTableRefReply.class
)
public class APIGetAccountPriceTableRefMsg extends APISyncCallMessage {
    @APIParam(resourceType = PriceTableVO.class, required = false)
    private String tableUuid;

    @APIParam(resourceType = AccountVO.class, required = false)
    private String accountUuid;

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public static APIGetAccountPriceTableRefMsg __example__() {
        APIGetAccountPriceTableRefMsg msg = new APIGetAccountPriceTableRefMsg();
        msg.tableUuid = uuid();
        return msg;
    }

}
