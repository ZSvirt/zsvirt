package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by lining on 2019/9/10.
 */

@RestRequest(
        path = "/billings/price-tables/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdatePriceTableEvent.class
)
public class APIUpdatePriceTableMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = PriceTableVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIUpdatePriceTableEvent)rsp).getInventory().getUuid() : "", PriceTableVO.class);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdatePriceTableMsg __example__() {
        APIUpdatePriceTableMsg msg = new APIUpdatePriceTableMsg();
        msg.setName("table_1");
        msg.setUuid(uuid());
        return msg;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
