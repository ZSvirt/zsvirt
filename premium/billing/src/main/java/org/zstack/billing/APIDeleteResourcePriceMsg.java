package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.billing.table.PriceTableVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/6/12.
 */
@RestRequest(
        path = "/billings/prices/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteResourcePriceEvent.class
)
public class APIDeleteResourcePriceMsg extends APIDeleteMessage implements APIAuditor {
    @APIParam(resourceType = PriceVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String tableUuid;

    @APIParam(required = false)
    private boolean cutoffPrice;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public static APIDeleteResourcePriceMsg __example__() {
        APIDeleteResourcePriceMsg msg = new APIDeleteResourcePriceMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIDeleteResourcePriceMsg)msg).getTableUuid() : "", PriceTableVO.class);
    }

    public boolean isCutoffPrice() {
        return cutoffPrice;
    }

    public void setCutoffPrice(boolean cutoffPrice) {
        this.cutoffPrice = cutoffPrice;
    }
}
