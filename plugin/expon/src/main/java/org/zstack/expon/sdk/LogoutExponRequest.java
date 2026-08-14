package org.zstack.expon.sdk;

import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/v2/logout",
        method = HttpMethod.POST,
        responseClass = LogoutExponResponse.class,
        version = "v1",
        sync = false
)
public class LogoutExponRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
