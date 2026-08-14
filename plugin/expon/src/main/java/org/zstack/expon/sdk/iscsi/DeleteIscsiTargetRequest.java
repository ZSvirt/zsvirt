package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/gateways/{id}",
        method = HttpMethod.DELETE,
        responseClass = DeleteIscsiTargetResponse.class
)
public class DeleteIscsiTargetRequest extends ExponRequest {

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String id;

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
