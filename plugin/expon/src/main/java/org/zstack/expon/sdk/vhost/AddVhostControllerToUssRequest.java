package org.zstack.expon.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/vhost/bind_uss",
        method = HttpMethod.PUT,
        responseClass = AddVhostControllerToUssResponse.class
)
public class AddVhostControllerToUssRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String vhostId;
    @Param
    private String ussGwId;
    @Param
    private String lunId;
    @Param
    private boolean isSnapshot;

    public String getVhostId() {
        return vhostId;
    }

    public void setVhostId(String vhostId) {
        this.vhostId = vhostId;
    }

    public String getUssGwId() {
        return ussGwId;
    }

    public void setUssGwId(String ussGwId) {
        this.ussGwId = ussGwId;
    }

    public String getLunId() {
        return lunId;
    }

    public void setLunId(String lunId) {
        this.lunId = lunId;
    }

    public void setSnapshot(boolean snapshot) {
        isSnapshot = snapshot;
    }

    public boolean isSnapshot() {
        return isSnapshot;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
