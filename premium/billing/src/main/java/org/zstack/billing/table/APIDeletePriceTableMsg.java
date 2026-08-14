package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
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
        path = "/billings/price-tables/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePriceTableEvent.class
)
public class APIDeletePriceTableMsg extends APIDeleteMessage implements APIAuditor {
    @APIParam(resourceType = PriceTableVO.class, successIfResourceNotExisting = true, scope = SCOPE_ALLOWED_SHARING)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeletePriceTableMsg __example__() {
        APIDeletePriceTableMsg msg = new APIDeletePriceTableMsg();
        msg.setUuid(uuid());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIDeletePriceTableMsg)msg).getUuid() : "", PriceTableVO.class);
    }
}
