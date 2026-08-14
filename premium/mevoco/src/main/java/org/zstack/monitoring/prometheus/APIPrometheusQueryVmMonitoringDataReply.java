package org.zstack.monitoring.prometheus;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/7/14.
 */
@RestResponse(allTo = "inventories")
public class APIPrometheusQueryVmMonitoringDataReply extends APIReply {
    @NoJsonSchema
    private Map inventories;

    public Map getInventories() {
        return inventories;
    }

    public void setInventories(Map inventories) {
        this.inventories = inventories;
    }
 
    public static APIPrometheusQueryVmMonitoringDataReply __example__() {
        APIPrometheusQueryVmMonitoringDataReply reply = new APIPrometheusQueryVmMonitoringDataReply();
        reply.setInventories(new HashMap());
        return reply;
    }

}
