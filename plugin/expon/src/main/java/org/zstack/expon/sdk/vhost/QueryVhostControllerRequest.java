package org.zstack.expon.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQuery;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.volume.VolumeModule;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/vhost",
        method = HttpMethod.GET,
        responseClass = QueryVhostControllerResponse.class,
        sync = true
)
@ExponQuery(inventoryClass = VolumeModule.class, replyClass = QueryVhostControllerResponse.class)
public class QueryVhostControllerRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
