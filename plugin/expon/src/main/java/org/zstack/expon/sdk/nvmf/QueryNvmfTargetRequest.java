package org.zstack.expon.sdk.nvmf;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponQueryRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;
@ExponRestRequest(
        path = "/block/nvmf",
        method = HttpMethod.GET,
        responseClass = QueryNvmfTargetResponse.class,
        sync = true
)
public class QueryNvmfTargetRequest extends ExponQueryRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
