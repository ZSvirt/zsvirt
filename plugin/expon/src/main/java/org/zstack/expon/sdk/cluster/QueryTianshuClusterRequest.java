package org.zstack.expon.sdk.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/tianshu",
        method = HttpMethod.GET,
        responseClass = QueryTianshuClusterResponse.class,
        sync = false
)
public class QueryTianshuClusterRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
