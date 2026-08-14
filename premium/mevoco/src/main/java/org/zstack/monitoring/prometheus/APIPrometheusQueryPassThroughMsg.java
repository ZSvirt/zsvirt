package org.zstack.monitoring.prometheus;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/7/14.
 */
@RestRequest(
        path = "/prometheus/all",
        method = HttpMethod.GET,
        responseClass = APIPrometheusQueryPassThroughReply.class
)
@Deprecated
public class APIPrometheusQueryPassThroughMsg extends APIPrometheusQueryMsg {
 
    public static APIPrometheusQueryPassThroughMsg __example__() {
        APIPrometheusQueryPassThroughMsg msg = new APIPrometheusQueryPassThroughMsg();

        msg.setExpression("collectd:collectd_memory");

        return msg;
    }

}
