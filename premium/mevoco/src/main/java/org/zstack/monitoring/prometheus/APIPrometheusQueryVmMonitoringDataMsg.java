package org.zstack.monitoring.prometheus;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/7/14.
 */
@RestRequest(
        path = "/prometheus/vm-instances",
        method = HttpMethod.GET,
        responseClass = APIPrometheusQueryVmMonitoringDataReply.class
)
@Deprecated
public class APIPrometheusQueryVmMonitoringDataMsg extends APIPrometheusQueryMsg {
    @APIParam(nonempty = true)
    private List<String> vmUuids;

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }
 
    public static APIPrometheusQueryVmMonitoringDataMsg __example__() {
        APIPrometheusQueryVmMonitoringDataMsg msg = new APIPrometheusQueryVmMonitoringDataMsg();
        msg.setVmUuids(Arrays.asList(uuid()));
        msg.setExpression("collectd:collectd_memory");
        return msg;
    }

}
