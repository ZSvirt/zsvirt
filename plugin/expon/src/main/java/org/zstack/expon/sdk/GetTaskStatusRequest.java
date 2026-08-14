package org.zstack.expon.sdk;

import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/tasks/{id}",
        method = HttpMethod.GET,
        responseClass = GetTaskStatusResponse.class,
        version = "v1",
        sync = false
)
public class GetTaskStatusRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
