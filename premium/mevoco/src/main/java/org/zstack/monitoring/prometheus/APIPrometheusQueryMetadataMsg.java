package org.zstack.monitoring.prometheus;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/9/1.
 */
@RestRequest(
        path = "/prometheus/meta-data",
        method = HttpMethod.GET,
        responseClass = APIPrometheusQueryMetadataReply.class
)
@Deprecated
public class APIPrometheusQueryMetadataMsg extends APISyncCallMessage {
    @APIParam(nonempty = true)
    private List<String> matches;

    public List<String> getMatches() {
        return matches;
    }

    public void setMatches(List<String> matches) {
        this.matches = matches;
    }
 
    public static APIPrometheusQueryMetadataMsg __example__() {
        APIPrometheusQueryMetadataMsg msg = new APIPrometheusQueryMetadataMsg();
        msg.setMatches(Arrays.asList("matches"));
        return msg;
    }

}
