package org.zstack.expon.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;


@ExponRestRequest(
        path = "/block/vhost/{vhostId}/vhost_binded_uss",
        method = HttpMethod.GET,
        responseClass = GetVhostControllerBoundUssResponse.class
)
public class GetVhostControllerBoundUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String vhostId;

    public void setVhostId(String vhostId) {
        this.vhostId = vhostId;
    }

    public String getVhostId() {
        return vhostId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
