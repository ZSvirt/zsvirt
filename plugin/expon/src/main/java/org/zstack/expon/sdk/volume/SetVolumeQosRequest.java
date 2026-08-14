package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/volumes/{volId}/qos",
        method = HttpMethod.PUT,
        responseClass = SetVolumeQosResponse.class
)
public class SetVolumeQosRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String volId;
    @Param
    private ExponVolumeQos qos;


    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public String getVolId() {
        return volId;
    }

    public void setVolId(String volId) {
        this.volId = volId;
    }

    public ExponVolumeQos getQos() {
        return qos;
    }

    public void setQos(ExponVolumeQos qos) {
        this.qos = qos;
    }
}
