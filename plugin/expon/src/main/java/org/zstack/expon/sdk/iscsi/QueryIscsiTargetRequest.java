package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;
@ExponRestRequest(
        path = "/block/iscsi/gateways",
        method = HttpMethod.GET,
        responseClass = QueryIscsiTargetResponse.class,
        sync = true
)
public class QueryIscsiTargetRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
