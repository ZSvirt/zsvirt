package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/9/28.
 */
@RestRequest(
        path = "/billings/price-tables/{tableUuid}/accounts/{accountUuid}",
        responseClass = APIChangeAccountPriceTableBindingEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIChangeAccountPriceTableBindingMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;

    @APIParam(resourceType = PriceTableVO.class)
    private String tableUuid;

    public static APIChangeAccountPriceTableBindingMsg __example__() {
        APIChangeAccountPriceTableBindingMsg msg = new APIChangeAccountPriceTableBindingMsg();
        msg.setTableUuid(uuid());
        msg.setAccountUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIChangeAccountPriceTableBindingEvent)rsp).getInventory().getUuid() : "", PriceTableVO.class);
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
