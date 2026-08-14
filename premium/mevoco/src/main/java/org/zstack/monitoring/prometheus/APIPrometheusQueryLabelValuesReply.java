package org.zstack.monitoring.prometheus;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/9/2.
 */
@RestResponse(allTo = "inventories")
public class APIPrometheusQueryLabelValuesReply extends APIReply {
    @NoJsonSchema
    private Map inventories;

    public Map getInventories() {
        return inventories;
    }

    public void setInventories(Map inventories) {
        this.inventories = inventories;
    }
 
    public static APIPrometheusQueryLabelValuesReply __example__() {
        APIPrometheusQueryLabelValuesReply reply = new APIPrometheusQueryLabelValuesReply();
        reply.setInventories(new HashMap());
        return reply;
    }

}
