package org.zstack.billing.table;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import java.util.List;
import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/10/10.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetAccountPriceTableRefReply extends APIReply {
    private List<String> accountUuids;
    private String tableUuid;

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public static APIGetAccountPriceTableRefReply __example__() {
        APIGetAccountPriceTableRefReply reply = new APIGetAccountPriceTableRefReply();
        reply.setAccountUuids(asList(uuid(), uuid(), uuid()));
        return reply;
    }

}
