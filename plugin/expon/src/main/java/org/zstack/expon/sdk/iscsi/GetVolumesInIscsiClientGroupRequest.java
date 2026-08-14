package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponParam;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/iscsi/clients/{id}/luns",
        method = HttpMethod.GET,
        responseClass = GetVolumesInIscsiClientGroupResponse.class
)
public class GetVolumesInIscsiClientGroupRequest extends ExponRequest {
    private static final HashMap<String, ExponParam.Parameter> parameterMap = new HashMap<>();

    @Param
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Map<String, ExponParam.Parameter> getParameterMap() {
        return parameterMap;
    }
}
