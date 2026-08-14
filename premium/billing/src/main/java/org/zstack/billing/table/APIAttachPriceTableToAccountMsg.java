package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

import static org.zstack.header.message.APIParam.SCOPE_ALLOWED_SHARING;

/**
 * Created by lining on 2019/9/10.
 */
@RestRequest(
        path = "/billings/price-tables/{tableUuid}/accounts/{accountUuid}",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAttachPriceTableToAccountEvent.class
)
public class APIAttachPriceTableToAccountMsg extends APIMessage implements APIAuditor {

    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    @APIParam(resourceType = PriceTableVO.class, scope = SCOPE_ALLOWED_SHARING)
    private String tableUuid;

    public static APIAttachPriceTableToAccountMsg __example__() {
        APIAttachPriceTableToAccountMsg msg = new APIAttachPriceTableToAccountMsg();
        msg.setTableUuid(uuid());
        msg.setAccountUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAttachPriceTableToAccountEvent)rsp).getInventory().getUuid() : "", PriceTableVO.class);
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }
}
