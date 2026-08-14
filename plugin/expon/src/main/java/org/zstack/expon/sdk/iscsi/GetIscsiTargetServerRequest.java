package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/gateways/{tianshuId}/server",
        method = HttpMethod.GET,
        responseClass = QueryIscsiClientGroupResponse.class,
        sync = false
)
public class GetIscsiTargetServerRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String tianshuId;

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
