package org.zstack.monitoring.prometheus;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/9/1.
 */
@RestResponse(allTo = "inventories")
public class APIPrometheusQueryMetadataReply extends APIReply {
    @NoJsonSchema
    private Map inventories;

    public Map getInventories() {
        return inventories;
    }

    public void setInventories(Map inventories) {
        this.inventories = inventories;
    }

    public static APIPrometheusQueryMetadataReply __example__() {
        APIPrometheusQueryMetadataReply reply = new APIPrometheusQueryMetadataReply();
        reply.setInventories(new HashMap());
        return reply;
    }

}
