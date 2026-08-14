package org.zstack.expon.sdk.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;


@ExponRestRequest(
        path = "/block/volumes/{volumeId}/clients",
        method = HttpMethod.GET,
        responseClass = GetVolumeBoundIscsiClientGroupResponse.class,
        sync = false
)
public class GetVolumeBoundIscsiClientGroupRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String volumeId;

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
