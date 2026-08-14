package org.zstack.monitoring.prometheus;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.Map;

/**
 * Created by xing5 on 2016/7/14.
 */
@RestResponse(allTo = "inventories")
public class APIPrometheusQueryPassThroughReply extends MessageReply {
    @NoJsonSchema
    private Map inventories;

    public Map getInventories() {
        return inventories;
    }

    public void setInventories(Map inventories) {
        this.inventories = inventories;
    }
 
    public static APIPrometheusQueryPassThroughReply __example__() {
        APIPrometheusQueryPassThroughReply reply = new APIPrometheusQueryPassThroughReply();


        return reply;
    }

}
